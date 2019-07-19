package xiaofu.lib.player.cover

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.view.MotionEvent
import android.view.View
import android.widget.*
import com.kk.taurus.playerbase.event.BundlePool
import com.kk.taurus.playerbase.event.EventKey
import com.kk.taurus.playerbase.event.OnPlayerEventListener
import com.kk.taurus.playerbase.player.OnTimerUpdateListener
import com.kk.taurus.playerbase.receiver.BaseCover
import com.kk.taurus.playerbase.receiver.IReceiverGroup
import com.kk.taurus.playerbase.touch.OnTouchGestureListener
import com.kk.taurus.playerbase.utils.TimeUtil
import xiaofu.component.player.R
import xiaofu.lib.player.play.DataInter

private const val MSG_CODE_DELAY_HIDDEN_CONTROLLER = 101
private const val MSG_CODE_SINGLE_TAP = 102

class ControllerCover constructor(context: Context, private val showDefinition: Boolean = true) : BaseCover(context), OnTimerUpdateListener, OnTouchGestureListener {
    private lateinit var btnPlay: CheckBox// 播放、暂停
    private lateinit var btnBack: ImageView// 返回
    private lateinit var tvTimes: TextView// 倍数播放
    private lateinit var tvDefinition: TextView// 清晰度
    private lateinit var tvCurrentTime: TextView// 当前时间
    private lateinit var tvAllTime: TextView// 总时间
    private lateinit var btnFull: ImageView// 全屏
    private lateinit var mSeekBar: SeekBar// 进度条
    private lateinit var allView: FrameLayout// 控制器
    private var mGestureEnable: Boolean = true // 手势能否使用
    private var animatorController: ObjectAnimator? = null// 淡入淡出动效
    private var isDoubleClick: Boolean = false// 是否触发了双击
    private var currentIsFull: Boolean = false// 当前是否是全屏


    private lateinit var timePlayLayout: LinearLayout
    private lateinit var hideTimePlay: View
    private lateinit var btnTimePlay05: TextView
    private lateinit var btnTimePlay075: TextView
    private lateinit var btnTimePlay10: TextView
    private lateinit var btnTimePlay125: TextView
    private lateinit var btnTimePlay15: TextView
    private lateinit var btnTimePlay175: TextView
    private lateinit var btnTimePlay20: TextView

    override fun onReceiverBind() {// 绑定控件
        super.onReceiverBind()

        // 控件初始化
        allView = view.findViewById(R.id.player_controller)
        btnBack = view.findViewById(R.id.player_iv_back)
        btnPlay = view.findViewById(R.id.player_cb_play_pause)
        tvTimes = view.findViewById(R.id.player_tv_time_play)
        tvDefinition = view.findViewById(R.id.player_tv_definition)
        tvCurrentTime = view.findViewById(R.id.player_tv_current_time)
        tvAllTime = view.findViewById(R.id.player_tv_all_time)
        btnFull = view.findViewById(R.id.player_iv_full)
        mSeekBar = view.findViewById(R.id.player_seek_bar)

        timePlayLayout = view.findViewById(R.id.player_time_play_layout)
        hideTimePlay = view.findViewById(R.id.player_hide_time_play)
        btnTimePlay05 = view.findViewById(R.id.player_time_play_05)
        btnTimePlay075 = view.findViewById(R.id.player_time_play_075)
        btnTimePlay10 = view.findViewById(R.id.player_time_play_10)
        btnTimePlay125 = view.findViewById(R.id.player_time_play_125)
        btnTimePlay15 = view.findViewById(R.id.player_time_play_15)
        btnTimePlay175 = view.findViewById(R.id.player_time_play_175)
        btnTimePlay20 = view.findViewById(R.id.player_time_play_20)

        // 事件,所有按钮执行的时候重新执行延迟隐藏控件的方法
        btnPlay.setOnCheckedChangeListener { _, isChecked ->
            sendDelayHiddenMessage()
            if (isChecked) {// false 触发暂停，true 触发播放
                requestResume(null)
            } else {
                requestPause(null)
            }
        }
        btnFull.setOnClickListener {
            sendDelayHiddenMessage()
            currentIsFull = true
            btnFull.visibility = View.GONE
            notifyReceiverEvent(DataInter.Event.EVENT_CODE_REQUEST_TOGGLE_SCREEN, null)
        }
        btnBack.setOnClickListener {
            sendDelayHiddenMessage()
            if (currentIsFull) {
                currentIsFull = false
                btnFull.visibility = View.VISIBLE
                notifyReceiverEvent(DataInter.Event.EVENT_CODE_REQUEST_TOGGLE_SCREEN, null)
            } else {
                notifyReceiverEvent(DataInter.Event.EVENT_CODE_REQUEST_CLOSE, null)
            }
        }
        tvTimes.setOnClickListener {
            sendDelayHiddenMessage()
            timePlayLayout.visibility = View.VISIBLE
        }
        hideTimePlay.setOnClickListener { timePlayLayout.visibility = View.GONE }
        // 倍数播放
        btnTimePlay05.setOnClickListener { timePlay(0.5f) }
        btnTimePlay075.setOnClickListener { timePlay(0.75f) }
        btnTimePlay10.setOnClickListener { timePlay(1.0f) }
        btnTimePlay125.setOnClickListener { timePlay(1.25f) }
        btnTimePlay15.setOnClickListener { timePlay(1.5f) }
        btnTimePlay175.setOnClickListener { timePlay(1.75f) }
        btnTimePlay20.setOnClickListener { timePlay(2.0f) }

        mSeekBar.setOnSeekBarChangeListener(onSeekBarChangeListener)
        groupValue.registerOnGroupValueUpdateListener(mOnGroupValueUpdateListener)

        if (!showDefinition) {
            tvDefinition.visibility = View.GONE
        }

    }

