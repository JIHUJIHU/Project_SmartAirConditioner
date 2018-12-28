package com.smart.airconditioner.model;

public class Weather {


    int weaatherId;
    float temperature;
    float humid;

    public int getWeaatherId() {
        return weaatherId;
    }

    public float getTemperature() {
        return temperature;
    }

    public float getHumid() {
        return humid;
    }


    public Weather(int weaatherId, float temperature, float humid) {
        this.weaatherId = weaatherId;
        this.temperature = temperature;
        this.humid = humid;
    }


}
