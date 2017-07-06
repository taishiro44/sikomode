/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sikomode;

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
    
}
