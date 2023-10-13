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
        private List<Integer> rain;

        public List<Integer> getRain() {
            return rain;
        }

        public void setRain(List<Integer> rain) {
            this.rain = rain;
        }
    }
}