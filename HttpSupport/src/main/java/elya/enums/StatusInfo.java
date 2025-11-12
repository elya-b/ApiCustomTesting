package elya.enums;

public enum StatusInfo {
    STATUS("status"),
    SERIES("series"),
    REASON("reason");

    private final String statusInfo;

    StatusInfo (String statusInfo) {
        this.statusInfo = statusInfo;
    }

    public String getStatusInfo() {
        return statusInfo;
    }

    public String toString() {
        return statusInfo;
    }
}
