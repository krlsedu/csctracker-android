package com.csctracker.androidtracker.service.monitor;

import android.util.Log;
import com.csctracker.androidtracker.service.monitor.core.CscTrackerCore;
import com.csctracker.androidtracker.service.monitor.core.model.ApplicationDetail;
import com.csctracker.androidtracker.service.monitor.core.SqlLitle;
import com.csctracker.androidtracker.ui.MainActivity;

import java.util.List;

public class Core {

    private static final int WAIT_TIME = 20000;
    private static boolean ativo = false;

    private final MainActivity mainActivity;

    private final Monitor monitor;

    private final SqlLitle sqlLitle;

    public Core(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
        monitor = new Monitor(mainActivity.getApplicationContext());
        sqlLitle = new SqlLitle(mainActivity.getApplicationContext());
    }

    public static boolean isAtivo() {
        return ativo;
    }

    public static void setAtivo(boolean ativo) {
        Core.ativo = ativo;
    }

    public static void alternStatus() {
        setAtivo(!Core.isAtivo());
    }

    public static void ativate() {
        setAtivo(true);
    }

    public static void desativate() {
        setAtivo(false);
    }

    public static void stop() {
        desativate();
        CscTrackerCore.stopQueue();
    }

    public void start() {
        if (!isAtivo()) {
            ativate();
            CscTrackerCore.init(mainActivity.getApplicationContext());
            new Thread(this::tracker).start();
        }
    }

    private void tracker() {
        if (CscTrackerCore.isDebug()) {
            Log.i("Initiated", "Initiated");
        }
        do {
            try {
                Thread.sleep(WAIT_TIME);
                Long ini = sqlLitle.getLastSync();
                long fim = System.currentTimeMillis();
                if (ini == null || ini == 0) {
                    ini = fim - (1000 * 60 * 60 * 4);
                }
                List<ApplicationDetail> applicationDetailList = monitor.monitora(ini, fim);

                if (applicationDetailList != null && !applicationDetailList.isEmpty()) {
                    CscTrackerCore.appendApplicationDetail(applicationDetailList);
                }
            } catch (Exception e) {
                Thread.currentThread().interrupt();
                error(e);
                break;
            }
        } while (isAtivo());
        if (CscTrackerCore.isDebug() && !isAtivo()) {
            Log.i("Stoped", "Stoped");
        }
    }

    public void error(Exception e) {
        Log.e("erro", e.getLocalizedMessage());
        Log.e("erro", e.getMessage());
        restart();
    }

    public void restart() {
        stop();
        start();
    }


}
