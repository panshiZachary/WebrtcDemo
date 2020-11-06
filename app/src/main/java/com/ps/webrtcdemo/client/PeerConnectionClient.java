package com.ps.webrtcdemo.client;

import android.content.Context;
import android.util.Log;

import com.ps.webrtcdemo.interfaces.SignalingParameters;
import com.ps.webrtcdemo.utils.Utils;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.DataChannel;
import org.webrtc.EglBase;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RtpSender;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.StatsReport;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;
import org.webrtc.voiceengine.WebRtcAudioUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class PeerConnectionClient {


    private  ScheduledExecutorService executor;
    private Context context;
    private PeerConnectionFactory factory;
    private static final PeerConnectionClient instance = new PeerConnectionClient();
    PeerConnectionFactory.Options options = null;
    private final PCObserver pcObserver = new PCObserver();
    private SDPObserver sdpObserver = new SDPObserver();
    private MediaConstraints pcConstraints;

    private int videoWidth;
    private int videoHeight;
    private int videoFps;
    MediaConstraints audioConstraints;
    private MediaConstraints sdpMediaConstraints;
    private MediaStream mediaStream;
//    本地的视频源
    private VideoTrack localVideoTrack;
//远程的视频源
    private VideoTrack remoteVideoTrack;

    private PeerConnection peerConnection;
    private List<IceCandidate> candidates;
//发一个offer 才算初始化成功
    boolean isInitiator;
//    本地的sdp
    private SessionDescription localSdp;
    private PeerConnectionEvents mainactivityInterface;
    SurfaceViewRenderer remoteView;
    private RtpSender localVideoSender;
    public static PeerConnectionClient getInstance() {
        return instance;
    }
    private PeerConnectionClient() {
        executor = Executors.newSingleThreadScheduledExecutor();
    }

    public void createPeerConnectionFactory(final Context context, PeerConnectionParameters peerConnectionParameters,
                                            PeerConnectionEvents events      ) {
//        创建连接
        mainactivityInterface = events;
//        耗时
        executor.execute(new Runnable() {
            @Override
            public void run() {
//                创建链接通道  并且初始化      java socket
                PeerConnectionFactory.initializeInternalTracer();
                PeerConnectionFactory.initializeFieldTrials("");
//
                WebRtcAudioUtils.setWebRtcBasedAcousticEchoCanceler(false);
                WebRtcAudioUtils.setWebRtcBasedAutomaticGainControl(false);
                WebRtcAudioUtils.setWebRtcBasedNoiseSuppressor(true);

                PeerConnectionFactory.initializeAndroidGlobals(context, true, true, true
                );
//                初始化的工作
                factory = new PeerConnectionFactory(options);




//                java  数据库连接池


            }
        });






    }
//创建 空连接  native   scoket（“ip”）
    public void createPeerConnection(final EglBase.Context renderEGLContext,
                                     final SurfaceViewRenderer localview, final SurfaceViewRenderer remoteView,
                                     final VideoCapturer videoCapturer,
                                     final SignalingParameters signalingParameters) {

        candidates = new ArrayList<>();
        this.remoteView = remoteView;
        executor.execute(new Runnable() {
            @Override
            public void run() {
                Log.i("david", "run: ----------------------------------->2");
                pcConstraints = new MediaConstraints();
                pcConstraints.optional.add(
                        new MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"));
//1280  *720
//
                videoWidth = 1280;
                videoHeight = 720;
                videoFps = 30;
                // Create audio constraints.
                audioConstraints = new MediaConstraints();
                sdpMediaConstraints = new MediaConstraints();
                sdpMediaConstraints.mandatory.add(
                        new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
                sdpMediaConstraints.mandatory.add(
                        new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
                Log.i("david", " 2  PeerConnectionFactory 初始化是否完成   "+factory);
                factory.setVideoHwAccelerationOptions(renderEGLContext, renderEGLContext);
                PeerConnection.RTCConfiguration rtcConfig =
                        new PeerConnection.RTCConfiguration(signalingParameters.iceServers);
                // TCP candidates are only useful when connecting to a server that supports
                // ICE-TCP.
                rtcConfig.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED;
                rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE;
                rtcConfig.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE;
                rtcConfig.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY;
                // Use ECDSA encryption.
                rtcConfig.keyType = PeerConnection.KeyType.ECDSA;
                //不是阻塞   peerConnection--->
                peerConnection = factory.createPeerConnection(rtcConfig, pcConstraints, pcObserver);

//                声音    推送B
//                创建一个音频音源
                AudioSource audioSource = factory.createAudioSource(audioConstraints);
                AudioTrack audioTrack = factory.createAudioTrack("ARDAMSa0", audioSource);

                audioTrack.setEnabled(true);


                mediaStream = factory.createLocalMediaStream("ARDAMS");
                mediaStream.addTrack(audioTrack);
//               音源有了  推送B


                //                创建一个视频源
                VideoSource videoSource = factory.createVideoSource(videoCapturer);
//                预览的格式
                videoCapturer.startCapture(videoWidth, videoHeight, videoFps);

                localVideoTrack = factory.createVideoTrack("ARDAMSv0", videoSource);
                localVideoTrack.setEnabled(true);
//                摄像头的数据会被直接渲染到 View
                localVideoTrack.addRenderer(new VideoRenderer(localview));
//远端就能够看到 摄像头的画面
                mediaStream.addTrack(localVideoTrack);
                peerConnection.addStream(mediaStream);
                for (RtpSender sender : peerConnection.getSenders()) {
                    if (sender.track() != null) {
                        String trackType = sender.track().kind();
                        if (trackType.equals("video")) {
                            localVideoSender = sender;
                        }
                    }
                }
//
//                视频B端



            }
        });
//
    }
    //   2   B  端   被邀请方
    public void createAnswer() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                Log.i("david", "createAnswer: 2");
                if (peerConnection != null) {
                    isInitiator = false;
                    //   2
                    peerConnection.createAnswer(sdpObserver, sdpMediaConstraints);
                }
            }
        });


    }
    public void createOffer() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                if (peerConnection != null) {
                    isInitiator = true;
                    peerConnection.createOffer(sdpObserver, sdpMediaConstraints);
                }
            }
        });
    }

    public void setRemoteDescription(final SessionDescription remoteDescription) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    if (peerConnection == null) {
                        return;

                    }
