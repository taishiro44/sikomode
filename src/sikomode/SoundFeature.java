/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sikomode;

import java.util.Arrays;

/**
 * 音の特徴を格納するクラス
 *
 * @author tanakataishiro
 */
public class SoundFeature {

    public int[] num;
    public int[] range;
    public long[] tick;

    public SoundFeature(int size) {
        //指定されたサイズで初期化 & 0埋め
        this.num = new int[size];
        this.range = new int[size];
        this.tick = new long[size];
        Arrays.fill(this.num, 0);
        Arrays.fill(this.range, 0);
        Arrays.fill(this.tick, 0);
    }

}
