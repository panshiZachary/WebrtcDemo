package com.ps.webrtcdemo.client;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import com.ps.webrtcdemo.interfaces.RoomConnectionParameters;
import com.ps.webrtcdemo.interfaces.SignalingEvents;
import com.ps.webrtcdemo.interfaces.SignalingParameters;
import com.ps.webrtcdemo.utils.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.SessionDescription;

public class WebSocketRTCClient implements WebSocketChannelClient.WebSocketChannelEvents {
    private static final String TAG = "WSRTCClient";
    private static final String ROOM_JOIN = "join";
    private static final String ROOM_MESSAGE = "message";
    private static final String ROOM_LEAVE = "leave";


     WebSocketChannelClient wsClient;

    public void sendOfferSdp(final SessionDescription sdp) {
        handler.post(new Runnable() {
            @Override
            public void run() {

                if (roomState != ConnectionState.CONNECTED) {
                    return;
                }
                JSONObject json = new JSONObject();
                try {
                    json.put("sdp", sdp.description);
                    json.put("type", "offer");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                sendPostMessage(MessageType.MESSAGE, messageUrl, json.toString());
            }
        });

    }

    // Send SDP or ICE candidate to a room server.
//    http
    private void sendPostMessage(
            final MessageType messageType, final String url, final String message) {
        String logInfo = url;
        if (message != null) {
            logInfo += ". Message: " + message;
        }
        AsyncHttpURLConnection httpConnection =
                new AsyncHttpURLConnection("POST", url, message, new AsyncHttpURLConnection.AsyncHttpEvents() {
                    @Override
                    public void onHttpError(String errorMessage) {
                    }

                    @Override
                    public void onHttpComplete(String response) {
                        if (messageType == MessageType.MESSAGE) {
                            try {
                                JSONObject roomJson = new JSONObject(response);
                                String result = roomJson.getString("result");
                                if (!result.equals("SUCCESS")) {
                                }
                            } catch (JSONException e) {
                            }
                        }
                    }
                });
        httpConnection.send();
    }
//sdp 交换成功

//    ICE 交换
    public void sendAnserSdp(final SessionDescription sdp) {
        handler.post(new Runnable(){

            @Override
            public void run() {
//
                Log.i("david", "run: 6");
                JSONObject jsonObject = new JSONObject();
                try {
                    jsonObject.put("sdp", sdp.description);
                    jsonObject.put("type","answer");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                wsClient.send(jsonObject.toString());
            }
        });


    }

    public void sendLocalIceCandidate(final IceCandidate candidate) {
//        发送中国公文
        handler.post(new Runnable() {
            @Override
            public void run() {
                JSONObject json = new JSONObject();
                try {
                    json.put("type", "candidate");
                    json.put("label", candidate.sdpMLineIndex);
                    json.put( "id", candidate.sdpMid);
                    json.put( "candidate", candidate.sdp);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

                if (initiator) {
                    // Call initiator sends ice candidates to GAE server.
                    if (roomState != ConnectionState.CONNECTED) {
                        return;
                    }
                    sendPostMessage(MessageType.MESSAGE, messageUrl, json.toString());
                    if (connectionParameters.loopback) {
                        mainActivityInterface.onRemoteIceCandidate(candidate);
                    }
                } else {
                    wsClient.send(json.toString());
                }


            }
        });

    }


    private enum ConnectionState {NEW, CONNECTED, CLOSED, ERROR}

    private enum MessageType {MESSAGE, LEAVE}

    private   Handler handler;
    private boolean initiator;
    private SignalingEvents mainActivityInterface;
    private ConnectionState roomState;
    private RoomConnectionParameters connectionParameters;
    private String messageUrl;
//
    private String leaveUrl;



    public WebSocketRTCClient(SignalingEvents mainActivityInterface) {
        this.mainActivityInterface = mainActivityInterface;
        roomState = ConnectionState.NEW;
        final HandlerThread handlerThread = new HandlerThread(TAG);
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }


    public void connectToRoom(RoomConnectionParameters roomConnectionParameters) {
//耗时

        this.connectionParameters = roomConnectionParameters;

        handler.post(new Runnable() {
            @Override
            public void run() {
//                    子线程
                final String connectionUrl = connectionParameters.roomUrl + "/" + "join/" + connectionParameters.roomId;

//                http请求
               wsClient = new WebSocketChannelClient(handler, WebSocketRTCClient.this);
                RoomParametersFetcher.RoomParametersFetcherEvents roomParametersFetcherEvents = new RoomParametersFetcher.RoomParametersFetcherEvents() {
                    @Override
                    public void onSignalingParametersReady(final SignalingParameters params) {
//                        回调这个接口    信号参数
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                initiator = params.initiator;
                                messageUrl = Utils.getMessageUrl(connectionParameters, params);
                                roomState = ConnectionState.CONNECTED;
//                        创建webrtc的连接  peerConnection
                                mainActivityInterface.onConnectedToRoom(params);
//Socket   ---->   注册状态------------》 假设未来的某个时候14号技师上线       长连接 推送给我
                                wsClient.connect(params.wssUrl, params.wssPostUrl);
//                        注册
                                wsClient.register(connectionParameters.roomId, params.clientId);
                            }
                        });

                    }

                    @Override
                    public void onSignalingParametersError(String description) {

                    }
                };

                RoomParametersFetcher roomParametersFetcher = new RoomParametersFetcher
                        (connectionUrl,null,roomParametersFetcherEvents );
                roomParametersFetcher.makeRequest();

            }
        });





    }

