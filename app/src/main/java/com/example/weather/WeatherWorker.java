package com.example.weather;

import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class WeatherWorker extends Worker {
    private static final String BASE_URL = "https://api.open-meteo.com/";

    public WeatherWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        // Retrofitインスタンス生成
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        // API呼び出し
        WeatherApi api = retrofit.create(WeatherApi.class);
        Call<WeatherResponse> call = api.getWeather();
        Log.d("debug","API呼び出し開始");
        call.enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                if (response.isSuccessful()) {
                    Log.d("debug","API呼び出し成功");
                    // 成功時、通知を送る
                    WeatherResponse weather = response.body();
                    List<Double> rainAmount = weather.getHourly().getRain();
                    String notificationMessage = createNotificationMessage(rainAmount);
                    sendNotification(notificationMessage);
                }
            }

            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {
                // 失敗時、何もしない
                Log.d("debug","API呼び出し失敗");
            }
        });
        try {//次回の通知設定
            setDailyNotificationSchedule();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return Result.success();
    }
    private String createNotificationMessage(List<Double> rainData) {
        // 雨が降るかどうかをチェックする
        boolean willRain = false;
        for (Double rainAmount : rainData) {
            if (rainAmount > 0) {
                willRain = true;
                break;
            }
        }

        // 雨が降る場合は「雨具必要」と表示し、データ配列を表示
        if (willRain) {
            return "雨具必要\n雨量データ: " + rainData.toString();
        } else {
            // 雨が降らない場合は「雨具必要なし」と表示
            return "雨具必要なし";
        }
    }

    // 通知送信メソッド
    private void sendNotification(String weatherInfo) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), "weatherChannel")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("天気情報")
                .setContentText(weatherInfo)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());
        if (ActivityCompat.checkSelfPermission(this.getApplicationContext(), android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        notificationManager.notify(1, builder.build());
    }

    private void setDailyNotificationSchedule() throws IOException {
        int[][] scheduleArray = load();
        Calendar cal = Calendar.getInstance();
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1;
        int hour = scheduleArray[dayOfWeek][0];
        int minute = scheduleArray[dayOfWeek][1];
        scheduleNotification(hour, minute);
    }
    public int[][] load() throws IOException {
        Context context = getApplicationContext();
        FileInputStream fileInputStream = context.openFileInput("myFile.txt");
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

        return arr;
    }

    private void scheduleNotification(int hour, int minute) {
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

            WorkManager.getInstance(this.getApplicationContext()).enqueue(workRequest);
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
}
