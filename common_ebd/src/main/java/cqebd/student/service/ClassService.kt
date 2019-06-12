package cqebd.student.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import cn.alauncher.demo2.hralibrary.HRA_API
import com.alibaba.android.arouter.launcher.ARouter
import com.jeremyliao.liveeventbus.LiveEventBus
import com.open.net.client.impl.tcp.nio.NioClient
import com.open.net.client.structures.BaseClient
import com.open.net.client.structures.BaseMessageProcessor
import com.open.net.client.structures.IConnectListener
import com.open.net.client.structures.TcpAddress
import com.open.net.client.structures.message.Message
import com.orhanobut.logger.Logger
import cqebd.student.commandline.CacheKey
import cqebd.student.commandline.Command
import cqebd.student.vo.MyIntents
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import xiaofu.lib.cache.ACache
import java.nio.charset.Charset
import java.util.*
import java.util.concurrent.TimeUnit

class ClassService : Service() {

    private lateinit var cache: ACache

    /**
     * 桥接学生发送指令到服务端
     */
    private val observer = androidx.lifecycle.Observer<String> {
        when (it) {
            Command.EAGER_ANSWER -> {
                send(Command.EAGER_ANSWER.plus(" 1"))// 这里空格+学生ID
            }
            Command.CONNECT_IP -> {
                connectSocket()
            }
        }
    }

    private lateinit var taskDisposable: Disposable

    private lateinit var mClient: NioClient// Socket客户端

    private val messageProcessor = object : BaseMessageProcessor() {
        override fun onReceiveMessages(mClient: BaseClient?, mQueen: LinkedList<Message>?) {
            mQueen?.forEach {
                val s = String(it.data, it.offset, it.length, Charset.forName("GBK"))

                executeLine(s)
            }
        }
    }


    override fun onCreate() {
        super.onCreate()
        Log.d("xiaofu", "课堂Service，onCreate")
        cache = ACache.get(applicationContext)
        // 程序内部通信
        LiveEventBus.get()
                .with(Command.COMMAND, String::class.java)
                .observeForever(observer)

        // 客户端初始化
        mClient = NioClient(messageProcessor, object : IConnectListener {
            override fun onConnectionSuccess() {
                Log.d("xiaofu", "socket连接成功")
                messageProcessor.send(mClient, "Socket连接成功\r\n".toByteArray())

                ARouter.getInstance()
                        .build("/app/main")
                        .navigation()

                MyIntents.classStatus = true
                HRA_API.getHRA_API(applicationContext).setStatusBarDropDisable(applicationContext)
                HRA_API.getHRA_API(applicationContext).setHomeGone(applicationContext)
                HRA_API.getHRA_API(applicationContext).setRecentGone(applicationContext)
            }

            override fun onConnectionFailed() {
                MyIntents.classStatus = false
                Log.d("xiaofu", "socket连接失败")
                HRA_API.getHRA_API(applicationContext).setStatusBarDropEnable(applicationContext)
                HRA_API.getHRA_API(applicationContext).setHomeVisible(applicationContext)
                HRA_API.getHRA_API(applicationContext).setRecentVisible(applicationContext)
            }
        })

        // 心跳包去连接
        taskDisposable = Observable.interval(0, 3, TimeUnit.SECONDS)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .subscribe {
                    Log.w("xiaofu", "延迟发送消息")
                    if (!mClient.isConnected) {// 未连接
                        HRA_API.getHRA_API(applicationContext).setStatusBarDropEnable(applicationContext)
                        HRA_API.getHRA_API(applicationContext).setHomeVisible(applicationContext)
                        HRA_API.getHRA_API(applicationContext).setRecentVisible(applicationContext)
                        connectSocket()
                    }
                }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Logger.d("课堂Service，onStartCommand")
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("xiaofu", "课堂Service，onDestroy")
        if (::taskDisposable.isInitialized) {
            taskDisposable.dispose()
        }
        if (::mClient.isInitialized) {
            mClient.disconnect()
        }
        LiveEventBus.get()
                .with(Command.COMMAND, String::class.java)
                .removeObserver(observer)
    }

    override fun onBind(intent: Intent): IBinder? {
        Logger.d("课堂Service，onBind")
        return null
    }

    /**
     * 处理命令
     */
    private fun executeLine(line: String) {
        val mCommand = line.split(" ")
        mCommand.forEach {
            Log.w("xiaofu", it)
        }
        when (mCommand[0]) {
            Command.LOCK_SCREEN -> {// 锁屏
                ARouter.getInstance()
                        .build("/app/aty/lockScreen")
                        .navigation()
            }
            Command.ANSWER_START -> {// 开始答题
                ARouter.getInstance()
                        .build("/app/aty/answer")
                        .navigation()
            }
            Command.UNLOCK_SCREEN, Command.EAGER_ANSWER_START, Command.EAGER_ANSWER_STOP -> {// 解锁,抢答，抢答结束
                LiveEventBus.get().with(Command.COMMAND).post(mCommand[0])
            }
            Command.SHUTDOWN -> {
                HRA_API.getHRA_API(applicationContext).setStatusBarDropEnable(applicationContext)
                HRA_API.getHRA_API(applicationContext).setHomeVisible(applicationContext)
                HRA_API.getHRA_API(applicationContext).setRecentVisible(applicationContext)

                HRA_API.getHRA_API(applicationContext).setDeviceShutDown(applicationContext)
            }
            else -> {
                Log.e("xiaofu", "转发了消息：$mCommand")
                LiveEventBus.get().with(Command.COMMAND).post(line)
            }
        }
    }

    /**
     * 封装消息
     */
    private fun send(command: String) {
        messageProcessor.send(mClient, command.plus(Command.END).toByteArray())
    }

    /**
     * 连接socket
     */
    private fun connectSocket() {
        val cache = ACache.get(this)
        val ip = cache.getAsString(CacheKey.IP_ADDRESS) ?: return

        mClient.setConnectAddress(arrayOf(TcpAddress(ip, 2020)))
        mClient.connect()
    }
}
