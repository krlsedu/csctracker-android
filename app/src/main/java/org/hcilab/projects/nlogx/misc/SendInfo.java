package org.hcilab.projects.nlogx.misc;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class SendInfo {
    public static void postData(String json) {
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

                e.printStackTrace();
                //return false;
                System.out.println(e.getMessage());

            } finally {

                if (connection != null) {
                    connection.disconnect();
                }
            }
        });
        thread.start();
    }
}
