package xiaofu.lib.player.view

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import com.kk.taurus.playerbase.assist.OnVideoViewEventHandler
import com.kk.taurus.playerbase.entity.DataSource
import com.kk.taurus.playerbase.receiver.ReceiverGroup
import com.kk.taurus.playerbase.widget.BaseVideoView
import xiaofu.component.player.R
import xiaofu.lib.player.cover.ControllerCover
import xiaofu.lib.player.cover.LoadingCover
import xiaofu.lib.player.play.DataInter

/**
 * BaseVideoView的二次封装
 * 声明为生命周期组件
 * Created by @author xiaofu on 2019/2/26.
 */
class Player @JvmOverloads constructor(
        context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), LifecycleObserver {

    private var playerListener: PlayerListener? = null
    private var isFull = false
    private var showDefinition = true// 默认都提供清晰度选择
    private var controllerCover: ControllerCover? = null

    interface PlayerListener {
        fun onScreenChange(isFull: Boolean)
        fun onBack()
        fun onComplete()
    }

    private var player: BaseVideoView
    private var receiveGroup: ReceiverGroup

    init {
        val view = LayoutInflater.from(context).inflate(R.layout.layout_component_player, this, true)
        player = view.findViewById(R.id.base_video_view)
        receiveGroup = ReceiverGroup()
    }

    //--------绑定生命周期--------
    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun resumePlayer() {
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun pausePlayer() {
        controllerCover?.setPlayerBtnStatus(false)
        player.pause()
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun stopPlayer() {
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    fun destoryPlayer() {
        player.stop()
        player.stopPlayback()
    }

    //--------播放器的方法封装--------
    fun play(url: String) {
        val dataSource = DataSource(url)
        //-----添加播放器组件-----
//        layout_component_player.setOnPlayerEventListener { eventCode, bundle ->
//            //            println("--->>>代码： $eventCode   bundle: $bundle")
//        }
        receiveGroup.clearReceivers()
        controllerCover = ControllerCover(context, showDefinition)
        receiveGroup.addReceiver("loading_cover", LoadingCover(context))
        receiveGroup.addReceiver("controller_cover", controllerCover)
        player.setReceiverGroup(receiveGroup)
        player.setEventHandler(onVideoViewEventHandler)
        //-----添加播放器组件-----
        player.setDataSource(dataSource)
        player.start()
    }

    fun playNoController(url: String) {
        val dataSource = DataSource(url)
        player.setDataSource(dataSource)
        player.start()
    }

    fun setPlayerListener(listener: PlayerListener) {
        this.playerListener = listener
    }

    /**
     * 播放器数据通信
     * 默认播放暂停重播父类已实现，这里用于自定义的设置
     */
    private val onVideoViewEventHandler = object : OnVideoViewEventHandler() {
        override fun onAssistHandle(assist: BaseVideoView?, eventCode: Int, bundle: Bundle?) {
            super.onAssistHandle(assist, eventCode, bundle)
            when (eventCode) {
                DataInter.Event.EVENT_CODE_REQUEST_TOGGLE_SCREEN -> {
                    isFull = !isFull
                    playerListener?.onScreenChange(isFull)
                    receiveGroup.groupValue.putBoolean(DataInter.Key.KEY_IS_LANDSCAPE, isFull)
                }
                DataInter.Event.EVENT_MULTIPLE_PLAY -> {
                    bundle?.let { b ->
                        val speed = b.getFloat(DataInter.Key.KEY_MULTIPLE_PLAY, 1f)
                        player.setSpeed(speed)
                    }
                }
                DataInter.Event.EVENT_CODE_REQUEST_CLOSE -> {
                    playerListener?.onBack()
                }
                DataInter.Event.EVENT_PLAY_COMPLETE -> {
                    playerListener?.onComplete()
                }

            }
        }
    }

    /**
     * 播放器状态暴露
     */
    fun isFullPlay(): Boolean {
        return isFull
    }

    fun setFullPlay(isFull: Boolean) {
        this.isFull = isFull
        controllerCover?.setPlayerStatus(isFull)
    }

    fun setShowDefinition(boolean: Boolean) {
        this.showDefinition = boolean
    }
}