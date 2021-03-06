package cqebd.student.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log
import cn.alauncher.demo2.hralibrary.HRA_API
import com.alibaba.android.arouter.launcher.ARouter
import com.google.gson.Gson
import com.jeremyliao.liveeventbus.LiveEventBus
import com.open.net.client.impl.tcp.nio.NioClient
import com.open.net.client.structures.BaseClient
import com.open.net.client.structures.BaseMessageProcessor
import com.open.net.client.structures.IConnectListener
import com.open.net.client.structures.TcpAddress
import com.open.net.client.structures.message.Message
import com.orhanobut.logger.Logger
import cqebd.student.BaseApp
import cqebd.student.commandline.CacheKey
import cqebd.student.commandline.Command
import cqebd.student.vo.MyIntents
import cqebd.student.vo.User
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import net.gotev.uploadservice.ServerResponse
import net.gotev.uploadservice.UploadInfo
import net.gotev.uploadservice.UploadStatusDelegate
import net.gotev.uploadservice.ftp.FTPUploadRequest
import xiaofu.lib.cache.ACache
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class ClassService : Service() {

    private lateinit var cache: ACache
    private lateinit var user: User

    /**
     * 桥接学生发送指令到服务端
     */
    private val observer = androidx.lifecycle.Observer<String> {
        Log.i("xiaofu", "桥接命令：$it")
        val sCommand = it.split(" ")

        when (sCommand[0]) {
            Command.EAGER_ANSWER -> {
                if (::user.isInitialized) {
                    send(Command.EAGER_ANSWER.plus(" ${user.ID}"))// 这里空格+学生ID
                }
            }
            Command.CONNECT_IP -> {
                connectSocket()
            }
            Command.STUDENT_INFO_UPDATE -> {
                updateUserInfo(it)
            }
            Command.ANSWER_SUBMIT, Command.EAGER_PRAISE,
            Command.MOUSE_CLICK, Command.MOUSE_MOVE, Command.MOUSE_DOWN, Command.MOUSE_UP,
            Command.MOUSE_DOUBLE -> {
                send(it)
            }
            Command.ANSWER_PIC -> {
                uploadFTP(sCommand[2], sCommand[3])
            }
            Command.SCREENS_REQUEST -> {
                // 发送桥接命令
                Log.e("远程桌面", "发送命令：$it")
                send(it)
            }
        }
    }

    private lateinit var taskDisposable: Disposable

    private lateinit var mClient: NioClient// Socket客户端

    private val messageProcessor = object : BaseMessageProcessor() {
        override fun onReceiveMessages(mClient: BaseClient?, mQueen: LinkedList<Message>?) {
            mQueen?.forEach {
                Log.e("command", "偏移量：${it.offset},长度:${it.length}")
                val s = String(it.data, it.offset, it.length, Charset.forName("UTF-8"))
                Log.e("command", s)
                executeLine(s)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        Logger.d("--->>>课堂服务 onCreate")
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
                if (::taskDisposable.isInitialized) {// 连接成功后就不需要此线程了
                    taskDisposable.dispose()
                }

                val loginFormat = "%s %d %d %s %s %d"// 指令、ID、身份证号、名字、头像、教室编号
                if (::user.isInitialized) {
                    send(loginFormat.format(Command.LOGIN_ROOM, user.ID, 0 ,user.Name, user.Avatar, 1))
//                    send(loginFormat)
                }

                ARouter.getInstance()
                    .build("/app/main")
                    .withBoolean("classing", true)
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

                LiveEventBus.get().with(Command.COMMAND).post(Command.CLASS_END)
            }
        })

        connectSocket()
        // 心跳包去连接
        taskDisposable = Observable.interval(0, 3, TimeUnit.SECONDS)
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .subscribe {
                if (!mClient.isConnected) {// 未连接
                    val userString = cache.getAsString(CacheKey.KEY_USER) ?: return@subscribe
                    try {
                        user = Gson().fromJson(userString, User::class.java)

                        HRA_API.getHRA_API(applicationContext)
                            .setStatusBarDropEnable(applicationContext)
                        HRA_API.getHRA_API(applicationContext).setHomeVisible(applicationContext)
                        HRA_API.getHRA_API(applicationContext).setRecentVisible(applicationContext)
                        connectSocket()
                    } catch (e: Exception) {

                    }
                }
            }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Logger.d("--->>>课堂服务 onStartCommand")
        intent?.let {
            val key = it.getIntExtra("mode", -1)
            Logger.d("--->>>服务获取到的值: $key")
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        super.onDestroy()
        Logger.d("--->>>课堂服务 onDestroy")

        MyIntents.classStatus = false
        HRA_API.getHRA_API(applicationContext).setStatusBarDropEnable(applicationContext)
        HRA_API.getHRA_API(applicationContext).setHomeVisible(applicationContext)
        HRA_API.getHRA_API(applicationContext).setRecentVisible(applicationContext)

        if (::taskDisposable.isInitialized) {
            taskDisposable.dispose()
        }
        if (::mClient.isInitialized) {
            mClient.disconnect()
        }
//        LiveEventBus.get()
//                .with(Command.COMMAND, String::class.java)
//                .removeObserver(observer)
//
//        System.exit(0)
    }

    override fun onBind(intent: Intent): IBinder? {
        Logger.d("课堂Service，onBind")
        return null
    }

    /**
     * 处理命令服务端
     */
    private fun executeLine(line: String) {
        val mCommand = line.split(" ")
        mCommand.forEach {
            Log.w("xiaofu", it)
        }
        when (mCommand[0]) {
            Command.LOGIN_INFO_UPDATE -> {// 通知当前在线人数
                try {
                    val count = Integer.parseInt(mCommand[2])
//                    MyIntents.currentCount = count
                    BaseApp.instance.kRespository.setPeopleCout(count)
                    Log.e("xiaofu", "当前人数：" + BaseApp.instance.kRespository.getPeople())
                } catch (e: Exception) {

                }
            }
            Command.LOGIN_INFO_ADD -> {// 有人进入
                BaseApp.instance.kRespository.addPeople()
//                KMyIntent.currentCount++
            }
            Command.LOGIN_INFO_REMOVE -> {// 有人离开房间
                BaseApp.instance.kRespository.removePeople()
//                KMyIntent.currentCount--
            }
            Command.LOGIN_ROOM_RESULT -> {// 返回登陆结果

            }
            Command.LOCK_SCREEN -> {// 锁屏
                ARouter.getInstance()
                    .build("/app/aty/lockScreen")
                    .navigation()

                send("机器已锁屏")
            }
            Command.ANSWER_START -> {// 开始答题
                ARouter.getInstance()
                    .build("/app/aty/answer")
                    .withString("commands", line)
                    .navigation()
            }
            Command.UNLOCK_SCREEN, Command.EAGER_ANSWER_START, Command.EAGER_ANSWER_STOP,
            Command.BROADCAST_STOP, Command.SHARE_DESKTOP, Command.SHARE_DESK_STOP, Command.DEMON_STOP -> {// 解锁,抢答，抢答结束
                LiveEventBus.get().with(Command.COMMAND).post(mCommand[0])
            }
            Command.SHUTDOWN -> {
                HRA_API.getHRA_API(applicationContext).setStatusBarDropEnable(applicationContext)
                HRA_API.getHRA_API(applicationContext).setHomeVisible(applicationContext)
                HRA_API.getHRA_API(applicationContext).setRecentVisible(applicationContext)

                HRA_API.getHRA_API(applicationContext).setDeviceShutDown(applicationContext)
            }
            Command.BROADCAST -> {
                if (mCommand[2] == "1") {//TCP
                    ARouter.getInstance()
                        .build("/app/aty/remote_java")
                        .navigation()
                } else {// player
                    Log.d("xiaofu", "播放器地址：${mCommand[3]}")

                    ARouter.getInstance()
                        .build("/app/aty/remote_player")
                        .withString("playUrl", mCommand[3])
                        .navigation()
                }
            }
            Command.DEMON_START -> {
                ARouter.getInstance()
                    .build("/app/aty/remote_java")
                    .withBoolean("isControl", true)
                    .navigation()
            }
            Command.PRAISE -> {
                cache.put(CacheKey.TOTAL_SUB, mCommand[3])
                LiveEventBus.get().with(Command.COMMAND).post(line)
            }
            Command.SYSTEM_CONFIG -> {
                try {
                    cache.put(CacheKey.FTP_PORT, mCommand[2])
                    cache.put(CacheKey.HTTP_PORT, mCommand[3])
                    cache.put(CacheKey.REMOTE_URL, mCommand[4])
                } catch (e: Exception) {
                    e.printStackTrace()
                }
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
        Logger.d("----->>>发送命令：" + command)
        messageProcessor.send(mClient, command.plus(Command.END).toByteArray())
    }

    /**
     * 连接socket
     */
    private fun connectSocket() {
        mClient.disconnect()// 断开处理
        Log.e("xiaofu", "断开后连接 socket")
//        val ip = cache.getAsString(CacheKey.IP_ADDRESS)
        val ip = "47.96.178.14"
        if (ip != null) {
            Log.e("xiaofu", "连接 socket = $ip")
//            mClient.setConnectAddress(arrayOf(TcpAddress(ip, 2021)))
            mClient.setConnectAddress(arrayOf(TcpAddress(ip, 2051)))
            mClient.connect()
        }
    }

    /**
     * FTP 服务
     */
    private fun uploadFTP(filePath: String, name: String) {
        try {
            Log.w("ftp", "进入FTP，文件路径：$filePath")
            val cache = ACache.get(this)
            val ip = cache.getAsString(CacheKey.IP_ADDRESS) ?: return
            Log.w("ftp", "进入FTP，文件路径：$filePath")
            val ftpPortS: String? = cache.getAsString(CacheKey.FTP_PORT)
            var ftpPort = 17171
            ftpPortS?.let {
                try {
                    ftpPort = Integer.parseInt(it)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            FTPUploadRequest(applicationContext, ip, ftpPort)
                .setUsernameAndPassword("ftpd", "password")
                .addFileToUpload(
                    filePath,
                    "/smartClass/Record/" + getFtpRemotePath() + name + ".png"
                )
                .setMaxRetries(2)
                .setDelegate(object : UploadStatusDelegate {
                    override fun onCancelled(context: Context?, uploadInfo: UploadInfo?) {
                        Log.w("ftp", "ftp 取消")
                    }

                    override fun onProgress(context: Context?, uploadInfo: UploadInfo?) {
                        Log.w("ftp", "ftp onProgress")
                    }

                    override fun onError(
                        context: Context?,
                        uploadInfo: UploadInfo?,
                        serverResponse: ServerResponse?,
                        exception: java.lang.Exception?
                    ) {
                        Log.w("ftp", "ftp 错误:" + exception?.message)
                    }

                    override fun onCompleted(
                        context: Context?,
                        uploadInfo: UploadInfo?,
                        serverResponse: ServerResponse?
                    ) {
                        Log.w("ftp", "ftp 完成")
                    }

                })
                .startUpload()
        } catch (e: Exception) {

        }
    }

    private fun getFtpRemotePath(): String {
        val date = Date(System.currentTimeMillis())
        val format = SimpleDateFormat("/yyyy/MM/dd/", Locale.CHINA)
        return format.format(date)
    }


    //-----------指令
    private fun updateUserInfo(line: String) {
        val sCommand = line.split(" ")
        try {
            if (sCommand[3].contains("localPath//:")) {// 本地图片，需要调用FTP
                uploadFTP(sCommand[3].replace("localPath//:", ""), "aaa")
            } else {
                send(line)
            }
        } catch (e: Exception) {

        }
    }
}
