/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sikomode;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ユーザをモデリングするクラス
 *
 * @author tanakataishiro
 */
public class Modeling {

    private Midi recode;
    private Midi score;

    public Modeling() {
    }

    /**
     * モデルを計算するMidiファイルのオブジェクトをセットする。<br>
     * @param recode
     * @param score 
     */
    public void setMidi(Midi recode, Midi score) {
        this.recode = recode;
        this.score = score;
    }

    /**
     * ユーザーのモデルを生成する。<br>
     * ユーザーがミスをした前後１小節の音の数と音域の変化量をモデルとする。<br>
     */
    public void culcModel() {
        List<Long> tickList = this.calcDiffece();
        byte[] soundScore;
        int[] soundRange = new int[tickList.size()];
        int[] soundNum = new int[tickList.size()];
        Arrays.fill(soundNum, 0);
        long tickRange = 60; //指定したtickの前後(+-30)を見る。
        //tickRangeで指定した時間の間の音を全部足したもの
        int[] soundSum = new int[128];
        for (int i = 0; i < tickList.size(); i++) {
            Arrays.fill(soundSum, (byte) 0); //0埋めで初期化
            long t = tickList.get(i);
            //指定したtickの前後を見る。
            //値の範囲の60は１小節の半分。(コレはmidiのタイミング解像度の半分)
//            System.out.println("t : " + t);
            for (long range = (t - tickRange); range < (t + tickRange); range++) {
                if (range < 0) { //ファイルの先頭以前の部分ならばcontinue
                    continue;
                }
                soundScore = this.score.getNoteOn(range);
                //ファイル末尾ならば終了
                if (soundScore[0] == -1) {
                    break;
                }
                //音の総和をとる
                for (int j = 0; j < soundScore.length; j++) {
                    soundSum[j] += soundScore[j];
                }
            }
            int minIdx = Integer.MAX_VALUE;
            int maxIdx = Integer.MIN_VALUE;

            for (int j = 0; j < soundSum.length; j++) {
                if (soundSum[j] != 0) {
                    if (minIdx > j) {
                        minIdx = j;
                    }
                    if (maxIdx < j) {
                        maxIdx = j;
                    }
                }
            }

//            System.out.println(Arrays.toString(soundSum));
            //差分の結果、抽出したtickの前後一小節の音域
            soundRange[i] = (maxIdx - minIdx);
            //音の数
            for (int j = 0; j < soundSum.length; j++) {
                soundNum[i] += soundSum[j];
            }
        }
        double rangeAve = 0;
        double numAve = 0;

        //結果をファイルに保存
        this.save2File(soundRange, soundNum);

        for (int i = 0; i < soundRange.length; i++) {
            rangeAve += soundRange[i];
            numAve += soundNum[i];
        }
        rangeAve /= soundRange.length;
        numAve /= soundNum.length;
        System.out.println("平均");
        System.out.println("幅 : " + rangeAve + ", 数 : " + numAve);

    }

    /**
     * 録音データと楽譜データの差分を取る。<br>
     *
     * @return 差があるTickのリスト
     */
    private List<Long> calcDiffece() {
        long tick;
        int recodeResolution = this.recode.getResolution();
        int scoreResolution = this.score.getResolution();
        double ratio = recodeResolution / scoreResolution;
        byte[] soundRecode = new byte[128];
        byte[] soundScore;
        List<Long> tickList = new ArrayList<>();
        boolean isDifferent;
        long range = recodeResolution / 16;
        while (true) {
            tick = this.score.getNoteOnTick();
            if (tick < 0) {
                break;
            }
            soundScore = score.getNoteOn(tick);
            //録音から読み込むときは、指定tickにゆとりを持たせる。
            //タイミング解像度1920で、指定tickからだけの読み込みはシビアすぎる。
            Arrays.fill(soundRecode, (byte) 0);
            long temp = (long) (tick * ratio);
            for (long i = (temp - range); i < (temp + range); i++) {
                byte[] s = this.recode.getNoteOn(i);
                for (int j = 0; j < s.length; j++) {
                    if (s[j] == 1) {
                        soundRecode[j] = 1;
                    }
                }
            }
            //バイト配列の差分処理
            //まずは二つの配列が違うということが分ればok
            isDifferent = !Arrays.equals(soundRecode, soundScore);
            //もし異なれば、tickを記録。
            if (isDifferent) {
                tickList.add(tick);
            }
        }
        return tickList;
    }

    /**
     * x, y の二次元の情報をファイルに保存します。<br>
     * x, y は空白区切りで保存されます。<br>
     * x, y の配列のサイズは同じでないといけません。<br>
     */
    private void save2File(int [] x, int [] y){
        //結果をファイルに保存
        //上書きモードです。
        File file2 = new File("output/range-num.txt");
        try (FileWriter fw = new FileWriter(file2)) {
            try (PrintWriter pw = new PrintWriter(new BufferedWriter(fw))) {
                for (int i = 0; i < x.length; i++) {
                    pw.print(x[i] + " ");
                    pw.print(y[i]);
                    pw.println();
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(Sikomode.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
