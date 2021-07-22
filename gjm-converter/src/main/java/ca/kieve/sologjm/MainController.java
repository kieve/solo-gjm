package ca.kieve.sologjm;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

public class MainController implements LogManager.Listener {
    private final LogManager m_logManager;

    private final FileChooser m_inputChooser;
    private final FileChooser m_outputChooser;
    private final MxlParser m_mxlParser;

    private Stage m_stage;

    private File m_inputFile;

    @FXML
    private TextField m_inputPath;

    @FXML
    private Button m_inputPathButton;

    @FXML
    private CheckBox m_swingBeat;

    @FXML
    private TextArea m_logArea;

    @FXML
    private Button m_convertButton;

    public MainController() {
        m_logManager = LogManager.getInstance();
        m_inputChooser = new FileChooser();
        m_outputChooser = new FileChooser();
        m_mxlParser = new MxlParser();

        m_logManager.addListener(this);

        FileChooser.ExtensionFilter mxlFileFilter =
                new ExtensionFilter("MusicXML files (*.mxl)", "*.mxl");
        m_inputChooser.getExtensionFilters().add(mxlFileFilter);

        FileChooser.ExtensionFilter gjmFilter =
                new ExtensionFilter("SOLO GJM files (*.gjm)", "*.gjm");
        m_outputChooser.getExtensionFilters().add(gjmFilter);
    }

    public void setStage(Stage stage) {
        m_stage = stage;
    }

    @FXML
    public void initialize() {
        for (String message : m_logManager.getMessages()) {
            onMessageAdded(message);
        }

        m_inputPathButton.setOnMouseClicked(event -> {
                m_inputFile = m_inputChooser.showOpenDialog(m_stage);
                m_inputPath.setText(m_inputFile.getAbsolutePath());
        });

        m_convertButton.setOnMouseClicked(event -> {
            try {
                m_mxlParser.parse(m_inputFile, "songTitle", "songAuthor", 120, 100, 40,
                        m_swingBeat.isSelected(), 0, 0);
                File saveFile = m_outputChooser.showSaveDialog(m_stage);
                saveFile.delete();
                BufferedWriter bw = new BufferedWriter(new FileWriter(saveFile));
                bw.write(m_mxlParser.getResult());
                bw.close();
            } catch (Exception e) {
                m_logManager.addMessage("Failed to parse file.", e);
            }
        });
    }

    @Override
    public void onMessageAdded(String message) {
        Platform.runLater(() -> {
            m_logArea.appendText(message);
            m_logArea.appendText("\n");
        });
    }
}
