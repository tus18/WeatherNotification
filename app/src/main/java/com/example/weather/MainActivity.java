package com.example.weather;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.TimeUnit;
public class MainActivity extends AppCompatActivity {

    private static final String BASE_URL = "https://api.open-meteo.com/";
    private TextView weatherCodeTextView;
    private Button fetchWeatherButton;
    private Button loadButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //曜日の取得
        Calendar cal = Calendar.getInstance();
        int week = cal.get(Calendar.DAY_OF_WEEK);
        Log.d("debug",Integer.toString(week));

        int h,m;
        switch(week){
            case 0:h=12;m=0;
            case 4:h=11;m=0;
        }

        weatherCodeTextView = findViewById(R.id.weatherCodeTextView);
        fetchWeatherButton = findViewById(R.id.fetchWeatherButton);
        loadButton = findViewById(R.id.loadButton);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("weatherChannel", "Weather Notification", NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }

        fetchWeatherButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                fetchWeather();
            }
        });
        loadButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                try {
                    load();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        // 時間の設定
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 20);//時
        calendar.set(Calendar.MINUTE, 0);//分

        long timeUntilNextJob = calendar.getTimeInMillis() - System.currentTimeMillis();
        if (timeUntilNextJob < 0) {
            // 既に9時を過ぎている場合、次の日に設定
            //timeUntilNextJob += TimeUnit.DAYS.toMillis(1);
            timeUntilNextJob = 0;
        }

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(WeatherWorker.class)
                .setInitialDelay(timeUntilNextJob, TimeUnit.MILLISECONDS)
                .build();

        WorkManager.getInstance(this).enqueue(workRequest);

    }

    private void fetchWeather() {
        String message = "";
        String fileName = "myFile.txt";
        String inputText = "12 30";

        try {
            FileOutputStream outStream = openFileOutput(fileName, MODE_PRIVATE);
            OutputStreamWriter writer = new OutputStreamWriter(outStream);
            writer.write(inputText);
            writer.flush();
            writer.close();
            Log.d("debug","成功");

            message = "File saved.";
        } catch (FileNotFoundException e) {
            Log.d("debug","ファイルなし");
            e.printStackTrace();
        } catch (IOException e) {
            message = e.getMessage();
            e.printStackTrace();
        }

        Toast.makeText(getBaseContext(), message, Toast.LENGTH_LONG).show();
    }

    private void load() throws IOException {
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
        weatherCodeTextView.setText("kousinn " + text);
    }



}
