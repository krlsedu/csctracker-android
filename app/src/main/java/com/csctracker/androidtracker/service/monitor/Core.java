package com.csctracker.androidtracker.service.monitor;

import android.content.Context;
import android.util.Log;
import com.csctracker.androidtracker.service.monitor.core.CscTrackerCore;
import com.csctracker.androidtracker.service.monitor.core.SqlLitle;
import com.csctracker.androidtracker.service.monitor.core.model.ApplicationDetail;
import com.csctracker.androidtracker.ui.MainActivity;

import java.util.List;

public class Core {

    private static final int WAIT_TIME = 20000;
    private static boolean ativo = false;

    private final Context context;

    private final Monitor monitor;

    private final SqlLitle sqlLitle;

    private MainActivity mainActivity;

    public Core(Context mainActivity, MainActivity mainActivity1) {
        this.context = mainActivity;
        this.mainActivity = mainActivity1;
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
            CscTrackerCore.init(context, mainActivity);
            new Thread(this::tracker).start();
        }
    }

    private void tracker() {
        if (CscTrackerCore.isDebug()) {
            Log.i("Initiated", "Initiated");
        }
//        try {
//            sqlLitle.limpaSync();
//        } catch (SQLException e) {
//            System.out.println("Erro ao limpar sync: " + e.getMessage());
//        }
        do {
            try {
                Thread.sleep(WAIT_TIME);
                Long ini = sqlLitle.getLastSync();
//                ini = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2023-03-26 23:15:00").getTime();
//                long fim = ini + (1000 * 60 * 60 * 24);
                long fim = System.currentTimeMillis();
//                fim = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2023-03-27 10:53:00").getTime();
                if (ini == null || ini == 0) {
//                    fim = ini + (1000 * 60 * 60 * 24);
                    ini = fim - (1000 * 60 * 60 * 4);
                }
//                System.out.println("fim: " + new Date(fim));
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