    /**
     * 倍数播放
     */
    private fun timePlay(timeValue: Float) {
        timePlayLayout.visibility = View.GONE
        val bundle = Bundle()
        bundle.putFloat(DataInter.Key.KEY_MULTIPLE_PLAY, timeValue)
        notifyReceiverEvent(DataInter.Event.EVENT_MULTIPLE_PLAY, bundle)
    }

    override fun onReceiverUnBind() {// 释放资源
        super.onReceiverUnBind()
        groupValue.unregisterOnGroupValueUpdateListener(mOnGroupValueUpdateListener)
    }

    override fun onCoverAttachedToWindow() {
        super.onCoverAttachedToWindow()
        allView.visibility = View.VISIBLE
    }

    override fun onCoverDetachedToWindow() {
        super.onCoverDetachedToWindow()
        removeDelayHiddenMessage()
    }

    override fun onPlayerEvent(eventCode: Int, bundle: Bundle?) {
        when (eventCode) {
            OnPlayerEventListener.PLAYER_EVENT_ON_BUFFERING_START,
            OnPlayerEventListener.PLAYER_EVENT_ON_DATA_SOURCE_SET,
            OnPlayerEventListener.PLAYER_EVENT_ON_PROVIDER_DATA_START,
            OnPlayerEventListener.PLAYER_EVENT_ON_SEEK_TO -> println("--->>>控制器日志：Loading开始")
            OnPlayerEventListener.PLAYER_EVENT_ON_VIDEO_RENDER_START -> println("--->>>控制器日志：开始播放了")
            OnPlayerEventListener.PLAYER_EVENT_ON_BUFFERING_END,
            OnPlayerEventListener.PLAYER_EVENT_ON_STOP,
            OnPlayerEventListener.PLAYER_EVENT_ON_START -> {
                btnPlay.isEnabled = true
                btnPlay.isChecked = true
            }
            OnPlayerEventListener.PLAYER_EVENT_ON_PROVIDER_DATA_ERROR,
            OnPlayerEventListener.PLAYER_EVENT_ON_SEEK_COMPLETE -> sendDelayHiddenMessage()
            OnPlayerEventListener.PLAYER_EVENT_ON_PLAY_COMPLETE -> {
                notifyReceiverEvent(DataInter.Event.EVENT_PLAY_COMPLETE, null)
                btnPlay.isChecked = false
            }
        }
    }

    override fun onReceiverEvent(eventCode: Int, bundle: Bundle?) {
    }

    override fun onErrorEvent(eventCode: Int, bundle: Bundle?) {

    }

    override fun onCreateCoverView(context: Context?): View {
        return View.inflate(context, R.layout.layout_controller_cover, null)
    }

    override fun getCoverLevel(): Int {
        return levelLow(1)
    }

    private fun setGestureEnable(gestureEnable: Boolean) {
        this.mGestureEnable = gestureEnable
    }

    /**
     * 监听
     */
    private val mOnGroupValueUpdateListener = object : IReceiverGroup.OnGroupValueUpdateListener {
        override fun filterKeys(): Array<String> {
            return arrayOf(DataInter.Key.KEY_COMPLETE_SHOW,
                    DataInter.Key.KEY_TIMER_UPDATE_ENABLE,
                    DataInter.Key.KEY_DATA_SOURCE,
                    DataInter.Key.KEY_IS_LANDSCAPE,
                    DataInter.Key.KEY_CONTROLLER_TOP_ENABLE)
        }

        override fun onValueUpdate(key: String, value: Any) {
            when (key) {
                DataInter.Key.KEY_COMPLETE_SHOW -> {
                }
                DataInter.Key.KEY_CONTROLLER_TOP_ENABLE -> {
                }
                DataInter.Key.KEY_IS_LANDSCAPE -> {
                }
                DataInter.Key.KEY_TIMER_UPDATE_ENABLE -> {
                }
                DataInter.Key.KEY_DATA_SOURCE -> {
                }
            }
        }
    }

