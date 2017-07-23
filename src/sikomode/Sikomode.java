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

        String filePathRecode = "midi/USERSONG019.MID";
        String filePathScore = "midi/yakusoku.mid";
        Midi recode = new Midi(filePathRecode);
        Midi score = new Midi(filePathScore);

        Modeling model = new Modeling();
        model.setMidi(recode, score);
        model.culcModel();

    }
}
