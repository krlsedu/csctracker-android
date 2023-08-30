package com.csctracker.androidtracker.service.monitor;

import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import com.csctracker.androidtracker.service.monitor.core.SqlLitle;
import com.csctracker.androidtracker.service.monitor.core.model.ApplicationDetail;

import java.lang.reflect.Field;
import java.util.*;

public class Monitor {

    private final UsageStatsManager usageStatsManager;
    private final PackageManager packageManager;
    private final SqlLitle sqlLitle;

    public Context context;

    private UsageEvents.Event eventAnt = null;

    public Monitor(Context context) {
        usageStatsManager = (UsageStatsManager) context.getSystemService(Context.USAGE_STATS_SERVICE);
        this.context = context;
        packageManager = context.getPackageManager();
        sqlLitle = new SqlLitle(context);
    }

    public List<ApplicationDetail> monitora(Long ini, Long fim) throws PackageManager.NameNotFoundException {
        UsageEvents events = usageStatsManager.queryEvents(ini, fim);
        UsageEvents.Event currentEvent;
        List<UsageEvents.Event> allEvents = new ArrayList<>();
        List<ApplicationDetail> applicationDetailArrayList = new ArrayList<>();
        while (events.hasNextEvent()) {
            currentEvent = new UsageEvents.Event();
            events.getNextEvent(currentEvent);
            String packageName = currentEvent.getPackageName();
//            Log.i("Evento", packageName);
            allEvents.add(currentEvent);
        }
        allEvents.sort(Comparator.comparing(UsageEvents.Event::getTimeStamp));
        UsageEvents.Event lastEvt = null;
        for (UsageEvents.Event eventAtu : allEvents) {
            try {
                if (eventAtu.getEventType() == UsageEvents.Event.ACTIVITY_RESUMED) {
                    if (eventAnt == null) {
                        eventAnt = eventAtu;
                    }
                    if (!eventAnt.getClassName().equals(eventAtu.getClassName())) {
                        ApplicationDetail applicationDetail = getApplicationDetail(eventAnt, eventAtu);
                        applicationDetailArrayList.add(applicationDetail);
                        eventAnt = eventAtu;
                        lastEvt = eventAtu;
                    }
                } else {
                    if ((eventAtu.getEventType() == UsageEvents.Event.MOVE_TO_BACKGROUND || eventAtu.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND) && eventAnt != null) {
                        ApplicationDetail applicationDetail = getApplicationDetail(eventAnt, eventAtu);
                        applicationDetailArrayList.add(applicationDetail);
                        lastEvt = eventAnt;
                        eventAnt = null;
                    }
                }
            } catch (Exception e) {
                Log.e("Erro monitor", Objects.requireNonNull(e.getMessage()));
            }
        }
        if (lastEvt != null) {
            try {
                sqlLitle.salvaLastSync(fim);
            } catch (Exception e) {
                //
            }
        }
        sqlLitle.salvaLastSync(fim);

        return applicationDetailArrayList;
    }

    private ApplicationDetail getApplicationDetail(UsageEvents.Event event, UsageEvents.Event eventRef) throws PackageManager.NameNotFoundException {
        long diff = eventRef.getTimeStamp() - event.getTimeStamp();
        ApplicationInfo ai = null;
        try {
            ai = packageManager.getApplicationInfo(event.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            //ignored
        }
        Field[] fields = Build.VERSION_CODES.class.getFields();
        ApplicationDetail applicationDetail = new ApplicationDetail();
        String osName = fields[Build.VERSION.SDK_INT].getName();
        applicationDetail.setActivityDetail(event.getClassName());
        applicationDetail.setProcessName(event.getPackageName());
        try {
            applicationDetail.setName(packageManager.getApplicationLabel(ai).toString());
        } catch (Exception e) {
            applicationDetail.setName("Unknown App");
        }
        applicationDetail.setTimeSpentMillis(diff);
        applicationDetail.setPluginName("android");
        applicationDetail.setOsName("Android - " + osName);
        applicationDetail.setDateIni(new Date(event.getTimeStamp()));
        applicationDetail.setDateEnd(new Date(event.getTimeStamp() + diff));
        applicationDetail.setHostName(Settings.Global.getString(context.getContentResolver(), Settings.Global.DEVICE_NAME));
        return applicationDetail;
    }
}
