/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sikomode;

/**
 *
 * @author tanakataishiro
 */
public class Sikomode {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here

        String filePathRecoding = "midi/USERSONG019.MID";
        String filePathScore = "midi/yakusoku.mid";
        Midi recoding = new Midi(filePathRecoding);
        Midi score = new Midi(filePathScore);
        
        //タイミング解像度==1920
        //録音は最初の音を基準にすると、
        //最初の音のミスが全体に響く。
        //メトロノームのtickにあわせる。
        //960 * 25 = 24000 は、ファイルの中身を見て決めた
        recoding.firstNoteOnTick = 960 * 25;

        Modeling model = new Modeling();
        model.setMidi(recoding, score);
        model.culcModel();
        String advice = model.getAdvice();

        System.out.println(advice);
    }
}
