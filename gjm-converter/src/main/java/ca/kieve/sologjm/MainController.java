package ca.kieve.sologjm;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Slider;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextFormatter.Change;
import javafx.stage.FileChooser;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.UnaryOperator;

public class MainController implements LogManager.Listener {
    private static class SliderNumericOperator implements UnaryOperator<Change> {
        private final AtomicBoolean m_programmatic;
        private final TextField m_textField;
        private final Slider m_slider;
        private final int m_min;
        private final int m_max;

        private SliderNumericOperator(AtomicBoolean programmatic, TextField textField,
                Slider slider)
        {
            m_programmatic = programmatic;
            m_textField = textField;
            m_slider = slider;
            m_min = (int) Math.round(slider.getMin());
            m_max = (int) Math.round(slider.getMax());
        }

        public static void addTo(AtomicBoolean programmatic, TextField textField, Slider slider) {
            textField.setTextFormatter(new TextFormatter<>(
                    new SliderNumericOperator(programmatic, textField, slider)));
        }

        private static Change deny(Change change) {
            change.setText("");
            change.setRange(change.getRangeStart(), change.getRangeStart());
            return change;
        }

        @Override
        public Change apply(Change change) {
            if (!change.isContentChange()) {
                return change;
            }

            // If this is a programmatic change by us, just let it happen.
            if (m_programmatic.get()) {
                return change;
            }

            String newText = change.getControlNewText();
            if (newText.length() == 0) {
                setSlider(m_programmatic, m_slider, m_slider.getMin());
                return change;
            }

            // Make sure every value is a numeral
            String stripped = newText.strip();
            if (newText.length() != stripped.length()) {
                return deny(change);
            }

            // No negative numbers
            if (newText.contains("-")) {
                return deny(change);
            }

            // Make sure it is a number
            int value;
            try {
                value = Integer.parseInt(newText);
            } catch (NumberFormatException e) {
                return deny(change);
            }

            if (value > m_max) {
                Platform.runLater(() -> {
                    setText(m_programmatic, m_textField, String.valueOf(m_max));
                });
                setSlider(m_programmatic, m_slider, m_max);
                return deny(change);
            } else if (value < m_min) {
                // Special case, we just set the slider to the min value, leave the text as is
                value = m_min;
            }
            setSlider(m_programmatic, m_slider, value);

            return change;
        }
    }

    private static class SliderTextUpdater implements ChangeListener<Number> {
        private final AtomicBoolean m_programmatic;
        private final TextField m_textField;

        private SliderTextUpdater(AtomicBoolean programmatic, TextField textField) {
            m_programmatic = programmatic;
            m_textField = textField;
        }

        public static void addTo(AtomicBoolean programmatic, TextField textField, Slider slider) {
            slider.valueProperty().addListener(new SliderTextUpdater(programmatic, textField));
        }

        @Override
        public void changed(ObservableValue<? extends Number> observableValue, Number oldValue,
                Number newValue)
        {
            // If this is a programmatic change by us, just let it happen.
            if (m_programmatic.get()) {
                return;
            }
            setText(m_programmatic, m_textField, (Double) newValue);
        }
    }

    private static final String ERROR_CLASS = "error";

    private final LogManager m_logManager;

    private final FileChooser m_inputChooser;
    private final FileChooser m_outputChooser;
    private final MxlParser m_mxlParser;

    private Stage m_stage;

    private File m_inputFile;

    private final AtomicBoolean m_programmatic;

    @FXML
    private TextField m_inputPath;

    @FXML
    private Button m_inputPathButton;

    @FXML
    private TextField m_songTitle;

    @FXML
    private TextField m_songAuthor;

    @FXML
    private TextField m_bpmField;

    @FXML
    private Slider m_bpmSlider;

    @FXML
    private TextField m_track1VolumeField;

    @FXML
    private Slider m_track1VolumeSlider;

    @FXML
    private TextField m_track2VolumeField;

