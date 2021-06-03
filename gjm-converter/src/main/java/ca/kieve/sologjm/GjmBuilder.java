package ca.kieve.sologjm;

import org.audiveris.proxymusic.Arpeggiate;
import org.audiveris.proxymusic.Articulations;
import org.audiveris.proxymusic.Attributes;
import org.audiveris.proxymusic.Backup;
import org.audiveris.proxymusic.Clef;
import org.audiveris.proxymusic.Direction;
import org.audiveris.proxymusic.EmptyPlacement;
import org.audiveris.proxymusic.Key;
import org.audiveris.proxymusic.Notations;
import org.audiveris.proxymusic.Note;
import org.audiveris.proxymusic.NoteType;
import org.audiveris.proxymusic.Pitch;
import org.audiveris.proxymusic.Rest;
import org.audiveris.proxymusic.StartStop;
import org.audiveris.proxymusic.Tie;
import org.audiveris.proxymusic.Tied;
import org.audiveris.proxymusic.Time;

import javax.xml.bind.JAXBElement;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class GjmBuilder {
    private enum _Clef {
        L2G,
        L4F
    }

    private enum _NoteType {
        THIRTY_SECOND("32nd", "The32nd", 2),
        SIXTEENTH("16th", "The16th", 4),
        EIGHTH("eighth", "Eighth", 8),
        QUARTER("quarter", null, 16),
        HALF("half", "Half", 32),
        WHOLE("whole", "Whole", 64);

        private final String m_mxlId;
        private final String m_gjmId;
        private final int m_durationMultiplier;

        _NoteType(String mxlId, String gjmId, int durationMultiplier) {
            m_mxlId = mxlId;
            m_gjmId = gjmId;
            m_durationMultiplier = durationMultiplier;
        }

        public static _NoteType fromMxl(String mxlId) {
            for (_NoteType note : values()) {
                if (note.m_mxlId.equals(mxlId)) {
                    return note;
                }
            }
            throw new UnsupportedOperationException("Unsupported Note Duration: " + mxlId);
        }
    }

    private enum _Step {
        C(1, 0, 2, 6),
        D(2, 2, 4, 4),
        E(3, 4, 6, 2),
        F(4, 5, 1, 7),
        G(5, 7, 3, 5),
        A(6, 9, 5, 3),
        B(7, 10, 7, 1);

        private final int m_sign;
        private final int m_pitchOffset;

        // https://en.wikipedia.org/wiki/Key_signature#Scales_with_sharp_key_signatures
        private final int m_sharpScale;
        private final int m_flatScale;

        _Step(int sign, int pitchOffset, int sharpScale, int flatScale) {
            m_sign = sign;
            m_pitchOffset = pitchOffset;
            m_sharpScale = sharpScale;
            m_flatScale = flatScale;
        }
    }

    private class _Pitch {
        private int m_octave; // 0 - 9
        private _Step m_step;
        private int m_alter; // -1 = flat, 1 = sharp, microtones not supported

        public _Pitch deepClone() {
            _Pitch result = new _Pitch();
            result.m_octave = m_octave;
            result.m_step = m_step;
            result.m_alter = m_alter;
            return result;
        }
    }

    private class _Note {
        private _NoteType m_noteType;
        private boolean m_isRest;
        private List<_Pitch> m_pitches = new ArrayList<>(3);
        private boolean m_isDotted;
        private boolean m_isTriplet;
        private boolean m_isArpeggiate;

        private boolean m_tieStart;
        private boolean m_tieEnd;

        // Swing stuff
        private boolean m_isSwing;
        private int m_swingIndexOverride;
        private double m_swingDurationMultiplier;

        public _Note deepClone() {
            _Note result = new _Note();
            result.m_noteType = m_noteType;
            result.m_isRest = m_isRest;
            result.m_pitches = new ArrayList<>();
            for (_Pitch pitch : m_pitches) {
                result.m_pitches.add(pitch.deepClone());
            }
            result.m_isDotted = m_isDotted;
            result.m_isTriplet = m_isTriplet;
            result.m_isArpeggiate = m_isArpeggiate;
            result.m_tieStart = m_tieStart;
            result.m_tieEnd = m_tieEnd;
            result.m_isSwing = m_isSwing;
            result.m_swingIndexOverride = m_swingIndexOverride;
            result.m_swingDurationMultiplier = m_swingDurationMultiplier;
            return result;
        }
    }

    private class _Measure {
        List<_Note> m_notes1 = new ArrayList<>();
        List<_Note> m_notes2 = new ArrayList<>();
    }

    private enum _SwingMode {
        FORCE,
        A,
        B,
        C
    }

    private static final int BPM = 120;
    private static final int BASE_DURATION = 125; // MS, length of a 1/64 note. 125 = 60bpm
    private static final int GJM_PITCH_OFFSET = 4; // Octave C1 starts at 4.

    private final String m_notationName;
    private final String m_notationAuthor;

    private int m_key = -1;
    private int m_beatsPerMeasure = -1;
    private _NoteType m_beatsUnit = _NoteType.QUARTER;

    private _Clef m_clef1;
    private _Clef m_clef2;

    private List<_Measure> m_measures = new ArrayList<>();

    private int m_currentMeasureId = 0;
    private _Measure m_currentMeasure = new _Measure();

    public GjmBuilder(String notationName, String notationAuthor) {
        m_notationName = notationName;
        m_notationAuthor = notationAuthor;
    }

    private static void p(Object object) {
        if (object == null) return;
        System.out.println(new Dumper.Column(object, ""));
    }

    private static void uo(Object object, String message) {
        if (object == null) return;
        p(object);
        throw new UnsupportedOperationException(message);
    }

    private static void uc(Collection<?> collection, String message) {
        if (collection.isEmpty()) return;
        throw new UnsupportedOperationException(message);
    }

    public void nextMeasure() {
        m_currentMeasureId++;
        m_measures.add(m_currentMeasure);
        m_currentMeasure = new _Measure();
        System.out.println("Measure " + m_currentMeasureId);
    }

    public void parseAttributes(Attributes attributes) {
        // TODO: We can possibly support up to three divisions / staves. But for now, only two?
        // attributes.getDivisions();
        // attributes.getStaves();

        p(attributes.getKey().get(0));
        p(attributes.getTime().get(0));

        List<Key> keyList = attributes.getKey();
        if (keyList != null && !keyList.isEmpty()) {
            if (m_key >= 0 || keyList.size() > 1) {
                throw new UnsupportedOperationException("Multiple key mapping not supported yet.");
            }
            m_key = keyList.get(0).getFifths().intValue();
        }

        List<Time> timeList = attributes.getTime();
        if (timeList != null && !timeList.isEmpty()) {
            if (m_beatsPerMeasure >= 0 || timeList.size() > 1) {
                throw new UnsupportedOperationException(
                        "Multiple beats / measure not supported yet.");
            }

            Time time = timeList.get(0);
            m_beatsPerMeasure = Integer.parseInt(
                    time.getTimeSignature().get(0).getValue());
            int beatValue = Integer.parseInt(
                    time.getTimeSignature().get(1).getValue());
            if (beatValue != 4) {
                throw new UnsupportedOperationException("Only x/4 time signatures supported.");
            }
        }

        // Clefts... This is associated and mapped to the number of staves.
        List<Clef> clefs = attributes.getClef();
        if (clefs.get(0).getSign().value().equals("G")) {
            m_clef1 = _Clef.L2G;
        } else {
            m_clef1 = _Clef.L4F;
        }
        if (clefs.get(1).getSign().value().equals("G")) {
            m_clef2 = _Clef.L2G;
        } else {
            m_clef2 = _Clef.L4F;
        }
    }

    public void parseDirection(Direction direction) {
        p(direction);
    }

    public void parseBackup(Backup backup) {
        p(backup);
    }

    public void parseNote(Note note) {
        parseNote(note, null, Collections.emptyList());
    }

    public void parseNote(Note note, _SwingMode swingMode, List<_Pitch> swingChords) {
        // Note: Duration type of "quarter notes" don't have a DurationType entry
        uo(note.getGrace(), "note grace");
        uo(note.getCue(), "note cue");
        uo(note.getUnpitched(), "note unpitch");
        uo(note.getInstrument(), "note instrument");
        uo(note.getFootnote(), "note footnote");
        uo(note.getLevel(), "note level");
        uo(note.getTimeModification(), "note timeModification");
        uo(note.getNotehead(), "note notehead");
        uo(note.getNoteheadText(), "note noteheadText");
        uc(note.getLyric(), "note lyric");
        uo(note.getPlay(), "note play");
        uo(note.getDynamics(), "note dynamics");
        uo(note.getEndDynamics(), "note endDynamics");
        uo(note.getAttack(), "note attack");
        uo(note.getRelease(), "note release");
        uo(note.getTimeOnly(), "note timeOnly");
        uo(note.getPizzicato(), "note pizzicato");
        uo(note.getFontFamily(), "note fontFamily");
        uo(note.getFontStyle(), "note fontStyle");
        uo(note.getFontSize(), "note fontSize");
        uo(note.getFontWeight(), "note fontWeight");
        uo(note.getColor(), "note color");
        uo(note.getRelativeX(), "note relativeX");
        uo(note.getRelativeY(), "note relativeY");
        uo(note.getPrintDot(), "note printDot");
        uo(note.getPrintLyric(), "note printLyric");
        uo(note.getPrintSpacing(), "note printSpacing");
        uo(note.getPrintObject(), "note printObject");

        for (Notations notations : note.getNotations()) {
            for (Object object : notations.getTiedOrSlurOrTuplet()) {
                if (!(object instanceof Tied)
                        && !(object instanceof Articulations)
                        && !(object instanceof Arpeggiate))
                {
                    p(object);
                    throw new UnsupportedOperationException("Unknown notation type.");
                }

                if (object instanceof Articulations) {
                    Articulations articulations = (Articulations) object;
                    for (JAXBElement<?> jele : articulations.getAccentOrStrongAccentOrStaccato()) {
                        if (!jele.getName().toString().equals("staccato")) {
                            throw new UnsupportedOperationException(
                                    "Only staccato articulation supported: "
                                            + jele.getName().toString());
                        }
                        System.out.println(jele.getName().toString());
                        p(jele.getValue());
                    }
                }
            }
        }

        if (note.getType() != null) {
            NoteType noteType = note.getType();
            uo(noteType.getSize(), "noteType Size");
        }

        if (note.getRest() != null) {
            Rest rest = note.getRest();
            p(rest);
            uo(rest.getMeasure(), "rest measure");
        }

        for (Tie tie : note.getTie()) {
            uo(tie.getTimeOnly(), "tie timeOnly");
        }

        if (note.getDot().size() > 1) {
            throw new UnsupportedOperationException("Only one dot supported... for now");
        }
        if (!note.getDot().isEmpty()) {
            EmptyPlacement dot = note.getDot().get(0);
            if (dot.getPlacement() != null) {
                throw new UnsupportedOperationException("Dot placement not supported");
            }
        }

        System.out.println("S-----------------------------------------------------------");

        _Note parsedNote = new _Note();
        if (note.getType() == null) {
            // If the note type isn't specified, assume it's a whole note
            parsedNote.m_noteType = _NoteType.WHOLE;
        } else {
            parsedNote.m_noteType = _NoteType.fromMxl(note.getType().getValue());
        }

        if (note.getRest() != null) {
            parsedNote.m_isRest = true;
        }

        Pitch pitch = note.getPitch();
        _Pitch parsedPitch = null;
        if (pitch != null) {
            if (parsedNote.m_isRest) {
                throw new IllegalStateException("Can't have a rest and pitch on same note.");
            }

            parsedPitch = new _Pitch();
            parsedPitch.m_octave = pitch.getOctave();
            if (parsedPitch.m_octave == 0) {
                throw new UnsupportedOperationException("Octave 0 not supported");
            }
            parsedPitch.m_step = _Step.valueOf(pitch.getStep().toString());

            // TODO: Fix how these are used...
            if (pitch.getAlter() != null) {
                BigDecimal alter = pitch.getAlter();
                if (alter.stripTrailingZeros().scale() > 0) {
                    throw new UnsupportedOperationException("Microtones not supported");
                }
                parsedPitch.m_alter = alter.intValue();
            } else {
                parsedPitch.m_alter = 0;
            }
        }

        if (!note.getDot().isEmpty()) {
            parsedNote.m_isDotted = true;
        }

        List<Tie> ties = note.getTie();
        for (Tie tie : ties) {
            if (tie.getType() == StartStop.START) {
                parsedNote.m_tieStart = true;
            } else {
                parsedNote.m_tieEnd = true;
            }
        }

        for (Notations notations : note.getNotations()) {
            for (Object object : notations.getTiedOrSlurOrTuplet()) {
                if (object instanceof Arpeggiate) {
                    parsedNote.m_isArpeggiate = true;
                    break;
                }
            }
        }

        int staff = note.getStaff().intValue();
        if (staff > 2 || staff < 1) {
            throw new UnsupportedOperationException("Only supports 2 staffs: " + staff);
        }

        List<_Note> currentTrack = staff == 1
                ? m_currentMeasure.m_notes1
                : m_currentMeasure.m_notes2;

        if (note.getChord() != null) {
            if (parsedNote.m_isRest) {
                throw new IllegalStateException("Can't chord a rest...");
            }

            _Note chordNote = currentTrack.get(currentTrack.size() - 1);
            chordNote.m_pitches.add(parsedPitch);

            // Also, connect any ties
            chordNote.m_tieStart |= parsedNote.m_tieStart;
            chordNote.m_tieEnd |= parsedNote.m_tieEnd;
        } else {
            if (parsedPitch != null) {
                parsedNote.m_pitches.add(parsedPitch);
                parsedNote.m_pitches.addAll(swingChords);
            }
            currentTrack.add(parsedNote);
        }

        /*
         *
         * DEBUG OUTPUT
         *
         */

        System.out.printf("Measure = %d, Staff = %s, Duration = %s",
                m_currentMeasureId,
                staff,
                note.getDuration());

        p(note.getType());

        p(note.getRest());
        p(note.getPitch());

        if (!note.getDot().isEmpty()) {
            System.out.println("--LONG DOT-- ");
        }

        // TODO: Parse sharp / flat / natural accidentals...
        p(note.getAccidental());

        for (Notations notations : note.getNotations()) {
            for (Object object : notations.getTiedOrSlurOrTuplet()) {
                if (object instanceof Arpeggiate) {
                    p(object);
                }
            }
        }

        for (Tie tempTie : note.getTie()) {
            p(tempTie);
        }

        /*
         * If this is specified, it means it's on the same stem / time as the previous note.
         */
        if (note.getChord() != null) {
            System.out.println("--CHORD--");
        }

        /*
         * Properties I don't care about....
         *
         * note.getStem()
         *     This is the note line going up / down, etc
         *
         * note.getBeam()
         *     This is the line connecting the notes.
         *
         * note.getVoice()
         *     I have no idea what it is. It's a string, usually a number?
         *
         * GJM doesn't support staccato
         * note.getNotations() (list)
         *     .getTiedOrSlurOrTuplet() (list)
         *         (Articulations) object
         *             .getAccentOrStrongAccentOrStaccato() (list)
         *                 .getName().toString()k
         *
         * note.getDefaultX()
         * note.getDefaultY()
         *     Print information about note position
         *
         * Don't care about rest display characteristics.
         * note.getRest()
         *     .getDisplayStep()
         *     .getDisplayOctave()
         */
        System.out.println("E-----------------------------------------------------------");
    }

    public void swing() {
        // Only care about swinging track 2, for now
        for (_Measure measure : m_measures) {
            measure.m_notes1 = swingTrack(measure.m_notes1);
        }
    }
    private List<_Note> swingTrack(List<_Note> notes) {
        List<_Note> result = new ArrayList<>();
        int currentBeatTally = 0;
        for (int i = 0; i < notes.size(); ++i) {
            _Note lNote = notes.get(i);
            result.add(lNote);

            if (i + 1 == notes.size()) {
                break;
            }
            _Note rNote = notes.get(i + 1);

            int beatTally = currentBeatTally;
            int beatTallyChange = lNote.m_noteType.m_durationMultiplier / 2;
            if (lNote.m_isDotted) {
                beatTallyChange += beatTallyChange / 2;
            }
            currentBeatTally += beatTallyChange;

            if (beatTally % 8 != 0) {
                continue;
            }

            if (lNote.m_isDotted || rNote.m_isDotted) {
                continue;
            }

            if (lNote.m_isRest || rNote.m_isRest) {
                continue;
            }

            boolean lIsEighth = lNote.m_noteType == _NoteType.EIGHTH;
            boolean rIsEighth = rNote.m_noteType == _NoteType.EIGHTH;
            if (!lIsEighth || !rIsEighth) {
                continue;
            }

            // We can swing these!
            _Note l2Note = lNote.deepClone();

            lNote.m_isSwing = true;
            lNote.m_tieStart = true;
            lNote.m_isTriplet = true;
            lNote.m_swingIndexOverride = 5;
            lNote.m_swingDurationMultiplier= 1.25d;
            // Already added at top of method

            l2Note.m_isSwing = true;
            l2Note.m_tieEnd = true;
            l2Note.m_swingIndexOverride = 5;
            l2Note.m_swingDurationMultiplier= 0.625d;
            result.add(l2Note);

            rNote.m_isSwing = true;
            rNote.m_swingIndexOverride = 6;
            rNote.m_swingDurationMultiplier = 0.75d;
            result.add(rNote);

            // Make sure we skip rNote on the next iteration
            ++i;

            // And correct the beat tally, since we're skipping
            currentBeatTally += beatTallyChange;
        }

        return result;
    }

    public String writeGjm() {
        // Header data
        IndentingStringBuilder sb = new IndentingStringBuilder("\t")
                .li("Version = '1.1.0.0'")
                .push("Notation = {")
                    .li("Version = '1.1.0.0',")
                    .li("NotationName = '%s',", m_notationName)
                    .li("NotationAuthor = '%s',", m_notationAuthor)
                    .li("NotationTranslater = 'SoloGJM',")
                    .li("NotationCreator = 'Miuna (kieve)',")
                    .li("Volume = 1,")
                    .li("BeatsPerMeasure = %d,", m_beatsPerMeasure)
                    .li("BeatDurationType = 'Quarter',") // TODO: Support others
                    .li("NumberedKeySignature = 'C',") // TODO: Figure out how to read this from MXL
                    .push("MeasureBeatsPerMinuteMap = {")
                        .li("{ 0, %d },", BPM) // TODO: Support different BPM per measure. Also, figure out how to read this from MXL
                    .pop("},")
                    .li("MeasureAlignedCount = %d,", m_measures.size())
                .pop("}")
                ;

        // Write the measures
        sb.push("Notation.RegularTracks = {");

        writeTrack(sb, 0);
        writeTrack(sb, 1);
        writeTrack(sb, 2);

        // End of measures
        sb.pop("}");

        return sb.toString();
    }

    private void writeTrack(IndentingStringBuilder sb, int trackIndex) {
        _Clef clef = m_clef1;
        if (trackIndex == 1) {
            clef = m_clef2;
        }

        sb.push("[" + trackIndex + "] = {")
                .push("MeasureKeySignatureMap = {") // TODO: Support multiple keys
                    .li("{ 0, %d },", m_key)
                .pop("},")
                .push("MeasureClefTypeMap = {") // TODO: Support multiple Clefs
                    .li("{ 0, '%s' },", clef.toString())
                .pop("},")
                .push("MeasureInstrumentTypeMap = {")
                    .li("{ 0, 'Piano' },")
                .pop("},")
                .push("MeasureVolumeCurveMap = {")
                    .li("{ 0, { 0.8, 0.7, 0.5, 0.5, 0.7, 0.6, 0.5, 0.4, } },")
                .pop("},")
                .push("MeasureVolumeMap = {")
                    .li("{ 0, %s },", trackIndex == 0 ? "1" : "0.7")
                .pop("},");

        int i = 0;
        for (_Measure measure : m_measures) {
            sb.push("[" + i + "] = {");
            if (trackIndex == 2) {
                // TODO: Support track 3
                sb.li("NotePackCount = 0");
                sb.pop("},");
                continue;
            }

            List<_Note> notes = trackIndex == 0
                    ? measure.m_notes1
                    : measure.m_notes2;

            sb.li("DurationStampMax = 63,") // TODO: Calculate this?
                    .li("NotePackCount = %d,", notes.size());

            int j = 0;
            int stampIndex = 0;
            for (_Note note : notes) {
                sb.push("[" + j + "] = {");

                if (note.m_tieStart || note.m_tieEnd) {
                    String tieValue;
                    if (note.m_tieStart && note.m_tieEnd) {
                        tieValue = "Both";
                    } else if (note.m_tieStart) {
                        tieValue = "Start";
                    } else {
                        tieValue = "End";
                    }
                    sb.li("TieType = '%s',", tieValue);
                }

                if (note.m_isTriplet) {
                    sb.li("Triplet = true,");
                }

                if (note.m_isRest) {
                    sb.li("IsRest = true,");
                }

                if (note.m_isDotted) {
                    sb.li("IsDotted = true,");
                }

                if (note.m_noteType.m_gjmId != null) {
                    sb.li("DurationType = '%s',", note.m_noteType.m_gjmId);
                }

                if (note.m_isArpeggiate) {
                    sb.li("ArpeggioMode ='Upward',");
                }

                sb.li("StampIndex = %d,", stampIndex);

                int duration = (int) Math.round(BASE_DURATION * (60 / (double) BPM))
                        * note.m_noteType.m_durationMultiplier;
                if (note.m_isSwing) {
                    duration = (int) Math.round(duration * note.m_swingDurationMultiplier);
                }
                sb.li("PlayingDurationTimeMs = %d,", duration);

                if (note.m_isRest) {
                    sb.li("ClassicPitchSignCount = 0,");
                } else {
                    sb.li("ClassicPitchSignCount = %d,", note.m_pitches.size());
                }
                sb.push("ClassicPitchSign = {");
                int pitchCount = 0;
                for (_Pitch pitch : note.m_pitches) {
                    int noteIndex = (pitch.m_octave - 1) * 12 + GJM_PITCH_OFFSET
                            + pitch.m_step.m_pitchOffset;

                    int offset = 0;
                    if (m_key < 0 && pitch.m_step.m_flatScale <= m_key) {
                        offset--;
                    } else if (m_key > 0 && pitch.m_step.m_sharpScale >= m_key) {
                        offset++;
                    }

                    noteIndex += offset;

                    // TODO: have to manually calculate key signature
                    // If the key is sharp / flat, have to maybe manually map that?
                    int pitchIndex = noteIndex;// + pitch.m_alter;

                    String alter = "NoControl";

                    sb.li("[%d] = { "
                            + "NumberedSign = %d, "
                            + "PlayingPitchIndex = %d, "
                            + "AlterantType = '%s', "
                            + "RawAlterantType = '%s', "
                            + "Volume = %s, "
                            + "%s },",
                            // TODO: This volumes is mapped / calculated somehow...
                            noteIndex,
                            pitch.m_step.m_sign,
                            pitchIndex,
                            alter,
                            alter,
                            (note.m_tieEnd ? "0.00" : "0.50"),
                            (note.m_isArpeggiate && pitchCount > 0
                                    ? String.valueOf(pitchCount * 100)
                                    : "" ));

                    pitchCount++;
                }
                sb.pop("},");


                sb.pop("},");
                if (note.m_isSwing) {
                    stampIndex += note.m_swingIndexOverride;
                } else {
                    stampIndex += note.m_noteType.m_durationMultiplier;
                }
                j++;
            }

            sb.pop("},");
            i++;
        }

        sb.pop("},");
    }
}
