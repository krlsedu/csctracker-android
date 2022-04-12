package com.csctracker.androidtracker.service.monitor.core;

import android.content.Context;
import android.util.Log;
import com.csctracker.androidtracker.service.monitor.core.model.ApplicationDetail;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

public class CscTrackerCore {
    public static final int QUEUE_TIMEOUT_SECONDS = 10;
    public static final int QUEUE_ERRORS_TIMEOUT_SECONDS = 90;
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final ScheduledExecutorService schedulerErrors = Executors.newScheduledThreadPool(1);
    private static final ConcurrentLinkedQueue<ApplicationDetail> heartbeatsQueue = new ConcurrentLinkedQueue<>();
    private static final ConcurrentLinkedQueue<String> resincErrors = new ConcurrentLinkedQueue<>();
    private static boolean debug = false;
    private static ObjectMapper objectMapper = null;
    private static ScheduledFuture<?> scheduledFixture;
    private static ScheduledFuture<?> scheduledFixtureErrors;

    private static SqlLitle sqlLitle;

    private CscTrackerCore() {
    }

    public static void init(Context context) {
        sqlLitle = new SqlLitle(context);
        setupQueueProcessor();
        setupQueueProcessorErrors();
    }

    private static void setupQueueProcessor() {
        final Runnable handler = CscTrackerCore::processHeartbeatQueue;
        final Runnable handlerErros = CscTrackerCore::resincErrors;
        long delay = QUEUE_TIMEOUT_SECONDS;
        scheduledFixture = scheduler.scheduleAtFixedRate(handler, delay, delay, java.util.concurrent.TimeUnit.SECONDS);
        scheduledFixtureErrors = schedulerErrors.scheduleAtFixedRate(handlerErros, QUEUE_ERRORS_TIMEOUT_SECONDS, QUEUE_ERRORS_TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
    }

    private static void setupQueueProcessorErrors() {
        final Runnable handlerErros = CscTrackerCore::resincErrors;
        scheduledFixtureErrors = schedulerErrors.scheduleAtFixedRate(handlerErros, QUEUE_ERRORS_TIMEOUT_SECONDS, QUEUE_ERRORS_TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
    }

    public static void stopQueue() {
        scheduledFixture.cancel(true);
        scheduledFixtureErrors.cancel(true);
    }

    private static void resincErrors() {

        try {
            sqlLitle.getErrors(resincErrors);
        } catch (SQLException e) {
            Log.w("ex", e);
        }

        while (true) {
            String jsonString = resincErrors.poll();
            if (jsonString == null) {
                return;
            }
            send(jsonString);
        }
    }

    private static void processHeartbeatQueue() {

        List<ApplicationDetail> timeTrackerHeartbets = new ArrayList<>();
        while (true) {
            ApplicationDetail h = heartbeatsQueue.poll();
            if (h == null)
                break;

            timeTrackerHeartbets.add(h);
        }

        if (!timeTrackerHeartbets.isEmpty()) {
            send(timeTrackerHeartbets);
        }
    }

    public static void send(List<ApplicationDetail> heartbeats) {
        String jsonString = null;
        try {
            jsonString = getObjectMapper().writeValueAsString(heartbeats);
            send(jsonString);
        } catch (Exception e) {
            try {
                sqlLitle.salva(jsonString);
            } catch (Exception ex) {
                Log.w("ex", ex);
            }
            Log.w("ex", e);
        }
    }

    private static void send(String jsonString) {
        try {
            System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
            postData(jsonString);
        } catch (Exception e) {
            try {
                sqlLitle.salva(jsonString);
            } catch (Exception ex) {
                Log.w("ex", ex);
            }
            Log.w("ex", e);
        }
    }

    public static void appendApplicationDetail(final ApplicationDetail applicationDetail) {
        heartbeatsQueue.add(applicationDetail);
    }

    public static void appendApplicationDetail(final List<ApplicationDetail> applicationDetail) {
        heartbeatsQueue.addAll(applicationDetail);
    }

    public static ObjectMapper getObjectMapper() {
        if (objectMapper == null) {
            objectMapper = new ObjectMapper();
        }
        return objectMapper;
    }

    public static boolean isDebug() {
        return debug;
    }

    private static void postData(String json) {
        HttpURLConnection connection = null;
        try {

            URL url = new URL("https://tracker.csctracker.com/usage-info");
            connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("POST"); // hear you are telling that it is a POST request, which can be changed into "PUT", "GET", "DELETE" etc.
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8"); // here you are setting the `Content-Type` for the data you are sending which is `application/json`
            connection.connect();

            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8));
            bw.write(json); // I dunno how to write this string..
            bw.flush();
            bw.close();

            int response = connection.getResponseCode();
            if (response != 201) {
                try {
                    sqlLitle.salva(json);
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
