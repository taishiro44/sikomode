/* To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sikomode;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
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
    private int resolution; //タイミング解像度
    private long firstNoteOnTick; //最初の音が鳴ったときのTick
    private byte[] currentNoteOn;
    private int[] noteOnIndices;
    private long tickMax;
    private int NOTE_ON = 144;
    private int NOTE_OFF = 128;
    private int NOTE_NUM_MAX = 128;

    public Midi() {
        this.currentNoteOn = new byte[this.NOTE_NUM_MAX];
        Arrays.fill(this.currentNoteOn, (byte) 0); //配列を0埋め
    }

    /**
     * MIDIファイルを読み込んだ後に、ファイルの値などを用いて初期化するやつとかを初期化する。
     */
    private void initialize() {
        this.midiFileType = this.midiFileFormat.getType();
        this.tracks = this.sequence.getTracks();
        this.noteOnIndices = new int[this.tracks.length];
        Arrays.fill(this.noteOnIndices, 0);
        this.tickMax = -1;
        long tickTemp;
        for (Track track : this.tracks) {
            this.midiEvent = track.get(track.size() - 1);
            tickTemp = this.midiEvent.getTick();
            if (tickTemp > tickMax) {
                tickMax = tickTemp;
            }
        }
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
     * 最初に音がなった瞬間のTickを取得してfirstNoteOnTickに格納する。
     */
    private long getFirstNoteOn() {
        long firstTick = Long.MAX_VALUE; //十分に大きい整数で初期化しておく。
        if (this.midiFileType == 0) {
            for (int i = 0; i < this.tracks[0].size(); i++) {
                this.midiEvent = this.tracks[0].get(i);
                this.midiMessage = this.midiEvent.getMessage().getMessage();
                if ((this.midiMessage[0] & 0xff) == this.NOTE_ON) {
                    firstTick = this.midiEvent.getTick();
                    break;
                }
            }
        } else if (this.midiFileType == 1) {
            for (Track track : this.tracks) {
                for (int i = 0; i < track.size(); i++) {
                    this.midiEvent = track.get(i);
                    this.midiMessage = this.midiEvent.getMessage().getMessage();
                    if ((this.midiMessage[0] & 0xff) == this.NOTE_ON) {
                        if (firstTick > this.midiEvent.getTick()) {
                            firstTick = this.midiEvent.getTick();
                            break;
                        }
                    }
                }
            }
        }
        return firstTick;
    }

    /**
     * ファイル先頭から読み込み、NoteOnとなっている時のTickを取り出す。<\br>
     * ファイルの読み込みインデックスは保存されるため、 <\br>
     * 繰り返し実行することでファイル末尾まで読み込めます。<\br>
     * ここで返されるtickは最初の音が鳴ったときのtickを0として返します。<\br>
     * ファイル末尾まで達したら負の値を返します。
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
        returnValue -= this.firstNoteOnTick;
        return returnValue;
    }

    private long getNoteOnTickSmf0() {
        //ファイル末尾まで達したら-1を返す。
        long tick = -1;
        //保持したindexからファイルを探索
        for (int i = this.noteOnIndices[0]; i < this.tracks[0].size(); i++) {
            this.midiEvent = this.tracks[0].get(i);
            this.midiMessage = this.midiEvent.getMessage().getMessage();
            //NoteOnならtickを更新
            if ((this.midiMessage[0] & 0xff) == this.NOTE_ON) {
                tick = this.midiEvent.getTick();
                this.noteOnIndices[0] = i + 1;
                //同一tickに複数の音があるためループでindexの更新
                for (int j = this.noteOnIndices[0]; j < this.tracks[0].size(); j++) {
                    this.midiEvent = this.tracks[0].get(j);
                    //同じtickがあればindexを更新する
                    if (this.midiEvent.getTick() == tick) {
                        this.noteOnIndices[0] = j + 1;
                        continue;
                    }
                    break;
                }
                break;
            }
        }
        return tick;
    }

    private long getNoteOnTickSmf1() {
        long returnValue;
        long[] tick = new long[this.tracks.length];
        //-1で初期化
        Arrays.fill(tick, -1);
        //複数trackからNoteOnのindexとtickを取得
        for (int i = 0; i < this.tracks.length; i++) {
            //trackの中を読む
            for (int j = this.noteOnIndices[i]; j < tracks[i].size(); j++) {
                this.midiEvent = tracks[i].get(j);
                this.midiMessage = this.midiEvent.getMessage().getMessage();
                //NoteOnかつベロシティ>0のとき、Indexとtickを保持
                if ((this.midiMessage[0] & 0xff) == this.NOTE_ON
                        && (this.midiMessage[2] & 0xff) > 0) {
                    tick[i] = this.midiEvent.getTick();
                    //NoteOnのindexを保持
                    this.noteOnIndices[i] = j;
                    //複数のNoteONが同一tickにある場合、Indexを更新する。
                    for (int k = this.noteOnIndices[i] + 1; k < tracks[i].size(); k++) {
                        this.midiEvent = tracks[i].get(k);
                        if (this.midiEvent.getTick() == tick[i]) {
                            this.noteOnIndices[i] = k;
                            continue;
                        }
                        break;
                    }
                    break;
                }
            }
        }
        //tickの最小値を取得
        long min = Long.MAX_VALUE;
        for (int i = 0; i < tick.length; i++) {
            //NoteOnが存在しないものはtick == -1 のため、if()ではじく
            if (tick[i] > 0) {
                if (min > tick[i]) {
                    min = tick[i];
                }
            }
        }
        //もし一つもNoteOnがなければ、minはLong.MAX_VALUE
        if (min == Long.MAX_VALUE) {
            returnValue = -1;
        } else {
            //最小のtickを持つtrackのindexを進める
            for (int i = 0; i < tick.length; i++) {
                if (tick[i] == min) {
                    this.noteOnIndices[i]++;
                }
            }
            returnValue = min;
        }
        return returnValue;
    }

    /**
     * 指定したTickに出ている音を取得する。 ただし、第一音がなる瞬間のTickをTick == 0として扱う。<\br>
     * (本当は、最初の音がなるまでに無音の部分があるため、Tick == 0は無音だったり、設定だったりする。<\br>
     * 差分を考えるときにTickを合わせた方が簡単だから、Tick==0を最初の音が鳴る瞬間に統一する。）<\br>
     * NoteOnのみを検出するように変更する。<\br>
     *
     * @param tick
     * @return tickがファイルの末尾を超えたとき-1埋めした配列を返す。
     */
    public byte[] getNoteOn(long tick) {
        byte[] returnValue = new byte[this.NOTE_NUM_MAX];
        if (this.midiFileType == 0) {
            returnValue = getNoteOnSmf0(tick + this.firstNoteOnTick);
        } else if (this.midiFileType == 1) {
            returnValue = getNoteOnSmf1(tick + this.firstNoteOnTick);
        }
        return returnValue;
    }

    private byte[] getNoteOnSmf0(long tick) {
        byte[] returnValue = new byte[this.NOTE_NUM_MAX];
        Arrays.fill(returnValue, (byte) 0);
        if (tick > this.tickMax) { //引数のtickがファイルを超えるとき
            Arrays.fill(returnValue, (byte) -1);
            return returnValue;
        }
        //保持したindexからファイルを探索
        for (int i = 0; i < this.tracks[0].size(); i++) {
            this.midiEvent = this.tracks[0].get(i);
            //指定したtickならば
            if (this.midiEvent.getTick() == tick) {
                //NoteOnならindexを更新, resutValueに格納
                //同一tickに複数の音があるためループ
                for (int j = i; j < this.tracks[0].size(); j++) {
                    this.midiEvent = this.tracks[0].get(j);
                    //同じtickがあればindexを更新する
                    if (this.midiEvent.getTick() == tick) {
                        //NoteOnの場所に1を立てる
                        this.midiMessage = this.midiEvent.getMessage().getMessage();
                        if ((this.midiMessage[0] & 0xff) == this.NOTE_ON) {
                            returnValue[this.midiMessage[1]] = 1;
                        }
                        continue;
                    }
                    break;
                }
                break;
            } else if (this.midiEvent.getTick() > tick) {
                break;
            }
        }
        return returnValue;
    }

    /**
     * yakusoku.mid の形式で、NOTE_OFFではなくベロシティ0でNOTE_OFFを表現している。<\br>
     * 毎回ファイル先頭から探索してる。くっそ効率悪い。<\br>
     *
     * @param tick
     * @return
     */
    private byte[] getNoteOnSmf1(long tick) {
        byte[] returnValue = new byte[this.NOTE_NUM_MAX];
        Arrays.fill(returnValue, (byte) 0);
        //引数のtickがファイルを超えるとき
        if (tick > this.tickMax) {
            Arrays.fill(returnValue, (byte) -1);
            return returnValue;
        }
        //全trackを探索
        for (Track track : this.tracks) {
            //指定indexからNoteOnまで探索
            for (int j = 0; j < track.size(); j++) {
                this.midiEvent = track.get(j);
                if (this.midiEvent.getTick() > tick) {
                    break;
                } else if (this.midiEvent.getTick() == tick) {
                    //複数のNoteONが同一tickにある場合
                    for (int k = j; k < track.size(); k++) {
                        this.midiEvent = track.get(k);
                        if (this.midiEvent.getTick() == tick) {
                            //NoteOnがあればreturnValueに格納
                            this.midiMessage = this.midiEvent.getMessage().getMessage();
                            if ((this.midiMessage[0] & 0xff) == this.NOTE_ON
                                    && (this.midiMessage[2] & 0xff) > 0) {
                                returnValue[this.midiMessage[1]] = 1;
                            }
                            continue;
                        }
                        break;
                    }
                    break;
                }
            }
        }
        return returnValue;
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
        Arrays.fill(zero, (byte) 0);
        if (Arrays.equals(zero, noteOnArray)) {
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
     * タイミング解像度を取得する。
     * @return 
     */
    public int getResolution() {
        return this.resolution;
    }

    /**
     * MIDIファイルからフォーマットなどの情報を読み込む
     */
    private void setFormat(){
        //タイミング解像度
        this.resolution = this.midiFileFormat.getResolution();
        //smf0, smf1, smf2のいずれか
        this.midiFileType = this.midiFileFormat.getType();
        //トラック取得
        this.tracks = this.sequence.getTracks();
    }
    
    /**
     * 読み込んだMIDIファイルのフォーマットを標準出力する。
     */
    public void printFormat() {
        System.out.println("File Format");
        System.out.println("type(0, 1 or 2) : " + this.midiFileFormat.getType());
        float divisionType = this.midiFileFormat.getDivisionType();
        String divisionTypeStr = "";
        //"switchに置換しろ"ってでるけど、switchはfloatに非対応
        if (divisionType == Sequence.PPQ) {
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