//                    sdp协议
                    String sdpDescrption = remoteDescription.description;
                    SessionDescription sdpRemote = new SessionDescription(remoteDescription.type, sdpDescrption);
                    peerConnection.setRemoteDescription(sdpObserver, sdpRemote);

                }
            });


    }

    private  void putAllIceCandidate() {
        if (candidates != null) {
            for (IceCandidate candidate : candidates) {
                peerConnection.addIceCandidate(candidate);
            }
            candidates = null;
        }
    }

    public void addRemoteIceCandidate(final IceCandidate candidate) {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                if (peerConnection != null) {
//                    IceCandidate   集合  //不仅是scoket iceCandite   自己的ICECandidate
                    if (candidates != null) {
                        candidates.add(candidate);
                    }
                    else {
                        peerConnection.addIceCandidate(candidate);
                    }

                }
            }
        });
    }


    class SDPObserver implements SdpObserver {
//        offer如果创建成功   onCreateSuccess  网络权限
//        本地sdp怎么来      必须先发offer   onCreateSuccess（SessionDescription ）
        @Override
        public void onCreateSuccess(SessionDescription sessionDescription) {
            if (localSdp != null) {
                return;
            }
            String sdpDescription = sessionDescription.description;

            String sdpDescription1 = Utils.preferCodec(sdpDescription, "VP8", false);
//本地的sdp  重新构建
            final SessionDescription sdp = new SessionDescription(sessionDescription.type, sdpDescription1);
            localSdp = sdp;
            executor.execute(new Runnable() {
                @Override
                public void run() {
//                    3
                    Log.i("david", "onCreateSuccess:3");
                    if (peerConnection != null) {
//                        耗时  线程  native   网络请求   // 不一定成功  交换机     wan口没有连接网线
                        peerConnection.setLocalDescription(sdpObserver, sdp);
                    }
                }
            });

        }
// 设置本地的SDP成功   (setLocalDescription ) 会引发onSetSuccess  方法回调
//       peerConnection.setLocalDescription 触发 ------>  onSetSuccess       对方的ICE拿到手
        @Override
        public void onSetSuccess() {
//            待续未完
//被叫
//            拿到了本地的sdp   -----本地sdp  告诉14号技师
//        网络发送sdp
        executor.execute(new Runnable() {
            @Override
            public void run() {
                if (peerConnection == null) {

                    return;
                }
//                4
                Log.i("david", "run: 4  ");
//                主叫
//                isInitiator   true  1      false  2
                if (isInitiator) {
//                    远端没有设置sdp    自己的sdp通过服务器传递出去
                    if (peerConnection.getRemoteDescription() == null) {
                        mainactivityInterface.onLocalDescription(localSdp);
                    }else{
                        putAllIceCandidate();
                    }

                } else if (peerConnection.getLocalDescription() != null) {
// 需要把被叫的sdp  传给   主叫  false  2
                    mainactivityInterface.onLocalDescription(localSdp);
                    putAllIceCandidate();
                }
            }
        });






        }

        @Override
        public void onCreateFailure(String s) {

        }

        @Override
        public void onSetFailure(String s) {

        }
    }


    public static class DataChannelParameters {
        public final boolean ordered;
        public final int maxRetransmitTimeMs;
        public final int maxRetransmits;
        public final String protocol;
        public final boolean negotiated;
        public final int id;

        public DataChannelParameters(boolean ordered, int maxRetransmitTimeMs, int maxRetransmits,
                                     String protocol, boolean negotiated, int id) {
            this.ordered = ordered;
            this.maxRetransmitTimeMs = maxRetransmitTimeMs;
            this.maxRetransmits = maxRetransmits;
            this.protocol = protocol;
            this.negotiated = negotiated;
            this.id = id;
        }
    }
