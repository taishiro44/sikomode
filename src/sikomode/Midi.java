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
import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Track;

/**
 * 読み込んだMIDIファイルを管理するクラス。
 * @author tanakataishiro
 */
public class Midi {
    
    private File file;
    private Sequence sequence;
    private Track[] tracks;
    private MidiEvent midiEvent;
    private byte [] midiMessage;
    
    public Midi() {
    }
    
    /**
     * MIDIファイルを読み込む
     * @param filePath
     * @return boolean ファイルの読み込み成否
     */
    public boolean readMidiFile(String filePath){
        this.file = new File(filePath);
        boolean canRead = this.file.canRead();
        if(canRead){
            try {
                this.sequence = MidiSystem.getSequence(this.file);
            } catch (InvalidMidiDataException | IOException ex) {
                Logger.getLogger(Midi.class.getName()).log(Level.SEVERE, null, ex);
                return false;
            }
            this.tracks = this.sequence.getTracks();
            System.out.println("\"" + filePath + "\"" + " read success.");
        } else {
            System.out.println("\"" + filePath + "\"" + " can not read.");
        }
        return canRead;
    }
    
    /**
     * 読み込んだMIDIファイルの中身を標準出力する。
     */
    public void print(){
        for(int k = 0; k < this.tracks.length; k++){
            System.out.println("track number : " + k);
            for(int i = 0; i < this.tracks[k].size(); i++){
                try{
                    this.midiEvent = this.tracks[k].get(i);
                    this.midiMessage = this.midiEvent.getMessage().getMessage();
                }catch(ArrayIndexOutOfBoundsException ex){
                    System.out.println("----------------------------------------");
                    break;
                }
                System.out.print("index : " + i + ", Tick : " + this.midiEvent.getTick() + ", ");
                for(int j = 0; j < this.midiMessage.length; j++){
                    System.out.print((0xFF & this.midiMessage[j]) + ", ");
                }
                System.out.println("");
            }
        }
    }
    
}
