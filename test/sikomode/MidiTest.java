/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sikomode;

import java.util.Arrays;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author tanakataishiro
 */
public class MidiTest {

    public MidiTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Midiファイルを読み込む関数のテスト
     */
    @Test
    public void testReadMidiFile() {
        System.out.println("readMidiFile");
        String filePath = "midi/USERSONG013.MID";
        Midi instance = new Midi();
        boolean expResult = true;
        boolean result = instance.readMidiFile(filePath);
        assertEquals(expResult, result);
    }

    /**
     * Test of print method, of class Midi.
     */
    @Test
    public void testPrint() {
        System.out.println("print");
        Midi instance = new Midi();
        String filePath = "midi/USERSONG013.MID";
        boolean result = instance.readMidiFile(filePath);
        instance.print();
    }

    /**
     * Test of getNoteOn method, of class Midi.
     */
    @Test
    public void testGetNoteOn() {
        System.out.println("getNoteOn");
        Midi instance = new Midi();
        String filePath = "midi/USERSONG013.MID";
        instance.readMidiFile(filePath);
        byte[] edf = new byte[128];
        Arrays.fill(edf, (byte)-1);
        String str;
        for(long tick = 0;; tick++){
            byte[] result = instance.getNoteOn(tick);
            if(Arrays.equals(result, edf)){
                break;
            }
            str = instance.byteArray2Code(result);
            System.out.println("Tick : " + tick + ", Name : " + str);
            
        }
    }

}
