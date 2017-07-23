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
        //差分を計算し、差分のあるTickのリストを取得
        List<Long> tickList = this.calcDiffece();
        //特徴
        SoundFeature feature = new SoundFeature(tickList.size());
        //ミスの直前、以後の値
        SoundFeature pre;
        SoundFeature post;
        //タイミング解像度は４分音符の長さなので、2倍することで２分音符分の長さをみる
        //４分の４拍子のため前後合わせて1小節の長さとなる
        int tickRange = this.score.getResolution()* 2;
        //特徴抽出
        pre = this.extractFeature(tickList, tickRange, 0);
        post = this.extractFeature(tickList, 0, tickRange);
        //前後の変化量を計算
        for(int i = 0; i < pre.range.length;i++){
            feature.range[i] = post.range[i] - pre.range[i];
        }
        for(int i = 0; i < pre.num.length;i++){
            feature.num[i] = post.num[i] - pre.num[i];
        }
        //結果をファイルに保存
        this.save2File(feature.range, feature.num);
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
        long range = recodeResolution / 16; //32分音符
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
     * 特徴を抽出する。<br>
     * 抽出する特徴は、ミス前後の音の数と音域の変化量とする。<br>
     * preRange <= tickList.get(index) <= postRangeの範囲を探索する。<br>
     * @param tickList tickのリスト
     * @param preRange 指定したtickの前の範囲
     * @param postRange 指定したtickの後の範囲
     * @return 
     */
    private SoundFeature extractFeature(List<Long> tickList, 
            int preRange, int postRange){
        //取得したNoteOnを格納する
        byte[] soundScore;
        //tickRangeで指定した時間の間の音の総和
        int[] soundSum = new int[128];
        //指定するtick
        long tick;
        SoundFeature feature = new SoundFeature(tickList.size());
        int minIdx, maxIdx;
        //ミスした箇所のtickのリストから、ミスした箇所の特徴を抽出する
        for (int i = 0; i < tickList.size(); i++) {
            Arrays.fill(soundSum, (byte) 0);
            //指定したtickの前後を見る。
            tick = tickList.get(i);
            for (long range = (tick - preRange);
                    range < (tick + postRange); range++) {
                //ファイルの先頭以前の部分ならばcontinue
                if (range < 0) {
                    continue;
                }
                //NoteOnを取得する
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
            //音域をとる
            minIdx = Integer.MAX_VALUE;
            maxIdx = Integer.MIN_VALUE;
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
            //差分の結果、抽出したtickの前後一小節の音域
            feature.range[i] = (maxIdx - minIdx);
            //音の数
            for (int j = 0; j < soundSum.length; j++) {
                feature.num[i] += soundSum[j];
            }
        }
        return feature;
    }
    
    /**
     * x, y の二次元の情報をファイルに保存します。<br>
     * x, y は空白区切りで保存されます。<br>
     * x, y の配列のサイズは同じでないといけません。<br>
     */
    private void save2File(int[] x, int[] y) {
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
