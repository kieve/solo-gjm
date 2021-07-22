package ca.kieve.sologjm;

import jakarta.xml.bind.JAXBException;
import org.audiveris.proxymusic.Attributes;
import org.audiveris.proxymusic.Backup;
import org.audiveris.proxymusic.Barline;
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipEntry;

public class MxlParser {
    // Debug runner
    public static void main(String[] args)
            throws IOException, MxlException, UnmarshallingException, JAXBException
    {
        MxlParser parser = new MxlParser();

        boolean swingBeat = false;
        parser.parse(new File(MXL_FILE), "Everything I Wanted", "Billie Eilish", 126, 100, 70,
                swingBeat, 0, 0);

        File debugFile = new File(DEBUG_FILE);
        debugFile.delete();
        BufferedWriter bw = new BufferedWriter(new FileWriter(debugFile));
        bw.write(parser.getResult());
        bw.close();
    }

    private static final String WORK_DIR = System.getProperty("user.dir");
    /*
     * SecretBase
     * IveanPolkka
     * Fukkireta
     * RenaiCirculation
     * Darkside
     * Summertime
     * Story
     * MyR
     * TheSpectre
     * Ra-Ra-Rasputin
     * Ignite
     * EverythingIWanted
     */
    private static final String MXL_FILE = WORK_DIR + "/assets/mxl/EverythingIWanted.mxl";
    private static final String DEBUG_FILE = WORK_DIR + "/assets/DEBUG.gjm";

    private String m_result;

    public String getResult() {
        return m_result;
    }

    public void parse(File file, String songTitle, String songAuthor, int bpm, int trackVolume1,
            int trackVolume2, boolean swingBeat, int octaveOffset1, int octaveOffset2)
            throws IOException, MxlException, UnmarshallingException, JAXBException
    {
        // First, test opening the .MXL file
        Marshalling.getContext(ScorePartwise.class);

        Mxl.Input mif = new Mxl.Input(file);

        RootFile first = mif.getRootFiles().get(0);
        ZipEntry zipEntry = mif.getEntry(first.fullPath);
        InputStream is = mif.getInputStream(zipEntry);
        ScorePartwise newScorePartwise = (ScorePartwise) Marshalling.unmarshal(is);

        List<Part> parts = newScorePartwise.getPart();
        System.out.printf("There are %d parts.\n", parts.size());

        GjmBuilder gjmBuilder = new GjmBuilder(songTitle, songAuthor, bpm, trackVolume1,
                trackVolume2, swingBeat, octaveOffset1, octaveOffset2);

        for (Part part : parts) {
            List<Measure> measures = part.getMeasure();
            System.out.printf("There are %d measures.\n", measures.size());
            int measureIndex = 0;
            for (Measure measure : measures) {
//                if (measureIndex != 0 && measureIndex != 15) {
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
                        gjmBuilder.parseForward((Forward) measurePart);
                    } else if (measurePart instanceof Barline) {
                        gjmBuilder.parseBarline((Barline) measurePart);
                    } else {
                        System.out.println("Dono: " + measurePart);
                    }
                }
                gjmBuilder.nextMeasure();
                measureIndex++;
            }
        }

        gjmBuilder.postProcess();

        m_result = gjmBuilder.writeGjm();
        System.out.println("====================================================");
        System.out.println(m_result);
    }
}
