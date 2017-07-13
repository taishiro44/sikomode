/* To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sikomode;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiFileFormat;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Track;

/**
 * 読み込んだMIDIファイルを管理するクラス。
 *
 * @author tanakataishiro
 */
public class Midi {

    private File file; //読み込むファイル
    private Sequence sequence;
    private Track[] tracks;
    private MidiEvent midiEvent;
    private MidiFileFormat midiFileFormat;
    private byte[] midiMessage;
    private int midiFileType; //smfのフォーマットタイプ(0, 1 or 2)
    private long firstNoteOnTick; //最初の音が鳴ったときのTick
    private byte[] currentNoteOn;
    private int currentIndex;
    private int[] currentIndices;
    private int[] noteOnIndices;
    private int NOTE_ON = 144;
    private int NOTE_OFF = 128;
    private int NOTE_NUM_MAX = 128;

    public Midi() {
        this.currentNoteOn = new byte[this.NOTE_NUM_MAX];
        Arrays.fill(this.currentNoteOn, (byte) 0); //配列を0埋め
        this.currentIndex = 0;
    }

    /**
     * MIDIファイルを読み込んだ後に、ファイルの値などを用いて初期化するやつとかを初期化する。
     */
    private void initialize() {
        this.midiFileType = this.midiFileFormat.getType();
        this.tracks = this.sequence.getTracks();
        this.currentIndices = new int[this.tracks.length];
        this.noteOnIndices = new int[this.tracks.length];
        Arrays.fill(this.currentIndices, 0);
        Arrays.fill(this.noteOnIndices, 0);
        this.setFirstNoteOn();
    }

