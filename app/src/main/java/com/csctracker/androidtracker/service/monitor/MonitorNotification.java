package com.csctracker.androidtracker.service.monitor;

import android.content.Context;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import com.csctracker.androidtracker.misc.Const;
import com.csctracker.androidtracker.ui.MainActivity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class MonitorNotification {
    private final SharedPreferences preferences;
    private final MainActivity mainActivity;
    private Date date = new Date(new Date().getTime() - 5 * 60 * 1000);

    public MonitorNotification(Context context, MainActivity mainActivity) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.mainActivity = mainActivity;
    }

    public void start() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS 00:00");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        Thread thread = new Thread(() -> {
            do {
                HttpURLConnection connection = null;
                try {
                    Thread.sleep(5000);
                    String spec = "https://gateway.csctracker.com/notify-sync/last-messages-date?date=" + sdf.format(date);
                    URL url = new URL(spec);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setRequestProperty("Authorization", "Bearer " + preferences.getString(Const.PREF_TOKEN, ""));
                    connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

                    int response = connection.getResponseCode();
                    if (response >= 200 && response <= 299) {
                        date = new Date();
                        BufferedReader in = new BufferedReader(new InputStreamReader(
                                connection.getInputStream()));
                        StringBuffer rb = new StringBuffer();
                        String inputLine;

                        while ((inputLine = in.readLine()) != null) {
                            rb.append(inputLine);
                        }
                        in.close();
                        List<OutputMessage> outputMessages = new ObjectMapper().readValue(rb.toString(), new ObjectMapper().getTypeFactory().constructCollectionType(List.class, OutputMessage.class));

                        for (OutputMessage outputMessage : outputMessages) {
                            if ("VD-DUQFULL0312".equals(outputMessage.getMachine()) || outputMessage.isForce()) {
                                String title = "Notification incoming from " + outputMessage.getApp();
                                String text = outputMessage.getFrom() + ": " + outputMessage.getText() + " (" + outputMessage.getTime() + ")";
                                mainActivity.sendNotification(title, text);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            } while (true);
        });
        thread.start();
    }

    public void notify(String text) {

        OutputMessage outputMessage = null;
        try {
            outputMessage = new ObjectMapper().readValue(text, OutputMessage.class);
            if ("VD-DUQFULL0312".equals(outputMessage.getMachine()) || outputMessage.isForce()) {
                String title = "Notification incoming from " + outputMessage.getApp();
                String msg = outputMessage.getFrom() + ": " + outputMessage.getText() + " (" + outputMessage.getTime() + ")";
                mainActivity.sendNotification(title, msg);
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }
}
