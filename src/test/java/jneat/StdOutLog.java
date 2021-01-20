package jneat;

import log.HistoryLog;

public class StdOutLog implements HistoryLog {
    @Override
    public void statusMessage(String message) {
        System.out.println(message);
    }

    @Override
    public void sendToStatus(String _msg) {
        System.out.println(_msg);
    }

    @Override
    public void sendToLog(String message) {
        System.out.println(message);
    }
}
