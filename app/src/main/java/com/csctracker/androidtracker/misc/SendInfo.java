package com.csctracker.androidtracker.misc;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import com.csctracker.androidtracker.service.monitor.core.SqlLitle;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class SendInfo {
    private final SharedPreferences preferences;
    private static SqlLitle sqlLitle;
    private Date date = new Date(new Date().getTime() - 5 * 60 * 1000);

    private Context context;

    public SendInfo(Context context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        sqlLitle = new SqlLitle(context);
        this.context = context;
    }

    public void send(String json, String endpoint) {

        Thread thread = new Thread(() -> {
            String jsonSend = json;
            String uri;
            if ("message".equals(endpoint)) {
                Message message = new Message(json);
                try {
                    jsonSend = new ObjectMapper().writeValueAsString(message);
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
                uri = preferences.getString(Const.PREF_URL, "https://bff.csctracker.com/") + "notify-sync/" + endpoint;
            } else {
                uri = preferences.getString(Const.PREF_URL, "https://bff.csctracker.com/") + "backend/" + endpoint;
            }
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
                bw.write(jsonSend);
                bw.flush();
                bw.close();

                int response = connection.getResponseCode();
                if (response < 200 || response > 299) {
                    try {
                        sqlLitle.salva(jsonSend, endpoint);
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
        });
        thread.start();
    }
}

