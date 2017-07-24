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
    private SoundFeature feature;
    private String advice;

    public Modeling() {
    }

    /**
     * モデルを計算するMidiファイルのオブジェクトをセットする。<br>
     *
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
        this.feature = new SoundFeature(tickList.size());
        //ミスの直前、以後の値
        SoundFeature pre;
        SoundFeature post;
        //タイミング解像度は４分音符の長さなので、2倍することで２分音符分の長さをみる
        //４分の４拍子のため前後合わせて1小節の長さとなる
        int tickRange = this.score.getResolution() * 2;
        //特徴抽出
        pre = this.extractFeature(tickList, tickRange, 0);
        post = this.extractFeature(tickList, 0, tickRange);
        //前後の変化量を計算
        for (int i = 0; i < pre.range.length; i++) {
            feature.range[i] = post.range[i] - pre.range[i];
        }
        for (int i = 0; i < pre.num.length; i++) {
            feature.num[i] = post.num[i] - pre.num[i];
        }
        feature.tick = pre.tick;
        //アドバイスを生成する
        this.createAdvice();
        //結果をファイルに保存
        this.saveToCsv(feature.range, feature.num);
    }

    /**
     * 作成したモデルを元に生成したアドバイスを取得する。
     *
     * @return
     */
    public String getAdvice() {
        return advice;
    }

    /**
     * モデルを元にアドバイスを生成する。
     */
    private void createAdvice() {
        //閾値を絶対値で一つ定める
        //そうすると一つの軸に対して、三つの領域が出来る。
        //二次元なので9個の領域->9このメッセージ　で十分でしょう

        //１小節の音が全部４分音符で８個
        //８個増えたときを閾値とする。
        int NUM_THRESHOLD = 8;
        //1オクターブで11個
        //2オクターブで23個（あとは12ずつ足していくだけ。
        //2オクターブを閾値とする。
        int RANGE_THRESHOLD = 23;
        //各領域のプロット数を格納する
        int[] range = {0, 0, 0}; //変化が{なし、正、負}
        int[] num = {0, 0, 0}; //変化{なし、正、負}
        //各領域のプロット数をカウント
        for (int i = 0; i < this.feature.length(); i++) {
            if (Math.abs(this.feature.num[i]) < NUM_THRESHOLD) { //速さの変化なし
                num[0]++;
            } else{ //変化あり
                if (this.feature.num[i] > 0) { //正の変化量
                    num[1]++;
                } else { //負の変化量
                    num[2]++;
                }
            }
            if (Math.abs(this.feature.range[i]) < RANGE_THRESHOLD) { //音域の変化なし
                range[0]++;
            } else{ //変化あり
                if (this.feature.range[i] > 0) { //正の変化量
                    range[1]++;
                } else { //負の変化量
                    range[2]++;
                }
            }
        }
        System.out.println("各領域のプロット数");
        System.out.println("range : " + Arrays.toString(range));
        System.out.println("num : " + Arrays.toString(num));
        String n = ""; //速さ
        String r = ""; //広さ
        int maxVal = num[0];
        int maxIndex = 0;
        for (int i = 1; i < num.length; i++) {
            if (num[i] > maxVal) {
                maxVal = num[i];
                maxIndex = i;
            }
        }
        switch (maxIndex) {
            case 0:
                n = "速さの変化がない時で、";
                break;
            case 1:
                n = "曲が速くなる時で、";
                break;
            case 2:
                n = "曲が遅くなる時で";
                break;
            default:
        }
        maxVal = range[0];
        maxIndex = 0;
        for (int i = 1; i < range.length; i++) {
            if (range[i] > maxVal) {
                maxVal = range[i];
                maxIndex = i;
            }
        }
        switch (maxIndex) {
            case 0:
                r = "音域の変化が小さい";
                break;
            case 1:
                r = "音域が広くなる";
                break;
            case 2:
                r = "音域が狭くなる";
                break;
            default:
        }
        this.advice = "あなたは、" + n + r + "場合が苦手なようです。";
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
        long range = recodeResolution / 8; //32分音符
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
     * preRange <= tickList.get(index) <= postRangeの範囲を探索する。<br> @param tickLi
     *
     * st tickのリスト
     *
     * @param preRange 指定したtickの前の範囲
     * @param postRange 指定したtickの後の範囲
     * @return
     */
    private SoundFeature extractFeature(List<Long> tickList,
            int preRange, int postRange) {
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
            //tickを保存
            feature.tick[i] = tick;
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
     * x, y の二次元の情報をcsvファイルに保存します。<br>
     * x, y は空白区切りで保存されます。<br>
     * x, y の配列のサイズは同じでないといけません。<br>
     */
    private void saveToCsv(int[] x, int[] y) {
        //結果をファイルに保存
        //上書きモードです。
        File file2 = new File("output/range-num.csv");
        try (FileWriter fw = new FileWriter(file2)) {
            try (PrintWriter pw = new PrintWriter(new BufferedWriter(fw))) {
                for (int i = 0; i < x.length; i++) {
                    pw.print(x[i] + " ,");
                    pw.print(y[i]);
                    pw.println();
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(Sikomode.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
                }