    @FXML
    private Slider m_track2VolumeSlider;

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
        m_programmatic = new AtomicBoolean(false);

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
                if (m_inputFile != null) {
                    m_inputPath.setText(m_inputFile.getAbsolutePath());
                    unsetError(m_inputPath);
                } else {
                    setError(m_inputPath);
                }
        });

        m_songTitle.textProperty().addListener($ -> unsetError(m_songTitle));
        m_songAuthor.textProperty().addListener($ -> unsetError(m_songAuthor));

        SliderTextUpdater.addTo(m_programmatic, m_bpmField, m_bpmSlider);
        SliderNumericOperator.addTo(m_programmatic, m_bpmField, m_bpmSlider);
        setTextFromSlider(m_programmatic, m_bpmField, m_bpmSlider);

        StringConverter<Double> volumeConverter = new StringConverter<>() {
            @Override
            public String toString(Double value) {
                return Math.round(value) + "%";
            }

            @Override
            public Double fromString(String s) {
                return 0d;
            }
        };

        SliderTextUpdater.addTo(m_programmatic, m_track1VolumeField, m_track1VolumeSlider);
        SliderNumericOperator.addTo(m_programmatic, m_track1VolumeField, m_track1VolumeSlider);
        m_track1VolumeSlider.setLabelFormatter(volumeConverter);
        setTextFromSlider(m_programmatic, m_track1VolumeField, m_track1VolumeSlider);

        SliderTextUpdater.addTo(m_programmatic, m_track2VolumeField, m_track2VolumeSlider);
        SliderNumericOperator.addTo(m_programmatic, m_track2VolumeField, m_track2VolumeSlider);
        m_track2VolumeSlider.setLabelFormatter(volumeConverter);
        setTextFromSlider(m_programmatic, m_track2VolumeField, m_track2VolumeSlider);

        m_convertButton.setOnMouseClicked(event -> {
            boolean anyError = false;
            if (m_inputFile == null) {
                anyError = true;
                setError(m_inputPath);
            }
            if (m_songTitle.getText().isBlank()) {
                anyError = true;
                setError(m_songTitle);
            }
            if (m_songAuthor.getText().isBlank()) {
                anyError = true;
                setError(m_songAuthor);
            }
            if (anyError) {
                return;
            }
            try {
                m_mxlParser.parse(m_inputFile,
                        m_songTitle.getText(),
                        m_songAuthor.getText(),
                        (int) Math.round(m_bpmSlider.getValue()),
                        (int) Math.round(m_track1VolumeSlider.getValue()),
                        (int) Math.round(m_track2VolumeSlider.getValue()),
                        m_swingBeat.isSelected(),
                        0, 0); // TODO: Let people shift octaves
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

    private static void setTextFromSlider(AtomicBoolean programmatic, TextField textField,
            Slider slider)
    {
        setText(programmatic, textField, slider.getValue());
    }

    private static void setText(AtomicBoolean programmatic, TextField textField, double value) {
        setText(programmatic, textField, Long.toString(Math.round(value)));
    }

    private static void setText(AtomicBoolean programmatic, TextField textField, String value) {
        programmatic.set(true);
        textField.setText(value);
        programmatic.set(false);
    }

    private static void setSliderFromText(AtomicBoolean programmatic, Slider slider,
            TextField textField)
    {
        setSlider(programmatic, slider, textField.getText());
    }

    private static void setSlider(AtomicBoolean programmatic, Slider slider, String value) {
        setSlider(programmatic, slider, Double.parseDouble(value));
    }

    private static void setSlider(AtomicBoolean programmatic, Slider slider, double value) {
        programmatic.set(true);
        slider.setValue(value);
        programmatic.set(false);
    }

    private static void setError(TextField textField) {
        ObservableList<String> styleClasses = textField.getStyleClass();
        if (!styleClasses.contains(ERROR_CLASS)) {
            styleClasses.add(ERROR_CLASS);
        }
    }

    private static void unsetError(TextField textField) {
        ObservableList<String> styleClasses = textField.getStyleClass();
        styleClasses.removeAll(Collections.singleton(ERROR_CLASS));
    }
}