//    sdp   http  发    socket 收
//    ice   http发     回调
    private class PCObserver implements PeerConnection.Observer{
//        产生ICE令牌   客户端 到服务器
    @Override
    public void onIceCandidate(final IceCandidate iceCandidate) {
//        产生中国公文  http   发送给瑞士
        executor.execute(new Runnable() {
            @Override
            public void run() {
                mainactivityInterface.onIceCandidate(iceCandidate);
            }
        });



    }
//方法 native 回调
        @Override
        public void onSignalingChange(PeerConnection.SignalingState signalingState) {

        }
//ICE完全交换成功
        @Override
        public void onIceConnectionChange(final PeerConnection.IceConnectionState iceConnectionState) {
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        if (iceConnectionState == PeerConnection.IceConnectionState.CONNECTED) {
                            mainactivityInterface.onIceConnected();
                        }else if (iceConnectionState == PeerConnection.IceConnectionState.DISCONNECTED) {
                            mainactivityInterface.onIceDisconnected();
                        } else if (iceConnectionState == PeerConnection.IceConnectionState.FAILED) {
                        }



                    }
                });



        }

        @Override
        public void onIceConnectionReceivingChange(boolean b) {

        }

        @Override
        public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {

        }



        @Override
        public void onIceCandidatesRemoved(IceCandidate[] iceCandidates) {

        }
