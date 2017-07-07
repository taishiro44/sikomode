/* To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sikomode;

import java.io.File;
import java.io.IOException;
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
    private long firstOnNoteTick; //最初の音が鳴ったときのTick

    public Midi() {
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
        this.midiFileType = this.midiFileFormat.getType();
        this.tracks = this.sequence.getTracks();
        this.setFirstOnNote();
        System.out.println("\"" + filePath + "\"" + " read success.");
        return true;
    }

    /**
     * 最初に音がなった瞬間のTickを取得してfirstOnTickに格納する。
     */
    private void setFirstOnNote() {
        if (this.midiFileType == 0) {
            for (int i = 0; i < this.tracks[0].size(); i++) {
                this.midiEvent = this.tracks[0].get(i);
                this.midiMessage = this.midiEvent.getMessage().getMessage();
                if ((this.midiMessage[0] & 0xff) == 144) { //本当はコードがある。
                    this.firstOnNoteTick = this.midiEvent.getTick();
                    break;
                }
            }
        } else if (this.midiFileType == 1) {
            this.firstOnNoteTick = 99999999; //十分に大きい整数で初期化しておく。
            for (Track track : this.tracks) {
                for (int i = 0; i < track.size(); i++) {
                    this.midiEvent = track.get(i);
                    this.midiMessage = this.midiEvent.getMessage().getMessage();
                    if ((this.midiMessage[0] & 0xff) == 144) {
                        if (this.firstOnNoteTick > this.midiEvent.getTick()) {
                            this.firstOnNoteTick = this.midiEvent.getTick();
                            break;
                        }
                    }
                }
            }
        }
        System.out.println("first on note Tick : " + this.firstOnNoteTick);
    }

    /**
     * 読み込んだMIDIファイルの中身を標準出力する。
     */
    public void print() {
        System.out.println("File Format");
        System.out.println("type(0, 1 or 2) : " + this.midiFileFormat.getType());
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
