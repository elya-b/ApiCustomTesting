package elya.general.enums;

public enum StatusInfo {
    STATUS("status"),
    SERIES("series"),
    REASON("reason");

    private final String statusInfo;

    StatusInfo (String statusInfo) {
        this.statusInfo = statusInfo;
    }

    @Override
    public String toString() {
        return statusInfo;
    }
}
