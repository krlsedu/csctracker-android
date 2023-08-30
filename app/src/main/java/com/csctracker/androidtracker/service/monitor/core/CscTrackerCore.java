package com.csctracker.androidtracker.service.monitor.core;

import android.content.Context;
import android.util.Log;
import com.csctracker.androidtracker.misc.SendInfo;
import com.csctracker.androidtracker.service.monitor.MonitorNotification;
import com.csctracker.androidtracker.service.monitor.RabbitListener;
import com.csctracker.androidtracker.service.monitor.core.model.ApplicationDetail;
import com.csctracker.androidtracker.service.monitor.core.model.Erro;
import com.csctracker.androidtracker.ui.MainActivity;
import com.fasterxml.jackson.databind.ObjectMapper;

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
    private static final ConcurrentLinkedQueue<Erro> resincErrors = new ConcurrentLinkedQueue<>();
    private static boolean debug = false;

    private static boolean running = false;
    private static ObjectMapper objectMapper = null;
    private static ScheduledFuture<?> scheduledFixture;
    private static ScheduledFuture<?> scheduledFixtureErrors;

    private static SqlLitle sqlLitle;
    private static SendInfo sendInfo;
    private static MonitorNotification monitorNotification;

    private static RabbitListener rabbitListener;

    private CscTrackerCore() {
    }

    public static void init(Context context, MainActivity mainActivity) {
        sqlLitle = new SqlLitle(context);
        sendInfo = new SendInfo(context);
        monitorNotification = new MonitorNotification(context, mainActivity);
        rabbitListener = new RabbitListener();
        rabbitListener.init(monitorNotification);
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
            Erro erro = resincErrors.poll();
            if (erro == null) {
                return;
            }
            sendErro(erro);
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
                sqlLitle.salva(jsonString, "usage-info");
            } catch (Exception ex) {
                Log.w("ex", ex);
            }
            Log.w("ex", e);
        }
    }

    private static void send(String jsonString) {
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
        postData(jsonString);
    }

    private static void sendErro(Erro erro) {
        postData(erro.getJson(), erro.getEndpoint());
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
        postData(json, "usage-info");
    }

    private static void postData(String json, String endpoint) {
        try {
            sendInfo.send(json, endpoint);
        } catch (Exception e) {
            sqlLitle.salva(json, endpoint);
        }
    }
}
