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
    private final SharedPreferences preferences;
    private static SqlLitle sqlLitle;

    public SendInfo(Context context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        sqlLitle = new SqlLitle(context);
    }

    public void postData(String json) {
        Thread thread = new Thread(() -> {
            HttpURLConnection connection = null;
            try {
                Message message = new Message(json);
                URL url = new URL("https://notify.csctracker.com/message");
                connection = (HttpURLConnection) url.openConnection();
                connection.setDoOutput(true);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Authorization", "Bearer " + preferences.getString(Const.PREF_TOKEN, ""));
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

        String uri;
        if ("message".equals(endpoint)) {
            postData(json);
        } else {
            uri = preferences.getString(Const.PREF_URL, "https://backend.csctracker.com/") + endpoint;

            HttpURLConnection connection = null;
            try {
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
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }
    }
}

