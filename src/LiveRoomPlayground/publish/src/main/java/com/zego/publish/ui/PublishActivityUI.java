package com.zego.publish.ui;

import android.app.Activity;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.zego.common.entity.SDKConfigInfo;
import com.zego.common.entity.StreamQuality;
import com.zego.publish.R;
import com.zego.publish.databinding.ActivityPublishBinding;
import com.zego.publish.databinding.PublishInputStreamIdLayoutBinding;
import com.zego.common.ZGBaseHelper;
import com.zego.common.ZGConfigHelper;
import com.zego.common.ZGPublishHelper;
import com.zego.common.constants.ZGLiveRoomConstants;
import com.zego.common.ui.BaseActivity;
import com.zego.common.ui.WebActivity;
import com.zego.common.util.AppLogger;
import com.zego.zegoliveroom.callback.IZegoLivePublisherCallback;
import com.zego.zegoliveroom.callback.IZegoResponseCallback;
import com.zego.zegoliveroom.constants.ZegoConstants;
import com.zego.zegoliveroom.entity.AuxData;
import com.zego.zegoliveroom.entity.ZegoPublishStreamQuality;

import java.util.HashMap;

public class PublishActivityUI extends BaseActivity {


    private ActivityPublishBinding binding;
    private PublishInputStreamIdLayoutBinding layoutBinding;
    private StreamQuality streamQuality = new StreamQuality();
    private SDKConfigInfo sdkConfigInfo = new SDKConfigInfo();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_publish);

        // 利用DataBinding 可以通过bean类驱动UI变化。
        // 方便快捷避免需要写一大堆 setText 等一大堆臃肿的代码。
        binding.setQuality(streamQuality);
        binding.setConfig(sdkConfigInfo);

        layoutBinding = binding.layout;
        layoutBinding.startButton.setText(getString(R.string.tx_start_publish));

        streamQuality.setRoomID(String.format("RoomID : %s", getIntent().getStringExtra("roomID")));
        // 调用sdk 开始预览接口 设置view 启用预览
        ZGPublishHelper.sharedInstance().startPreview(binding.preview);

        // 设置推流回调
        ZGPublishHelper.sharedInstance().setPublisherCallback(new IZegoLivePublisherCallback() {

            // 推流回调文档说明: <a>https://doc.zego.im/API/ZegoLiveRoom/Android/html/index.html</a>

            @Override
            public void onPublishStateUpdate(int errorCode, String streamID, HashMap<String, Object> hashMap) {
                // 推流状态更新，errorCode 非0 则说明推流成功
                // 推流常见错误码请看文档: <a>https://doc.zego.im/CN/490.html</a>

                if (errorCode == 0) {

                    binding.title.setTitleName(getString(R.string.tx_publish_success));
                    AppLogger.getInstance().i(ZGPublishHelper.class, "推流成功, streamID : %s", streamID);
                    Toast.makeText(PublishActivityUI.this, getString(R.string.tx_publish_success), Toast.LENGTH_SHORT).show();
                } else {

                    binding.title.setTitleName(getString(R.string.tx_publish_fail));
                    AppLogger.getInstance().i(ZGPublishHelper.class, "推流失败, streamID : %s, errorCode : %d", streamID, errorCode);
                    Toast.makeText(PublishActivityUI.this, getString(R.string.tx_publish_fail), Toast.LENGTH_SHORT).show();
                    // 当推流失败时需要显示布局
                    showInputStreamIDLayout();
                }

            }

            @Override
            public void onJoinLiveRequest(int i, String s, String s1, String s2) {
                /**
                 * 房间内有人申请加入连麦时会回调该方法
                 * 观众端可通过 {@link com.zego.zegoliveroom.ZegoLiveRoom#requestJoinLive(IZegoResponseCallback)}
                 *  方法申请加入连麦
                 * **/
            }

            @Override
            public void onPublishQualityUpdate(String s, ZegoPublishStreamQuality zegoPublishStreamQuality) {
                /**
                 * 推流质量更新, 回调频率默认3秒一次
                 * 可通过 {@link com.zego.zegoliveroom.ZegoLiveRoom#setPublishQualityMonitorCycle(long)} 修改回调频率
                 */
                streamQuality.setFps(String.format("帧率: %f", zegoPublishStreamQuality.vnetFps));
                streamQuality.setBitrate(String.format("码率: %f kbs", zegoPublishStreamQuality.vkbps));
            }

            @Override
            public AuxData onAuxCallback(int i) {
                // aux混音，可以将外部音乐混进推流中。类似于直播中添加伴奏，掌声等音效
                // 另外还能用于ktv场景中的伴奏播放
                // 想深入了解可以进入进阶功能中的-mixing。
                // <a>https://doc.zego.im/CN/253.html</a> 文档中有说明
                return null;
            }

            @Override
            public void onCaptureVideoSizeChangedTo(int width, int height) {
                // 当采集时分辨率有变化时，sdk会回调该方法
                streamQuality.setResolution(String.format("分辨率: %dX%d", width, height));
            }

            @Override
            public void onMixStreamConfigUpdate(int i, String s, HashMap<String, Object> hashMap) {
                // 混流配置更新时会回调该方法。
            }

            @Override
            public void onCaptureVideoFirstFrame() {
                // 当SDK采集摄像头捕获到第一帧时会回调该方法

            }
        });


        // 监听摄像头与麦克风开关
        binding.swCamera.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (buttonView.isPressed()) {
                    sdkConfigInfo.setEnableCamera(isChecked);
                    ZGConfigHelper.sharedInstance().enableCamera(isChecked);
                }
            }
        });

        binding.swMic.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (buttonView.isPressed()) {
                    sdkConfigInfo.setEnableMic(isChecked);
                    ZGConfigHelper.sharedInstance().enableMic(isChecked);
                }

            }
        });

    }

    public void goSetting(View view) {
        PublishSettingActivityUI.actionStart(this);
    }

    @Override
    protected void onDestroy() {
        // 停止所有的推流和拉流后，才能执行 logoutRoom
        ZGPublishHelper.sharedInstance().stopPreviewView();
        ZGPublishHelper.sharedInstance().stopPublishing();

        // 当用户退出界面时退出登录房间
        ZGBaseHelper.sharedInstance().loginOutRoom();
        super.onDestroy();
    }

    /**
     * button 点击事件
     * 开始推流
     */
    public void onStart(View view) {
        String streamID = layoutBinding.edStreamId.getText().toString();
        if (!"".equals(streamID)) {
            // 隐藏输入StreamID布局
            hideInputStreamIDLayout();

            streamQuality.setStreamID(String.format("StreamID : %s", streamID));

            // 开始推流
            ZGPublishHelper.sharedInstance().startPublishing(streamID, "", ZegoConstants.PublishFlag.JoinPublish);

        } else {
            AppLogger.getInstance().i(PublishActivityUI.class, getString(com.zego.common.R.string.tx_stream_id_cannot_null));
            Toast.makeText(this, getString(com.zego.common.R.string.tx_stream_id_cannot_null), Toast.LENGTH_LONG).show();
        }
    }

    private void hideInputStreamIDLayout() {
        // 隐藏InputStreamIDLayout布局
        layoutBinding.getRoot().setVisibility(View.GONE);
        binding.publishStateView.setVisibility(View.VISIBLE);
    }

    private void showInputStreamIDLayout() {
        // 显示InputStreamIDLayout布局
        layoutBinding.getRoot().setVisibility(View.VISIBLE);
        binding.publishStateView.setVisibility(View.GONE);
    }


    public static void actionStart(Activity activity, String roomID) {
        Intent intent = new Intent(activity, PublishActivityUI.class);
        intent.putExtra("roomID", roomID);
        activity.startActivity(intent);
    }

    public void goCodeDemo(View view) {
        WebActivity.actionStart(this, "https://doc.zego.im/CN/209.html", getString(R.string.tx_publish_guide));
    }
}
