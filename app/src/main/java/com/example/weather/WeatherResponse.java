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
        private List<Double> rain;

        public List<Double> getRain() {
            return rain;
        }

        public void setRain(List<Double> rain) {
            this.rain = rain;
        }
    }
}