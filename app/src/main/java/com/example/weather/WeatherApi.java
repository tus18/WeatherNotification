package com.example.weather;
import retrofit2.Call;
import retrofit2.http.GET;

public interface WeatherApi {
    @GET("v1/forecast?latitude=34.0667&longitude=131.0333&hourly=weathercode&timezone=Asia%2FTokyo&forecast_days=1")
    Call<WeatherResponse> getWeather();
}