    /**
     * MIDIファイルを読み込む
     *
     * @param filePath
     * @return boolean ファイルの読み込み成否
     */
    public boolean readMidiFile(String filePath) {
        this.file = new File(filePath);
        boolean canRead = this.file.canRead();
        if (!canRead) {
            System.out.println("\"" + filePath + "\"" + " can not read.");
            return false;
        }
        try {
            this.sequence = MidiSystem.getSequence(this.file);
            this.midiFileFormat = MidiSystem.getMidiFileFormat(this.file);
        } catch (InvalidMidiDataException | IOException ex) {
            System.out.println("\"" + filePath + "\"" + " can not read.");
            Logger.getLogger(Midi.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        System.out.println("\"" + filePath + "\"" + " read success.");
        //初期化
        this.initialize();
        return true;
    }

    /**
     * 最初に音がなった瞬間のTickを取得してfirstOnTickに格納する。
     */
    private void setFirstNoteOn() {
        if (this.midiFileType == 0) {
            for (int i = 0; i < this.tracks[0].size(); i++) {
                this.midiEvent = this.tracks[0].get(i);
                this.midiMessage = this.midiEvent.getMessage().getMessage();
                if ((this.midiMessage[0] & 0xff) == this.NOTE_ON) {
                    this.firstNoteOnTick = this.midiEvent.getTick();
                    break;
                }
            }
        } else if (this.midiFileType == 1) {
            this.firstNoteOnTick = Long.MAX_VALUE; //十分に大きい整数で初期化しておく。
            for (Track track : this.tracks) {
                for (int i = 0; i < track.size(); i++) {
                    this.midiEvent = track.get(i);
                    this.midiMessage = this.midiEvent.getMessage().getMessage();
                    if ((this.midiMessage[0] & 0xff) == this.NOTE_ON) {
                        if (this.firstNoteOnTick > this.midiEvent.getTick()) {
                            this.firstNoteOnTick = this.midiEvent.getTick();
                            break;
                        }
                    }
                }
            }
        }
        System.out.println("first on note Tick : " + this.firstNoteOnTick);
    }

    /**
     * ファイル先頭から読み込み、NoteOnとなっている時のTickを取り出す。<\br>
     * ファイルの読み込みインデックスは保存されるため、 <\br>
     * 繰り返し実行することでファイル末尾まで読み込めます。<\br>
     *
     * @return
     */
    public long getNoteOnTick() {
        long returnValue = 0;
        if (this.midiFileType == 0) {
            returnValue = getNoteOnTickSmf0();
        } else if (this.midiFileType == 1) {
            returnValue = getNoteOnTickSmf1();
        }
        return returnValue;
    }

    private long getNoteOnTickSmf0() {
        long tick = -1; //ファイル末尾まで達したら-1を返す。
        for (int i = this.noteOnIndices[0]; i < this.tracks[0].size(); i++) {
            this.midiEvent = this.tracks[0].get(i);
            this.midiMessage = this.midiEvent.getMessage().getMessage();
            if ((this.midiMessage[0] & 0xff) == this.NOTE_ON) {
                tick = this.midiEvent.getTick();
                this.noteOnIndices[0] = i + 1;
                break;
            }
        }
        return tick;
    }

    private long getNoteOnTickSmf1() {
        long tick = Long.MAX_VALUE; //return value
        for (int i = 0; i < this.tracks.length; i++) {
            for (int j = this.noteOnIndices[i]; j < tracks[i].size(); j++) {
                this.midiEvent = tracks[i].get(j);
                this.midiMessage = this.midiEvent.getMessage().getMessage();
                if ((this.midiMessage[0] & 0xff) == this.NOTE_ON) {
                    if ((this.midiMessage[2] & 0xff) > 0) { //ベロシティ>0でNoteOn
                        if (this.midiEvent.getTick() < tick) {
                            tick = this.midiEvent.getTick();
                            this.noteOnIndices[i] = j + 1; //インデックスを保存
                            break;
                        }
                    }
                }
            }
        }
        if (tick == Long.MAX_VALUE) {
            tick = -1; //ファイル末尾まで達したら-1を返す。
        }
        return tick;
    }

    /**
     * 指定したTickに出ている音を取得する。 ただし、第一音がなる瞬間のTickをTick == 0として扱う。<\br>
     * (本当は、最初の音がなるまでに無音の部分があるため、Tick == 0は無音だったり、設定だったりする。<\br>
     * 差分を考えるときにTickを合わせた方が簡単だから、Tick==0を最初の音が鳴る瞬間に統一する。）<\br>
     *
     * @param tick
     * @return tickがファイルの末尾を超えたとき-1埋めした配列を返す。
     */
    public byte[] getSound(long tick) {
        byte[] returnValue = new byte[this.NOTE_NUM_MAX];
        if (this.midiFileType == 0) {
            returnValue = getSoundSmf0(tick + this.firstNoteOnTick);
        } else if (this.midiFileType == 1) {
            returnValue = getSoundSmf1(tick + this.firstNoteOnTick);
        }
        return returnValue;
    }

    private byte[] getSoundSmf0(long tick) {
        this.midiEvent = this.tracks[0].get(this.tracks[0].size() - 1);
        if (tick > this.midiEvent.getTick()) { //引数のtickがファイルを超えるとき
            Arrays.fill(this.currentNoteOn, (byte) -1);
            return this.currentNoteOn;
        }
        for (int i = this.currentIndex; i < this.tracks[0].size(); i++) {
            this.midiEvent = this.tracks[0].get(i);
            if (this.midiEvent.getTick() > tick) {
                break;
            } else if (this.midiEvent.getTick() >= tick) {
                this.midiMessage = this.midiEvent.getMessage().getMessage();
                if ((this.midiMessage[0] & 0xff) == this.NOTE_ON) {
                    this.currentNoteOn[this.midiMessage[1]] = 1;
                }
                if ((this.midiMessage[0] & 0xff) == this.NOTE_OFF) {
                    this.currentNoteOn[this.midiMessage[1]] = 0;
                }
                this.currentIndex = i;
            }
        }
        return this.currentNoteOn;
    }

    /**
     * yakusoku.mid の形式で、NOTE_OFFではなくベロシティ0でNOTE_OFFを表現している。<\br>
     * 毎回ファイル先頭から探索してる。くっそ効率悪い。<\br>
     *
     * @param tick
     * @return
     */
    private byte[] getSoundSmf1(long tick) {
        if (tick > this.midiEvent.getTick()) { //引数のtickがファイルを超えるとき
            Arrays.fill(this.currentNoteOn, (byte) -1);
            return this.currentNoteOn;
        }
        for (int i = 0; i < this.tracks.length; i++) {
            for (int j = this.currentIndices[i]; j < tracks[i].size(); j++) {
                this.midiEvent = tracks[i].get(j);
                if (this.midiEvent.getTick() > tick) {
                    break;
                } else if (this.midiEvent.getTick() >= tick) {
                    this.midiMessage = this.midiEvent.getMessage().getMessage();
                    if ((this.midiMessage[0] & 0xff) == this.NOTE_ON) {
                        if ((this.midiMessage[2] & 0xff) > 0) {
                            this.currentNoteOn[this.midiMessage[1]] = 1;
                        } else { //ベロシティ0でNote_Off
                            this.currentNoteOn[this.midiMessage[1]] = 0;
                        }
                    }
                    this.currentIndices[i] = j; //すでに探索した部分は探索しない。
                }
            }
        }
        return this.currentNoteOn;
    }

    /**
     * NoteOnのバイト配列を音名に変換する関数です。 変換は以下のurl参照。ヤマハ式で変換しました。<\br>
     *
     * @see <a href="http://www.g200kg.com/jp/docs/tech/notefreq.html">DTM技術情報</a>
     * @param noteOnArray
     * @return
     */
    public String byteArray2Code(byte[] noteOnArray) {
        String[] code = {"C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "H"};
        String result = "";
        byte[] zero = new byte[this.NOTE_NUM_MAX];
        Arrays.fill(zero, (byte)0);
        if(Arrays.equals(zero, noteOnArray)){
            return "NA";
        }
        for (int i = 0; i < noteOnArray.length; i++) {
            if (noteOnArray[i] == 1) {
                result += code[i % 12] + (i / 12 - 2) + ",";
            }
        }
        return result;
    }

    /**
     * 読み込んだMIDIファイルのフォーマットを標準出力する。
     */
    public void printFormat() {
        System.out.println("File Format");
        System.out.println("type(0, 1 or 2) : " + this.midiFileFormat.getType());
        float divisionType = this.midiFileFormat.getDivisionType();
        String divisionTypeStr = "";
        if (divisionType == Sequence.PPQ) { //"switchに置換しろ"ってでるけど、switchはfloatに非対応
            divisionTypeStr = "PPQ";
        } else if (divisionType == Sequence.SMPTE_24) {
            divisionTypeStr = "SMPTE_24";
        } else if (divisionType == Sequence.SMPTE_25) {
            divisionTypeStr = "SMTPE_25";
        } else if (divisionType == Sequence.SMPTE_30) {
            divisionTypeStr = "SMPTE_30";
        } else if (divisionType == Sequence.SMPTE_30DROP) {
            divisionTypeStr = "SMTPE_30DROP";
        }
        System.out.println("divisionType : " + divisionTypeStr);
        System.out.println("resolution" + this.midiFileFormat.getResolution());
    }

    /**
     * 読み込んだMIDIファイルの中身を標準出力する。
     */
    public void print() {
        for (int k = 0; k < this.tracks.length; k++) {
            System.out.println("Track Number : " + k);
            for (int i = 0; i < this.tracks[k].size(); i++) {
                this.midiEvent = this.tracks[k].get(i);
                this.midiMessage = this.midiEvent.getMessage().getMessage();
                System.out.print("Index : " + i + ", Tick : " + this.midiEvent.getTick() + ", ");
                for (int j = 0; j < this.midiMessage.length; j++) {
                    System.out.print((0xFF & this.midiMessage[j]) + ", ");
                }
                System.out.println("");
            }
        }
    }

}
