package com.example.weather;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;
public class MainActivity extends AppCompatActivity {

    private TextView weatherCodeTextView;
    private Button loadButton;

    private int[][] test_arr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        weatherCodeTextView = findViewById(R.id.weatherCodeTextView);
        loadButton = findViewById(R.id.loadButton);
        TimePicker timePicker = findViewById(R.id.timePicker);
        Button saveButton = findViewById(R.id.saveButton);
        Spinner daySpinner = findViewById(R.id.daySpinner);
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.days_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        daySpinner.setAdapter(adapter);
        Log.d("debug","start");
//        ファイルの書き込み失敗時の初期化用
//        test_arr = new int[][]{{12, 0}, {11, 0}, {10, 0}, {9, 0}, {8, 0}, {7, 0}, {6, 0}};
//        saveArray(test_arr);

        try {
            // ファイルから配列を読み込む
            test_arr = load();
            Log.d("debug",Arrays.deepToString(test_arr));
        } catch (IOException e) {
            // ファイル読み込みエラーの処理
            Log.e("MainActivity", "ファイル読み込みに失敗", e);
            Toast.makeText(this, "ファイル読み込みに失敗しました", Toast.LENGTH_SHORT).show();
            // エラーが発生した場合のデフォルト値を設定
            test_arr = new int[][]{{12, 0}, {11, 0}, {10, 0}, {9, 0}, {8, 0}, {7, 0}, {6, 0}};
        }


        //曜日の取得
        Calendar cal = Calendar.getInstance();
        int week = cal.get(Calendar.DAY_OF_WEEK);
        Log.d("debug","week:"+Integer.toString(week));

        int h,m;
        int i;
        switch(week){
            case 1:i=0;break;//日曜日
            case 2:i=1;break;//月曜日
            case 3:i=2;break;//火曜日
            case 4:i=3;break;//水曜日
            case 5:i=4;break;//木曜日
            case 6:i=5;break;//金曜日
            default:i=6;     //土曜日
        }
        h = test_arr[i][0];
        m = test_arr[i][1];
        scheduleNotification(h,m);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("weatherChannel", "Weather Notification", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        loadButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                try {
                    test_arr=load();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });

// 曜日と時間を保存する
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int selectedDay = daySpinner.getSelectedItemPosition();
                int hour = timePicker.getCurrentHour();
                int minute = timePicker.getCurrentMinute();
                Log.d("debug","daySpinner:"+selectedDay);

                // test_arr を更新
                test_arr[selectedDay][0] = hour;
                test_arr[selectedDay][1] = minute;

                // 更新された配列を保存
                saveArray(test_arr);
                if(selectedDay+1 == week){
                    scheduleNotification(hour,minute);
                }
            }
        });

    }

    // 配列を保存するメソッド
    private void saveArray(int[][] arr) {
        String fileName = "myFile.txt";
        String inputText = Arrays.deepToString(arr);

        try {
            FileOutputStream outStream = openFileOutput(fileName, MODE_PRIVATE);
            OutputStreamWriter writer = new OutputStreamWriter(outStream);
            writer.write(inputText);
            writer.flush();
            writer.close();
            Toast.makeText(getBaseContext(), "配列が保存されました", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(getBaseContext(), "保存に失敗しました", Toast.LENGTH_SHORT).show();
        }
    }

    //通知時間の更新
    private void scheduleNotification(int hour, int minute){
        try {
            // 時間の設定
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);

            long timeUntilNextJob = calendar.getTimeInMillis() - System.currentTimeMillis();
            if (timeUntilNextJob < 0) {
                // 既に設定時刻を過ぎている場合、次の日に設定

                scheduleNextDayNotification();
                return;
            }

            OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(WeatherWorker.class)
                    .setInitialDelay(timeUntilNextJob, TimeUnit.MILLISECONDS)
                    .build();

            WorkManager.getInstance(this).enqueue(workRequest);
            Log.d("debug", "今日は" + hour + "時" + minute + "分に通知");
        }catch(IOException e){
            Log.e("WeatherWorker", "通知スケジュール設定時にエラー発生", e);
        }
    }
    private void scheduleNextDayNotification() throws IOException {
        Calendar calendar = Calendar.getInstance();
        int currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK); // 現在の曜日を取得 日曜が１
        int nextDayOfWeek = (currentDayOfWeek % 7) ; // 次の曜日を計算 (日曜日は0, 月曜日は1, ..., 土曜日は6)

        int[][] scheduleArray = load(); // スケジュール配列を読み込む
        int hour = scheduleArray[nextDayOfWeek][0];
        int minute = scheduleArray[nextDayOfWeek][1];

        // 指定された時間に設定
        calendar.add(Calendar.DAY_OF_YEAR, 1); // 翌日に設定
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        long timeUntilNextJob = calendar.getTimeInMillis() - System.currentTimeMillis();

        // WorkRequestを作成し、WorkManagerでスケジュール
        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(WeatherWorker.class)
                .setInitialDelay(timeUntilNextJob, TimeUnit.MILLISECONDS)
                .build();
        WorkManager.getInstance(getApplicationContext()).enqueue(workRequest);

        Log.d("debug", "次の通知は " + calendar.getTime() + " に設定されました");
    }




    private int[][] load() throws IOException {
        FileInputStream fileInputStream = openFileInput("myFile.txt");
        BufferedReader reader = new BufferedReader(new InputStreamReader(fileInputStream, "UTF-8"));

        String lineBuffer;
        ArrayList<String> text = new ArrayList<>();
        while (true){
            lineBuffer = reader.readLine();
            if (lineBuffer != null){
                text.add(lineBuffer);
            }
            else {
                break;
            }
        }
        reader.close();

        //文字列を先頭からループ
        //int型配列に変換するコードにバグあり
        List<Integer> numbers = new ArrayList<>();
        for (String str : text) {
            StringBuilder numberBuffer = new StringBuilder();
            for (char ch : str.toCharArray()) {
                if (ch != '[' && ch != ']' && ch != ','&& ch != ' ') {
                    numberBuffer.append(ch);
                    Log.d("debug",""+ch);
                } else if (numberBuffer.length() > 0) {
                    numbers.add(Integer.parseInt(numberBuffer.toString()));
                    numberBuffer = new StringBuilder(); // バッファをリセット
                }
            }
            // 最後の数字を追加（もしバッファに残っていれば）
            if (numberBuffer.length() > 0) {
                numbers.add(Integer.parseInt(numberBuffer.toString()));
            }
        }
        Log.d("debug","shuuryou");
        int k =0;
        int[][] arr;
        arr = new int[7][2];
        for (int i=0;i<numbers.size();i++){
            if(i>1 && i%2 == 0){
                k++;
            }
            arr[k][i%2] = numbers.get(i);
        }

        weatherCodeTextView.setText("kousinn" + Arrays.deepToString(arr));
        return arr;
    }

}