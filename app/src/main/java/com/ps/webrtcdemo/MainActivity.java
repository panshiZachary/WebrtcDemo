package com.ps.webrtcdemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import com.ps.webrtcdemo.client.PeerConnectionClient;
import com.ps.webrtcdemo.client.WebSocketRTCClient;
import com.ps.webrtcdemo.interfaces.RoomConnectionParameters;
import com.ps.webrtcdemo.interfaces.SignalingEvents;
import com.ps.webrtcdemo.interfaces.SignalingParameters;
import com.ps.webrtcdemo.utils.Utils;
import com.ps.webrtcdemo.view.PercentFrameLayout;

import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.RendererCommon;
import org.webrtc.SessionDescription;
import org.webrtc.StatsReport;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;

import static org.webrtc.RendererCommon.ScalingType.SCALE_ASPECT_FILL;
import static org.webrtc.RendererCommon.ScalingType.SCALE_ASPECT_FIT;

public class MainActivity extends AppCompatActivity implements PeerConnectionClient.PeerConnectionEvents, SignalingEvents {
//    remoteView   远端的信息  localView 近端信息
    private SurfaceViewRenderer remoteView,localView;
    //房间服务器参数
     RoomConnectionParameters roomConnectionParameters;
    private PercentFrameLayout remotePercentLayout, localPercentLayout;
    //    连接参数  （视频  码率 编码）
    private PeerConnectionClient.PeerConnectionParameters peerConnectionParameters;



//    绘制全局的上下文
    private EglBase rootEglBase;

    PeerConnectionClient peerConnectionClient;

    WebSocketRTCClient webSocketRTCClient;

    private SignalingParameters signalingParameters;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initView();
        initWebrtc();
//        连接房间服务器
//        起点
        connectRoom();
    }

    private void connectRoom() {

//        opnfire 即时聊天  webrtc

        roomConnectionParameters = new RoomConnectionParameters("https://47.99.85.202", "123456", false);
        webSocketRTCClient = new WebSocketRTCClient(this);
        webSocketRTCClient.connectToRoom(roomConnectionParameters);
    }

    private void initWebrtc() {
//        Opengl绘制
        rootEglBase= EglBase.create();

        remoteView.init(rootEglBase.getEglBaseContext(), null);
        localView.init(rootEglBase.getEglBaseContext(), null);
//悬浮顶端
        localView.setZOrderMediaOverlay(true);
//        硬件加速
        localView.setEnableHardwareScaler(true);

        remoteView.setEnableHardwareScaler(true);


        remoteView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL);
//网络数据  内存  ----
        remoteView.setMirror(true);

//        设置连接参数
        peerConnectionParameters=  PeerConnectionClient.PeerConnectionParameters.createDefault();

//        多   初始化
        peerConnectionClient=PeerConnectionClient.getInstance();

        peerConnectionClient.createPeerConnectionFactory(this, peerConnectionParameters, this);




    }

    private void initView() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        setContentView(R.layout.activity_main);
        remoteView = findViewById(R.id.remote_video_view);
        localView = findViewById(R.id.local_video_view);
        remotePercentLayout = findViewById(R.id.remote_video_layout);
        localPercentLayout = findViewById(R.id.local_video_layout);






    }


//A  客户端  //
//
    @Override
    public void onLocalDescription(SessionDescription sdp) {
//            转发  websocketClient
        Log.i("david", "run:5  ");
        if (webSocketRTCClient != null) {
            if (signalingParameters.initiator) {
//                主动发送offer
                webSocketRTCClient.sendOfferSdp(sdp);
            }else
            {
//              6    被叫 发送 sdp  到服务器
                webSocketRTCClient.sendAnserSdp(sdp);


            }


        }
    }

    public void disconnection(View view) {

    }

    @Override
    public void onIceCandidate(final IceCandidate candidate) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                webSocketRTCClient.sendLocalIceCandidate(candidate);

            }
        });





    }

    @Override
    public void onIceCandidatesRemoved(IceCandidate[] candidates) {

    }

    @Override
    public void onIceConnected() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateVideoView();
            }
        });
    }

    private void updateVideoView() {
        remotePercentLayout.setPosition(0, 0, 100, 100);
        remoteView.setScalingType(SCALE_ASPECT_FILL);
        remoteView.setMirror(false);
        localPercentLayout.setPosition(
                72, 72, 25, 25);
        localView.setScalingType(SCALE_ASPECT_FIT);
        localView.setMirror(true);

        localPercentLayout.requestLayout();
        remotePercentLayout.requestLayout();
    }

    @Override
    public void onIceDisconnected() {

    }

    @Override
    public void onPeerConnectionClosed() {

    }

    @Override
    public void onPeerConnectionStatsReady(StatsReport[] reports) {

    }

    @Override
    public void onPeerConnectionError(String description) {

    }

//    ------------房间服务器回调--------------------
////B  客户端执行这一段代码
    @Override
    public void onConnectedToRoom(SignalingParameters params) {

//        peerconnectionClitent
        signalingParameters = params;
        //VideoCapturer  对
        VideoCapturer videoCapturer = Utils.createVideoCaptuer(this);
//        创建空的连接PeerConnection
        peerConnectionClient.createPeerConnection(rootEglBase.getEglBaseContext(), localView
                , remoteView, videoCapturer, signalingParameters);
        Log.i("david", "onConnectedToRoom: 1");
//        发送请求 offer
        if (signalingParameters.initiator) {
//            主叫 找 被叫
            peerConnectionClient.createOffer();


        }else {


//
//          1   被叫  找 主 叫
            if (params.offerSdp != null) {
                     peerConnectionClient.setRemoteDescription(params.offerSdp);
                    peerConnectionClient.createAnswer();
            }



        }





//




    }
//    主叫接收到   被叫的sdp
    @Override
    public void onRemoteDescription(final SessionDescription sdp) {
        runOnUiThread(new Runnable() {
                          @Override
                          public void run() {
                              if (peerConnectionClient == null) {
                                  return;
                              }
                              peerConnectionClient.setRemoteDescription(sdp);
//signalingParameters.initiator=true  主动发送视频
                              if (!signalingParameters.initiator) {
                                  peerConnectionClient.createAnswer();
                              }
                          }
                      }
        );
    }
//收到了对方的ICE   socket

    @Override
    public void onRemoteIceCandidate(final IceCandidate candidate) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                peerConnectionClient.addRemoteIceCandidate(candidate);
            }
        });
    }




    @Override
    public void onRemoteIceCandidatesRemoved(IceCandidate[] candidates) {

    }

    @Override
    public void onChannelClose() {

    }

    @Override
    public void onChannelError(String description) {

    }
}
