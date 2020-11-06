package com.ps.webrtcdemo.interfaces;

public class RoomConnectionParameters {
    public final String roomUrl;
    public final String roomId;
    public final boolean loopback;

    public RoomConnectionParameters(String roomUrl, String roomId, boolean loopback) {
        this.roomUrl = roomUrl;
        this.roomId = roomId;
        this.loopback = loopback;
    }
}
