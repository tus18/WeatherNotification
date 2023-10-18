package com.example.weather;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private static final String BASE_URL = "https://api.open-meteo.com/";
    private TextView weatherCodeTextView;
    private Button fetchWeatherButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        weatherCodeTextView = findViewById(R.id.weatherCodeTextView);
        fetchWeatherButton = findViewById(R.id.fetchWeatherButton);
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

        // 時間の設定
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tokyo"));
        calendar.set(Calendar.HOUR_OF_DAY, 2);//時
        calendar.set(Calendar.MINUTE, 40);//分

        long timeUntilNextJob = calendar.getTimeInMillis() - System.currentTimeMillis();
        if (timeUntilNextJob < 0) {
            // 既に9時を過ぎている場合、次の日に設定
            timeUntilNextJob += TimeUnit.DAYS.toMillis(1);
            //timeUntilNextJob = 0;
        }

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(WeatherWorker.class)
                .setInitialDelay(timeUntilNextJob, TimeUnit.MILLISECONDS)
                .build();

        WorkManager.getInstance(this).enqueue(workRequest);

    }

    private void fetchWeather() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        WeatherApi api = retrofit.create(WeatherApi.class);
        Call<WeatherResponse> call = api.getWeather();

        call.enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                if (response.isSuccessful()) {
                    WeatherResponse weather = response.body();

                    // weathercodeのリストを取得
                    List<Integer> rainAmount = weather.getHourly().getRain();

                    // TextViewにデータをセット
                    weatherCodeTextView.setText("雨量: " + rainAmount.toString());
                    sendNotification("Weather codes: " + rainAmount.toString());
                } else {
                    Toast.makeText(MainActivity.this, "データ取得失敗", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {
                Toast.makeText(MainActivity.this, "通信エラー", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void sendNotification(String weatherInfo) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "weatherChannel")
                .setSmallIcon(R.drawable.ic_launcher_foreground)  // アイコンの設定
                .setContentTitle("天気情報")  // タイトル
                .setContentText(weatherInfo)  // 本文
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        notificationManager.notify(1001, builder.build());
    }

}
