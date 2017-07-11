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
        String filePath = "midi/yakusoku.mid";
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
        String filePath = "midi/yakusoku.mid";
        boolean result = instance.readMidiFile(filePath);
        instance.print();
    }

    /**
     * Test of getNoteOn method, of class Midi.
     */
    @Test
    public void testGetSound() {
        System.out.println("getSound");
        Midi instance = new Midi();
        String filePath = "midi/yakusoku.mid";
        instance.readMidiFile(filePath);
        byte[] edf = new byte[128];
        Arrays.fill(edf, (byte)-1);
        String str;
        for(long tick = 0;; tick++){
            byte[] result = instance.getSound(tick);
            if(result[0] == -1) {
                break;
            }
            if(Arrays.equals(result, edf)){
                break;
            }
            str = instance.byteArray2Code(result);
            System.out.println("Tick : " + tick + ", Name : " + str);
        }
    }

    /**
     * Test of byteArray2Code method, of class Midi.
     */
    @Test
    public void testByteArray2Code() {
//        System.out.println("byteArray2Code");
//        byte[] noteOnArray = null;
//        Midi instance = new Midi();
//        String expResult = "";
//        String result = instance.byteArray2Code(noteOnArray);
//        assertEquals(expResult, result);
//        // TODO review the generated test code and remove the default call to fail.
//        fail("The test case is a prototype.");
    }

    /**
     * Test of printFormat method, of class Midi.
     */
    @Test
    public void testPrintFormat() {
        System.out.println("printFormat");
        Midi instance = new Midi();
        String filePath = "midi/yakusoku.mid";
        boolean result = instance.readMidiFile(filePath);
        instance.printFormat();
    }

    /**
     * Test of getNoteOnTick method, of class Midi.
     */
    @Test
    public void testGetNoteOnTick() {
        System.out.println("getNoteOnTick");
        Midi instance = new Midi();
        String filePath = "midi/USERSONG013.MID";
        instance.readMidiFile(filePath);
        long expResult = 0L;
        while(true){
            long result = instance.getNoteOnTick();
            if(result == -1) break;
            System.out.println("Note on Tick : " + result);
        }
    }

}
