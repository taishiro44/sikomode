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
//        System.out.println("readMidiFile");
//        String filePath = "midi/yakusoku.mid";
//        Midi instance = new Midi();
//        boolean expResult = true;
//        boolean result = instance.readMidiFile(filePath);
//        assertEquals(expResult, result);
    }

    /**
     * Test of print method, of class Midi.
     */
    @Test
    public void testPrint() {
//        System.out.println("print");
//        Midi instance = new Midi();
//        String filePath = "midi/yakusoku.mid";
//        boolean result = instance.readMidiFile(filePath);
//        instance.print();
    }

    /**
     * Test of getNoteOn method, of class Midi.
     */
    @Test
    public void testGetNoteOn() {
        System.out.println("getNoteOn");
        Midi instance = new Midi();
        String filePath = "midi/yakusoku.MID";
        instance.readMidiFile(filePath);
        byte[] edf = new byte[128];
        Arrays.fill(edf, (byte)-1);
        String str;
        long tick;
        for(;;){
            tick = instance.getNoteOnTick();
            if(tick < 0) break;
            byte[] result = instance.getNoteOn(tick);
            if(result[0] < 0) {
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
//        System.out.println("printFormat");
//        Midi instance = new Midi();
//        String filePath = "midi/yakusoku.mid";
//        boolean result = instance.readMidiFile(filePath);
//        instance.printFormat();
    }

    /**
     * Test of getNoteOnTick method, of class Midi.
     */
    @Test
    public void testGetNoteOnTick() {
//        System.out.println("getNoteOnTick");
//        Midi instance = new Midi();
//        String filePath = "midi/yakusoku.mid";
//        instance.readMidiFile(filePath);
//        long expResult = 0L;
//        while(true){
//            long result = instance.getNoteOnTick();
//            if(result < 0) {
//                System.out.println("result < 0");
//                break;
//            }
//            System.out.println("Note on Tick : " + result);
//        }
    }

}
