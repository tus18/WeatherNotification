package com.example.weather;
import java.util.List;

public class WeatherResponse {
    private Hourly hourly;

    public Hourly getHourly() {
        return hourly;
    }

    public void setHourly(Hourly hourly) {
        this.hourly = hourly;
    }

    public static class Hourly {
        private List<Integer> weathercode;

        public List<Integer> getWeathercode() {
            return weathercode;
        }

        public void setWeathercode(List<Integer> weathercode) {
            this.weathercode = weathercode;
        }
    }
}