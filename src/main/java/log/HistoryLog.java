package log;

public interface HistoryLog {
    void statusMessage(String message);

    void sendToStatus(String _msg);

    void sendToLog(String message);
}
