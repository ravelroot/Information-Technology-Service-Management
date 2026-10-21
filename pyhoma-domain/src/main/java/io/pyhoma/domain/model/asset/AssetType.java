package io.pyhoma.domain.model.asset;

public enum AssetType {
    COMPUTER("monitor-smartphone", true, true),
    LAPTOP("laptop", true, true),
    SERVER("server", true, true),
    MONITOR("monitor", false, false),
    PRINTER("printer", false, true),
    NETWORK_DEVICE("network", false, true),
    PHONE("smartphone", false, true),
    TABLET("tablet", true, true),
    PERIPHERAL("mouse", false, false),
    OTHER("box", false, false);

    private final String icon;
    private final boolean hardwareDetails;
    private final boolean networked;

    AssetType(String icon, boolean hardwareDetails, boolean networked) {
        this.icon = icon;
        this.hardwareDetails = hardwareDetails;
        this.networked = networked;
    }

    public String icon() {
        return icon;
    }

    public boolean hasHardwareDetails() {
        return hardwareDetails;
    }

    public boolean isNetworked() {
        return networked;
    }
}
