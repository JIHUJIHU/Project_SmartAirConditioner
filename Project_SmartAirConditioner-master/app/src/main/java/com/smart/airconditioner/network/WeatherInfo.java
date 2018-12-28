package com.smart.airconditioner.network;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.smart.airconditioner.MainActivity;
import com.smart.airconditioner.R;
import com.smart.airconditioner.model.Weather;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;


public class WeatherInfo {

    private Context context;
    private final String API_ID;

    public WeatherInfo(Context context) {
        this.context = context;
        API_ID = context.getString(R.string.weather_id);
    }

    public void getCurrentWeather() {
        WeatherTask task = new WeatherTask();
        task.execute();
    }

    private class WeatherTask extends AsyncTask<String, Void, JSONObject> {


        @Override
        protected JSONObject doInBackground(String... sId) {

            URLConnection urlConn = null;
            BufferedReader bufferedReader = null;
            try {
                URL url = new URL("http://api.openweathermap.org/data/2.5/weather?lat=36.6199520&lon=127.5257840&units=metric&appid=" +
                        API_ID);
                urlConn = url.openConnection();
                bufferedReader = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));

                StringBuffer stringBuffer = new StringBuffer();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuffer.append(line);
                }

                return new JSONObject(stringBuffer.toString());
            } catch (Exception ex) {
                return null;
            } finally {
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        @Override
        protected void onPostExecute(JSONObject result) {
            super.onPostExecute(result);
            processData(result);
        }
    }

    private void processData(JSONObject obj) {
        try {
            JSONArray weatherArr = obj.getJSONArray("weather");
            String weather = weatherArr.getJSONObject(0).getString("id");
            int weatherValue = Integer.parseInt(weather);

            JSONObject mainObj = obj.getJSONObject("main");

            String temp = mainObj.getString("temp");
            float tempValue = Float.parseFloat(temp);
            String humid = mainObj.getString("humidity");
            float humidValue = Float.parseFloat(humid);

            Weather w = new Weather(weatherValue, tempValue, humidValue);

            ((MainActivity) context).notifyWeatherChange(w);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}