    /**
     * 播放器用户滑动事件处理
     */
    private val onSeekBarChangeListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            if (fromUser)
                updateUI(progress, seekBar.max)
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {

        }

        override fun onStopTrackingTouch(seekBar: SeekBar) {
            sendSeekEvent(seekBar.progress)
        }
    }

    private fun sendSeekEvent(progress: Int) {
        mSeekProgress = progress
        mHandler.removeCallbacks(mSeekEventRunnable)
        mHandler.postDelayed(mSeekEventRunnable, 300)
    }

    private val mSeekEventRunnable = Runnable {
        if (mSeekProgress < 0)
            return@Runnable
        val bundle = BundlePool.obtain()
        bundle.putInt(EventKey.INT_DATA, mSeekProgress)
        requestSeek(bundle)
    }

    private lateinit var mTimeFormat: String
    private var mBufferPercentage: Int = 0
    private var mSeekProgress = -1

    override fun onTimerUpdate(curr: Int, duration: Int, bufferPercentage: Int) {
        if (!::mTimeFormat.isInitialized) {
            mTimeFormat = TimeUtil.getFormat(duration.toLong())
        }
        mBufferPercentage = bufferPercentage
        updateUI(curr, duration)
    }

    private fun updateUI(curr: Int, duration: Int) {
        mSeekBar.max = duration
        mSeekBar.progress = curr
        val mBuffer: Int = (mBufferPercentage * 1.0f / 100 * duration).toInt()
        mSeekBar.secondaryProgress = mBuffer
        tvCurrentTime.text = TimeUtil.getTime(mTimeFormat, curr.toLong())
        tvAllTime.text = TimeUtil.getTime(mTimeFormat, duration.toLong())
    }

    //--------控制器手势事件--------
    override fun onEndGesture() {

    }

    override fun onSingleTapUp(event: MotionEvent?) {
        mHandler.sendEmptyMessageDelayed(MSG_CODE_SINGLE_TAP, 300)
    }

    override fun onDown(event: MotionEvent?) {

    }

    override fun onDoubleTap(event: MotionEvent?) {
        isDoubleClick = true
        val b = btnPlay.isChecked
        btnPlay.isChecked = !b
    }

    override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float) {

    }

    //--------控制器延时隐藏--------
    private val mHandler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when (msg.what) {
                MSG_CODE_DELAY_HIDDEN_CONTROLLER -> {
                    setControllerState(false)
                }
                MSG_CODE_SINGLE_TAP -> {
                    if (isDoubleClick) {
                        isDoubleClick = false
                    } else {
                        toggleController()
                    }
                }
            }
        }
    }

    private fun sendDelayHiddenMessage() {
        removeDelayHiddenMessage()
        mHandler.sendEmptyMessageDelayed(MSG_CODE_DELAY_HIDDEN_CONTROLLER, 5000)
    }

    private fun removeDelayHiddenMessage() {
        mHandler.removeMessages(MSG_CODE_DELAY_HIDDEN_CONTROLLER)
    }

    //--------控制器状态、动效处理--------
    private fun isControllerShow(): Boolean {
        return allView.visibility == View.VISIBLE
    }

    private fun toggleController() {
        setControllerState(!isControllerShow())
    }

    private fun cancelAnimation() {
        animatorController?.cancel()
        animatorController?.removeAllListeners()
        animatorController?.removeAllUpdateListeners()
    }

    private fun setControllerState(state: Boolean) {
        if (state) {
            sendDelayHiddenMessage()
        } else {
            removeDelayHiddenMessage()
        }
        setControllerViewState(state)
    }

    private fun setControllerViewState(show: Boolean) {
        allView.clearAnimation()
        cancelAnimation()
        animatorController = ObjectAnimator.ofFloat(allView, "alpha",
                if (show) 0f else 1f, if (show) 1f else 0f).setDuration(500)
        addAnimatorListener(show)
    }

    private fun addAnimatorListener(show: Boolean) {
        animatorController?.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator?) {
                super.onAnimationStart(animation)
                if (show) {
                    allView.visibility = View.VISIBLE
                }
            }

            override fun onAnimationEnd(animation: Animator?) {
                super.onAnimationEnd(animation)
                if (!show) {
                    allView.visibility = View.GONE
                }
            }
        })
        animatorController?.start()
    }

    fun setPlayerStatus(isFull: Boolean) {
        btnFull.visibility = if (isFull) {
            View.GONE
        } else {
            View.VISIBLE
        }
    }

    fun setPlayerBtnStatus(isPlay: Boolean) {
        btnPlay.isChecked = isPlay
    }
}