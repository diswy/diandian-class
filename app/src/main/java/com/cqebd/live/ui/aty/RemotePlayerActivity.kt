package com.cqebd.live.ui.aty

import android.annotation.SuppressLint
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewTreeObserver
import androidx.lifecycle.Observer
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.cqebd.live.R
import com.cqebd.live.databinding.ActivityRemotePlayerBinding
import com.jeremyliao.liveeventbus.LiveEventBus
import com.ywl5320.wlmedia.WlMedia
import com.ywl5320.wlmedia.enums.*
import com.ywl5320.wlmedia.listener.WlOnPcmDataListener
import com.ywl5320.wlmedia.listener.WlOnVideoViewListener
import cqebd.student.commandline.CacheKey
import cqebd.student.commandline.Command
import org.jetbrains.anko.toast
import xiaofu.lib.base.activity.BaseBindActivity
import xiaofu.lib.cache.ACache
import java.util.*

@Route(path = "/app/aty/remote_player")
class RemotePlayerActivity : BaseBindActivity<ActivityRemotePlayerBinding>() {
    private var source = "udp://239.0.0.2:5555"
//    private val source = "rtp://239.0.0.2:5555"

    private val observer = Observer<String> { s ->
        if (Command.BROADCAST_STOP == s || Command.DEMON_STOP == s) {
            finish()
        }
    }

    @Autowired
    @JvmField
    var isControl: Boolean = false

    private var screenshotImageViewX = 0
    private var screenshotImageViewY = 0
    private var xCord = 0
    private var yCord = 0
    private var initX = 0
    private var initY = 0
    private var userId = 0
    private var mouseMoved = false
    private var currentPressTime: Long = 0
    private var lastPressTime: Long = 0

    private lateinit var wlMedia: WlMedia

    override fun isFullScreen(): Boolean = true
    override fun isKeepScreenOn(): Boolean = true

    override fun getLayoutRes(): Int = R.layout.activity_remote_player

    @SuppressLint("ClickableViewAccessibility")
    override fun initialize(binding: ActivityRemotePlayerBinding) {
        ARouter.getInstance().inject(this)

        val cache = ACache.get(this)
        val url:String? = cache.getAsString(CacheKey.REMOTE_URL)
        if (url != null){
            source = url
        }

        LiveEventBus.get().with(Command.COMMAND, String::class.java).observe(this, observer)

        val vto = binding.container.viewTreeObserver
        vto.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                screenshotImageViewX = binding.container.height
                screenshotImageViewY = binding.container.width
                val obs = binding.container.viewTreeObserver
                obs.removeOnGlobalLayoutListener(this)
            }
        })
        wlMedia = WlMedia()
        wlMedia.setPlayModel(WlPlayModel.PLAYMODEL_AUDIO_VIDEO)//声音视频都播放
        wlMedia.setCodecType(WlCodecType.CODEC_MEDIACODEC)//优先使用硬解码
        wlMedia.mute = WlMute.MUTE_CENTER//立体声
        wlMedia.volume = 80//80%音量
        wlMedia.playPitch = 1.0f//正常速度
        wlMedia.playSpeed = 1.0f//正常音调
        wlMedia.setRtspTimeOut(30)//网络流超时时间
//        wlMedia.setShowPcmData(true);//回调返回音频pcm数据
        wlMedia.setSampleRate(WlSampleRate.RATE_44100)//设置音频采样率为指定值（返回的PCM就是这个采样率）
        binding.remoteSurface.setWlMedia(wlMedia)//给视频surface设置播放器

        wlMedia.setOnPreparedListener {
            wlMedia.setVideoScale(WlScaleType.SCALE_FULL_SURFACE)
            wlMedia.start()
            Log.d("xiaofu", "这里执行了！")
        }

        wlMedia.setOnLoadListener { load ->
            if (load) {
                Log.d("xiaofu", "加载中")
            } else {
                Log.d("xiaofu", "播放中")
            }
        }

        wlMedia.setOnErrorListener { code, msg -> Log.d("xiaofu", "code is :$code, msg is :$msg") }

        wlMedia.setOnCompleteListener { Log.d("xiaofu", "播放完成") }

        wlMedia.setOnPauseListener { pause ->
            if (pause) {
                Log.d("xiaofu", "暂停中")
            } else {
                Log.d("xiaofu", "继续播放")
            }
        }

        wlMedia.setOnPcmDataListener(object : WlOnPcmDataListener {
            override fun onPcmInfo(bit: Int, channel: Int, samplerate: Int) {
                Log.d("xiaofu", "pcm info samplerate :$samplerate")
            }

            override fun onPcmData(size: Int, data: ByteArray) {
                Log.d("xiaofu", "pcm data size :$size")
            }
        })

        binding.remoteSurface.setOnVideoViewListener(object : WlOnVideoViewListener {
            override fun initSuccess() {
                wlMedia.setSource(source)
                wlMedia.prepared()
            }

            override fun moveSlide(value: Double) {

            }

            override fun movdFinish(value: Double) {
                wlMedia.seek(value.toInt().toDouble())
            }
        })


        play(source)
//        toast(isControl.toString())
        if (isControl) {
            binding.controllerRemote.setOnTouchListener { v, event ->
                when (event.action and MotionEvent.ACTION_MASK) {
                    MotionEvent.ACTION_DOWN -> {
                        xCord = screenshotImageViewX - event.y.toInt()
                        yCord = event.x.toInt()
                        initX = xCord
                        initY = yCord
                        sendCommand(Command.MOUSE_CLICK, xCord.toFloat() / screenshotImageViewX, yCord.toFloat() / screenshotImageViewY)
                        mouseMoved = false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        xCord = screenshotImageViewX - event.y.toInt()
                        yCord = event.x.toInt()
                        if (xCord - initX != 0 && yCord - initY != 0) {
                            initX = xCord
                            initY = yCord
                            sendCommand(Command.MOUSE_MOVE, xCord.toFloat() / screenshotImageViewX, yCord.toFloat() / screenshotImageViewY)
                            mouseMoved = true
                        }
                    }
                    MotionEvent.ACTION_UP -> {
                        currentPressTime = System.currentTimeMillis()
                        val interval = currentPressTime - lastPressTime
                        if (interval <= 1400 && !mouseMoved) {
                            sendCommand(Command.MOUSE_DOUBLE, initX.toFloat() / screenshotImageViewX, initY.toFloat() / screenshotImageViewY)
                        }
                    }
                }
                true
            }

        }
    }

    /**
     * 发送远程控制的响应命令
     */
    private val actionFormat = "%s %d %f %f"

    private fun sendCommand(command: String, x: Float, y: Float) {
        LiveEventBus.get()
                .with(Command.COMMAND, String::class.java)
                .post(String.format(Locale.CHINA, actionFormat, command, userId, x, y))
    }

    private fun play(url: String) {
        wlMedia.setSource(url)
        wlMedia.prepared()
    }

    private fun stop(view: View) {
        wlMedia.stop()
    }

    private fun pause(view: View) {
        wlMedia.pause()
    }

    private fun resume(view: View) {
        wlMedia.resume()
    }

    override fun onPause() {
        super.onPause()
        wlMedia.pause()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        wlMedia.onDestroy()
    }

    override fun onDestroy() {
        super.onDestroy()
        wlMedia.onDestroy()
    }

}
