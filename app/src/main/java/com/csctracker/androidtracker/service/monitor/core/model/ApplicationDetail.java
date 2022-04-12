package com.csctracker.androidtracker.service.monitor.core.model;

import lombok.Data;

import java.util.Date;

@Data
public class ApplicationDetail {
    private String name;
    private String activityDetail;
    private Long timeSpentMillis;
    private Date dateIni;
    private Date dateEnd;
    private String osName;
    private String hostName;
    private String pluginName;
    private String processName;
}
