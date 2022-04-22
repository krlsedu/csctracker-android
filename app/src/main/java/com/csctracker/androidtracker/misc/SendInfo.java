package com.csctracker.androidtracker.misc;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import com.csctracker.androidtracker.service.monitor.core.SqlLitle;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class SendInfo {


    private Context context;
    private SharedPreferences preferences;
    private static SqlLitle sqlLitle;

    public SendInfo(Context context) {
        this.context = context;
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        sqlLitle = new SqlLitle(context);
    }

    public void postData(String json) {
        Thread thread = new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                Message message = new Message(json);
                URL url = new URL("https://notify.csctracker.com/info");
                connection = (HttpURLConnection) url.openConnection();
                connection.setDoOutput(true);
                connection.setRequestMethod("POST"); // hear you are telling that it is a POST request, which can be changed into "PUT", "GET", "DELETE" etc.
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8"); // here you are setting the `Content-Type` for the data you are sending which is `application/json`
                connection.connect();

                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8));
                bw.write(new ObjectMapper().writeValueAsString(message)); // I dunno how to write this string..
                bw.flush();
                bw.close();

                int response = connection.getResponseCode();
                if (response != 201) {

                }
            } catch (Exception e) {
                try {
                    sqlLitle.salva(json, "info");
                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
                }
            }
        });
        thread.start();
    }

    public void send(String json, String endpoint) {
        if ("info".equals(endpoint)) {
            postData(json);
        } else {

            HttpURLConnection connection = null;
            try {

                String uri = preferences.getString(Const.PREF_URL, "https://backend.csctracker.com/") + endpoint;
                URL url = new URL(uri);
                connection = (HttpURLConnection) url.openConnection();
                connection.setDoOutput(true);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Authorization", "Bearer " + preferences.getString(Const.PREF_TOKEN, ""));
                connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                connection.connect();

                BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8));
                bw.write(json);
                bw.flush();
                bw.close();

                int response = connection.getResponseCode();
                if (response != 201) {
                    try {
                        sqlLitle.salva(json, endpoint);
                    } catch (Exception e) {
                        //
                    }
                }


            } catch (Exception e) {

                e.printStackTrace();
                //return false;

            } finally {

                if (connection != null) {
                    connection.disconnect();
                }
            }
        }
    }
}
