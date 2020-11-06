package com.ps.webrtcdemo.interfaces;

import org.webrtc.IceCandidate;
import org.webrtc.PeerConnection;
import org.webrtc.SessionDescription;

import java.util.List;

public  class SignalingParameters {
    public final List<PeerConnection.IceServer> iceServers;
    public final boolean initiator;
    public final String clientId;
    public final String wssUrl;
    public final String wssPostUrl;
    public final SessionDescription offerSdp;
    public final List<IceCandidate> iceCandidates;

    public SignalingParameters(List<PeerConnection.IceServer> iceServers, boolean initiator,
                               String clientId, String wssUrl, String wssPostUrl, SessionDescription offerSdp,
                               List<IceCandidate> iceCandidates) {
/**
 *  "stun:47.107.132.117:3478",
 *   "turn:47.107.132.117:3478"
 *
 *     "username": "1542375414:dongnao",
 *             "credential": "+nyuCAwSeBwzAjbK2yzGVv4nTv8=",
 */
        this.iceServers = iceServers;
//      "is_initiator": "false",
        this.initiator = initiator;
//        "client_id": "10999957",
        this.clientId = clientId;
//          "wss_url": "wss://47.107.132.117:8089/ws",
        this.wssUrl = wssUrl;
//         "wss_post_url": "https://47.107.132.117:8089",
        this.wssPostUrl = wssPostUrl;

//        offer
        this.offerSdp = offerSdp;

//        offer
//         IceCandidate candidate = new IceCandidate(
//                                message.getString("id"), message.getInt("label"), message.getString("candidate"));

//         "{\"type\":\"candidate\",\
// "label\":0,\"id\":\"audio\",\"candidate\":\"candidate:2375628131 1 udp 1686052607
// 113.246.153.121 15751 typ srflx raddr 192.168.1.128 rport 36376 generation 0 ufrag o5oc network-id 3

// network-cost 10\"}"
        this.iceCandidates = iceCandidates;
    }


}