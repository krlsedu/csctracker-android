package com.csctracker.androidtracker.service.monitor;

import android.util.Log;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class SystemInfo {
    private static final int SECONDS_TO_IDLE = 30;

    private static String osName = null;

    private static String hostName = null;

    private State state = State.UNKNOWN;


    public static String getOsName() {
        if (osName == null) {
            osName = System.getProperty("os.name");
        }
        return osName;
    }

    public static String getHostName() {
        if (hostName == null) {
            try {
                InetAddress addr;
                addr = InetAddress.getLocalHost();
                hostName = addr.getHostName();
            } catch (UnknownHostException ex) {
                Log.e("System", ex.getMessage());
            }
        }
        return hostName;
    }

    private State getState() {

        int idleSec = 1 / 1000;
        return idleSec < SECONDS_TO_IDLE ? State.ONLINE : State.IDLE;

    }

    public boolean isChangedState() {

        State newState = getState();

        if (newState != state) {
            state = newState;
            Log.i("State", state.toString());
            return true;
        }

        return false;

    }

    public boolean isOnline() {
        return getState().equals(State.ONLINE);
    }

    enum State {
        UNKNOWN, ONLINE, IDLE, AWAY
    }
}
