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
 *
 * @author tanakataishiro
 */
public class Sikomode {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here

        Midi train = new Midi();
        Midi query = new Midi();
        String filePathTrain = "midi/USERSONG013.MID";
        String filePathQuery = "midi/yakusoku.mid";
        train.readMidiFile(filePathTrain);
        query.readMidiFile(filePathQuery);

        long tick;
        byte[] soundTrain;
        byte[] soundQuery;
        List<Long> tickList = new ArrayList<>();
        boolean isDifferent;
        while (true) {
            tick = query.getNoteOnTick();
            if (tick < 0) {
                break;
            }
            soundTrain = train.getNoteOn(tick);
            soundQuery = query.getNoteOn(tick);
            //バイト配列の差分処理
            //まずは二つの配列が違うということが分ればok
            isDifferent = !Arrays.equals(soundTrain, soundQuery);
            //もし異なれば、tickを記録。
            if (isDifferent) {
                tickList.add(tick);
            }
        }
        //結果をファイルに保存
        //上書きモードです。
        File file = new File("diff.txt");
        try (FileWriter fw = new FileWriter(file)) {
            try (PrintWriter pw = new PrintWriter(new BufferedWriter(fw))) {
                for (int i = 0; i < tickList.size(); i++) {
                    pw.print(tickList.get(i));
                    pw.println();
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(Sikomode.class.getName()).log(Level.SEVERE, null, ex);
        }
        //差分の結果を使ってモデリング
        //まずは音域のみを見る。
        //getSoundは、引数のtickの瞬間に鳴っている音を返す。
        //こいつをNoteOnのみ返すようにすれば、音域だけでなく音の数もとることが出来る。
        //なぜNoteOnか、というと、ダンパーペダルがあるため、音の終わりを捉えるのは難しいからである。
        int[] soundRange = new int[tickList.size()];
        long tickRange = 60; //指定したtickの前後(+-30)を見る。
        //tickRangeで指定した時間の間の音を全部足したもの
        int[] soundSum = new int[128];
        for (int i = 0; i < tickList.size(); i++) {
            Arrays.fill(soundSum, (byte)0); //0埋めで初期化
            long t = tickList.get(i);
            //指定したtickの前後を見る。
            //値の範囲の60は１小節の半分。(コレはmidiのタイミング解像度の半分)
            System.out.println("t : " + t);
            for (long range = (t - tickRange); range < (t + tickRange); range++) {
                if(range < 0) { //ファイルの先頭以前の部分ならばcontinue
                    continue;
                }
                soundQuery = query.getNoteOn(range);
                //ファイル末尾ならば終了
                if (soundQuery[0] == -1) {
                    break;
                }
                //音の総和をとる
                for (int j = 0; j < soundQuery.length; j++) {
                    soundSum[j] += soundQuery[j];
                }
            }
            int minIdx = Integer.MAX_VALUE;
            int maxIdx = Integer.MIN_VALUE;
            
            for (int j = 0; j < soundSum.length; j++) {
                if(soundSum[j] != 0){
                    if(minIdx > j){
                        minIdx = j;
                    }
                    if(maxIdx < j){
                        maxIdx = j;
                    }
                }
            }
            
            System.out.println(Arrays.toString(soundSum));
            //差分の結果、抽出したtickの前後一小節の音域
            soundRange[i] = (maxIdx - minIdx);
        }

        System.out.println("range");
        for(int i = 0; i < soundRange.length; i++){
            System.out.println(i + " : " + soundRange[i]);
        }
            
        //ここで出てきた音域（と音の数）の配列がユーザーのモデル。
        //次に、モデルの解釈を行う。
        //ひとまず、最も簡単な方法で行う。閾値を設定し、閾値ごとにメッセージを用意する、
        //音の数と音域で、閾値の組み合わせの数だけメッセージが生成されることになる。。
            
        
        //その前に、↑の部分で、
        //ファイル先頭以前を見ているときの処理を追加せねば。。。
        //あと録音しようか。
    }
}
