package com.example.weather;

import android.content.Context;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.util.List;

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
        call.enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(Call<WeatherResponse> call, Response<WeatherResponse> response) {
                if (response.isSuccessful()) {
                    // 成功時、通知を送る
                    WeatherResponse weather = response.body();
                    List<Integer> rainAmount = weather.getHourly().getRain();
                    sendNotification("雨量: " + rainAmount.toString());
                }
            }

            @Override
            public void onFailure(Call<WeatherResponse> call, Throwable t) {
                // 失敗時、何もしない
            }
        });
        return Result.success();
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
}