    private void disconnectFromRoomInternal() {

        sendPostMessage(MessageType.LEAVE, leaveUrl, null);
    }



//lance  进入了房间    david---->lance
//1    peerConntion.createAnswer()

//2     onWebSocketMessage  主叫发送了   ICeCandidate
//    回调这  当被叫响应了 answer   sdp传过来服务器  服务器会推送 到 onWebSocketMessage
    @Override
    public void onWebSocketMessage(String message) {
//如果      对方上线消息  createAnswer -----》
// 文字


        if (wsClient.getState() != WebSocketChannelClient.WebSocketConnectionState.REGISTERED) {
            Log.e(TAG, "Got WebSocket message in non registered state.");
            return;
        }

        try {
            JSONObject json  = new JSONObject(message);
            String msgText = json.getString("msg");
            if (msgText.length() > 0) {
                Log.i(TAG, "onWebSocketMessage: json1  "+msgText);
                JSONObject json1 = new JSONObject(msgText);

                String type = json1.getString("type");
                if (type.equals("candidate")) {
                    Log.i(TAG, "onWebSocketMessage:------lable值 ----> "+json1.getInt("label"));
                    IceCandidate iceCandidate = new IceCandidate(json1.getString("id"), json1.getInt("label"),
                            json1.getString("candidate"));
                    mainActivityInterface.onRemoteIceCandidate(iceCandidate);
                } else if (type.equals("remove-candidates")) {
                    JSONArray candidateArray = json1.getJSONArray("candidates");
                    IceCandidate[] candidates = new IceCandidate[candidateArray.length()];
                    for (int i = 0; i < candidateArray.length(); ++i) {
                        JSONObject iceJson = candidateArray.getJSONObject(i);
                        IceCandidate iceCandidate = new IceCandidate(iceJson.getString("id"), iceJson.getInt("lable"), iceJson.getString("candidate"));
                        candidates[i] = iceCandidate;
                    }
                    mainActivityInterface.onRemoteIceCandidatesRemoved(candidates);
                } else if (type.equals("answer")) {
//
                    if (initiator) {
//                        主动发送视频       并且对方同意了    sdp 对方
                        SessionDescription sdp = new SessionDescription(
                                SessionDescription.Type.fromCanonicalForm(type), json1.getString("sdp"));
                        mainActivityInterface.onRemoteDescription(sdp);
                    } else {
                    }
                } else if (type.equals("offer")) {
                    if (!initiator) {
                        SessionDescription sdp = new SessionDescription(
                                SessionDescription.Type.fromCanonicalForm(type), json1.getString("sdp"));
                        mainActivityInterface.onRemoteDescription(sdp);
                    } else {
                    }
                } else if (type.equals("bye")) {
                    mainActivityInterface.onChannelClose();
                } else {
                }
            } else {

            }
        } catch (JSONException e) {
            Log.i(TAG, "onWebSocketMessage: "+e.toString());
        }

    }

    @Override
    public void onWebSocketClose() {

    }

    @Override
    public void onWebSocketError(String description) {

    }

}
