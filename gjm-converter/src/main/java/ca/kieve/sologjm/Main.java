package ca.kieve.sologjm;

import org.audiveris.proxymusic.Attributes;
import org.audiveris.proxymusic.Backup;
import org.audiveris.proxymusic.Direction;
import org.audiveris.proxymusic.Forward;
import org.audiveris.proxymusic.Note;
import org.audiveris.proxymusic.Print;
import org.audiveris.proxymusic.ScorePartwise;
import org.audiveris.proxymusic.ScorePartwise.Part;
import org.audiveris.proxymusic.ScorePartwise.Part.Measure;
import org.audiveris.proxymusic.mxl.Mxl;
import org.audiveris.proxymusic.mxl.Mxl.MxlException;
import org.audiveris.proxymusic.mxl.RootFile;
import org.audiveris.proxymusic.util.Marshalling;
import org.audiveris.proxymusic.util.Marshalling.UnmarshallingException;

import javax.xml.bind.JAXBException;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipEntry;

public class Main {
    private static final String WORK_DIR = System.getProperty("user.dir");
    /*
     * SecretBase
     * RenaiCirculation
     * Story
     * MyR
     * Darkside
     * Summertime
     * TheSpectre
     */
    private static final String MXL_FILE = WORK_DIR + "/assets/SecretBase.mxl";
    private static final String DEBUG_FILE = WORK_DIR + "/assets/DEBUG.gjm";

    private static void p(Object object) {
        if (object == null) return;
        System.out.println(new Dumper.Column(object, ""));
    }

    public static void main(String[] args)
            throws IOException, MxlException, UnmarshallingException, JAXBException
    {
        // TODO: Print usage.

        // First, test opening the .MXL file
        Marshalling.getContext(ScorePartwise.class);

        Mxl.Input mif = new Mxl.Input(new File(MXL_FILE));

        RootFile first = mif.getRootFiles().get(0);
        ZipEntry zipEntry = mif.getEntry(first.fullPath);
        InputStream is = mif.getInputStream(zipEntry);
        ScorePartwise newScorePartwise = (ScorePartwise) Marshalling.unmarshal(is);


        List<Part> parts = newScorePartwise.getPart();
        System.out.printf("There are %d parts.\n", parts.size());

        GjmBuilder gjmBuilder = new GjmBuilder("Test notation.", "Test author.");

        for (Part part : parts) {
            List<Measure> measures = part.getMeasure();
            System.out.printf("There are %d measures.\n", measures.size());
            int measureIndex = 0;
            for (Measure measure : measures) {
//                if (measureIndex != 0 && measureIndex != 26) {
//                    measureIndex++;
//                    continue;
//                }
                List<Object> measureParts = measure.getNoteOrBackupOrForward();
                for (Object measurePart : measureParts) {
                    if (measurePart instanceof Print) {
                        // We don't care about the page print attributes.
                    } else if (measurePart instanceof Attributes) {
                        gjmBuilder.parseAttributes((Attributes) measurePart);
                    } else if (measurePart instanceof Note) {
                        gjmBuilder.parseNote((Note) measurePart);
                    } else if (measurePart instanceof Direction) {
                        gjmBuilder.parseDirection((Direction) measurePart);
                    } else if (measurePart instanceof Backup) {
                        gjmBuilder.parseBackup((Backup) measurePart);
                    } else if (measurePart instanceof Forward) {
                        Forward forward = (Forward) measurePart;
                        throw new UnsupportedOperationException(
                                "------------------------ FORWARD?");
                    } else {
                        System.out.println("Dono: " + measurePart);
                    }
                }
                gjmBuilder.nextMeasure();
                measureIndex++;
            }
        }

        gjmBuilder.swing();

        String result = gjmBuilder.writeGjm();
        System.out.println("====================================================");
        System.out.println(result);

        File debugFile = new File(DEBUG_FILE);
        debugFile.delete();
        BufferedWriter bw = new BufferedWriter(new FileWriter(debugFile));
        bw.write(result);
        bw.close();
    }
}