//P2P最终入口
        @Override
        public void onAddStream(final MediaStream mediaStream) {

//        显示在这里
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    if (peerConnection == null) {
                        return;
                    }
                    if (mediaStream.audioTracks.size() > 1 || mediaStream.videoTracks.size() > 1) {
                        return;
                    }
//
                    if (mediaStream.videoTracks.size() == 1) {
                        remoteVideoTrack = mediaStream.videoTracks.get(0);
                        remoteVideoTrack.setEnabled(true);
                        remoteVideoTrack.addRenderer(new VideoRenderer(remoteView));
                    }


                }
            });


        }

        @Override
        public void onRemoveStream(MediaStream mediaStream) {

        }

        @Override
        public void onDataChannel(DataChannel dataChannel) {

        }

        @Override
        public void onRenegotiationNeeded() {

        }
    }

    public static class PeerConnectionParameters {
        public final boolean videoCallEnabled;
//        回拨的意思
        public final boolean loopback;

        public final boolean tracing;
        public final int videoWidth;
        public final int videoHeight;
//        帧率
        public final int videoFps;
//        比特率   60kb
        public final int videoMaxBitrate;
        public final String videoCodec;//视频编码
        public final boolean videoCodecHwAcceleration;//硬编码
        public final boolean videoFlexfecEnabled;
        public final int audioStartBitrate;
        public final String audioCodec;
        public final boolean noAudioProcessing;
        public final boolean aecDump;
        public final boolean enableLevelControl;
        private final DataChannelParameters dataChannelParameters;

        public static PeerConnectionParameters createDefault() {
            return new PeerConnectionParameters(true, false,
                    false, 0, 0, 0,
                    0, "VP8",
                    true,
                    false,
                    0, "OPUS",
                    false,
                    false,
                    false,
                    false,
                    false,
                    false,
                    false);
        }

        public PeerConnectionParameters(boolean videoCallEnabled, boolean loopback, boolean tracing,
                                        int videoWidth, int videoHeight, int videoFps, int videoMaxBitrate, String videoCodec,
                                        boolean videoCodecHwAcceleration, boolean videoFlexfecEnabled, int audioStartBitrate,
                                        String audioCodec, boolean noAudioProcessing, boolean aecDump, boolean useOpenSLES,
                                        boolean disableBuiltInAEC, boolean disableBuiltInAGC, boolean disableBuiltInNS,
                                        boolean enableLevelControl) {
            this(videoCallEnabled, loopback, tracing, videoWidth, videoHeight, videoFps, videoMaxBitrate,
                    videoCodec, videoCodecHwAcceleration, videoFlexfecEnabled, audioStartBitrate, audioCodec,
                    noAudioProcessing, aecDump, useOpenSLES, disableBuiltInAEC, disableBuiltInAGC,
                    disableBuiltInNS, enableLevelControl, null);
        }

        public PeerConnectionParameters(boolean videoCallEnabled, boolean loopback, boolean tracing,
                                        int videoWidth, int videoHeight, int videoFps, int videoMaxBitrate, String videoCodec,
                                        boolean videoCodecHwAcceleration, boolean videoFlexfecEnabled, int audioStartBitrate,
                                        String audioCodec, boolean noAudioProcessing, boolean aecDump, boolean useOpenSLES,
                                        boolean disableBuiltInAEC, boolean disableBuiltInAGC, boolean disableBuiltInNS,
                                        boolean enableLevelControl, DataChannelParameters dataChannelParameters) {
            this.videoCallEnabled = videoCallEnabled;
            this.loopback = loopback;
            this.tracing = tracing;
            this.videoWidth = videoWidth;
            this.videoHeight = videoHeight;
            this.videoFps = videoFps;
            this.videoMaxBitrate = videoMaxBitrate;
            this.videoCodec = videoCodec;
            this.videoFlexfecEnabled = videoFlexfecEnabled;
            this.videoCodecHwAcceleration = videoCodecHwAcceleration;
            this.audioStartBitrate = audioStartBitrate;
            this.audioCodec = audioCodec;
            this.noAudioProcessing = noAudioProcessing;
            this.aecDump = aecDump;
            this.enableLevelControl = enableLevelControl;
            this.dataChannelParameters = dataChannelParameters;

        }
    }

//  --------------------------接口-------------- 连接回调接口---------------------------------
    public interface PeerConnectionEvents {
        /**
         * Callback fired once local SDP is created and set.
         * 网络路径   客户端----》服务器
         */
        void onLocalDescription(final SessionDescription sdp);

        /**
         * Callback fired once local Ice candidate is generated.
         */
        void onIceCandidate(final IceCandidate candidate);

        /**
         * Callback fired once local ICE candidates are removed.
         */
        void onIceCandidatesRemoved(final IceCandidate[] candidates);

        /**
         * Callback fired once connection is established (IceConnectionState is
         * CONNECTED).
         */
        void onIceConnected();

        /**
         * Callback fired once connection is closed (IceConnectionState is
         * DISCONNECTED).
         */
        void onIceDisconnected();

        /**
         * Callback fired once peer connection is closed.
         */
        void onPeerConnectionClosed();

        /**
         * Callback fired once peer connection statistics is ready.
         */
        void onPeerConnectionStatsReady(final StatsReport[] reports);

        /**
         * Callback fired once peer connection error happened.
         */
        void onPeerConnectionError(final String description);
    }





}
