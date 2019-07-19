//package com.cqebd.live
//
//import android.Manifest
//import android.app.Activity
//import android.content.ComponentName
//import android.content.Context
//import android.content.Intent
//import android.content.pm.PackageInfo
//import android.graphics.Bitmap
//import android.graphics.BitmapFactory
//import android.graphics.PixelFormat
//import android.hardware.display.DisplayManager
//import android.media.Image
//import android.media.ImageReader
//import android.media.projection.MediaProjection
//import android.media.projection.MediaProjectionManager
//import android.os.Environment
//import android.util.Log
//import android.view.KeyEvent
//import android.view.View
//import androidx.lifecycle.Observer
//import androidx.lifecycle.ViewModelProviders
//import cn.alauncher.demo2.hralibrary.HRA_API
//import com.alibaba.android.arouter.facade.annotation.Autowired
//import com.alibaba.android.arouter.facade.annotation.Route
//import com.alibaba.android.arouter.launcher.ARouter
//import com.cqebd.live.databinding.ActivityMainBinding
//import com.cqebd.live.socketTool.KTool
//import com.cqebd.live.socketTool.MySocketClient
//import com.cqebd.live.socketTool.SocketTool
//import com.cqebd.live.ui.QRActivity
//import com.google.gson.Gson
//import com.google.gson.JsonParseException
//import com.jeremyliao.liveeventbus.LiveEventBus
//import com.king.zxing.Intents
//import com.open.net.client.impl.tcp.nio.NioClient
//import com.open.net.client.structures.BaseClient
//import com.open.net.client.structures.BaseMessageProcessor
//import com.open.net.client.structures.IConnectListener
//import com.open.net.client.structures.TcpAddress
//import com.open.net.client.structures.message.Message
//import com.orhanobut.logger.Logger
//import com.tbruyelle.rxpermissions2.RxPermissions
//import cqebd.student.BaseApp
//import cqebd.student.commandline.CacheKey
//import cqebd.student.commandline.Command
//import cqebd.student.tools.ByteTools
//import cqebd.student.viewmodel.ClassViewModel
//import cqebd.student.vo.CAnswerInfo
//import cqebd.student.vo.MyIntents
//import cqebd.student.vo.User
//import io.reactivex.Flowable
//import io.reactivex.schedulers.Schedulers
//import org.jetbrains.anko.toast
//import xiaofu.lib.base.activity.BaseBindActivity
//import xiaofu.lib.cache.ACache
//import xiaofu.lib.inline.loadUrl
//import xiaofu.lib.utils.base64
//import java.io.*
//import java.net.InetSocketAddress
//import java.net.Socket
//import java.nio.charset.Charset
//import java.util.*
//
//
//@Route(path = "/app/main")
//class MainActivity : BaseBindActivity<ActivityMainBinding>() {
//
//    @Autowired
//    @JvmField
//    var classing: Boolean = false
//
//    private lateinit var cache: ACache
//
//    private lateinit var viewModel: ClassViewModel
//
//    private val raceHandFrag by lazy { navigationAsFrag("/app/frag/race") }
//
//    private val observer = Observer<String> {
//        Log.e("xiaofu", it)
//        when (it) {
//            Command.EAGER_ANSWER_START -> {
//                binding.containerMain.visibility = View.VISIBLE
//
//                supportFragmentManager.beginTransaction()
//                    .replace(R.id.container_main, raceHandFrag)
//                    .commitAllowingStateLoss()
//            }
//            Command.EAGER_ANSWER_STOP -> {
//                binding.containerMain.visibility = View.GONE
//                supportFragmentManager.beginTransaction()
//                    .remove(raceHandFrag)
//                    .commitAllowingStateLoss()
//            }
//            Command.CLASS_END -> {
//                binding.tvHintStatus.visibility = View.GONE
//            }
//            Command.SHARE_DESKTOP -> {// 开始截图
//
//            }
//            Command.SHARE_DESK_STOP -> {// 停止
//
//            }
//
//
//        }
//    }
//
//    override fun isFullScreen(): Boolean = true
//    override fun isKeepScreenOn(): Boolean = true
//
//    override fun getLayoutRes(): Int = R.layout.activity_main
//
//    override fun initialize(binding: ActivityMainBinding) {
//        ARouter.getInstance().inject(this)
//        cache = ACache.get(this)
//
//        LiveEventBus.get()
//            .with(Command.COMMAND, String::class.java)
//            .observeForever(observer)
//
//        viewModel = ViewModelProviders.of(this, BaseApp.instance.factory).get(ClassViewModel::class.java)
//        viewModel.getTime().observe(this, Observer {
//            binding.stuIndexDatetime.text = it
//        })
//
//        mDisposablePool.add(viewModel.startTime())
//
//        binding.stuIndexDate.text = viewModel.getStringData()
//
//        readInfoBySD()
//
//        binding.tvHintStatus.visibility = if (classing) View.VISIBLE else View.GONE
//
//        val directory = File(Environment.getExternalStorageDirectory().absolutePath + "/my_capture")
//        if (!directory.exists()) {
//            directory.mkdirs()
//        }
//
//
//        screenShort()
//    }
//
//    override fun onResume() {
//        super.onResume()
//
//        // 初始化昵称和头像
//        val nickname: String? = cache.getAsString(CacheKey.KEY_NICK)
//        val avatar: String? = cache.getAsString(CacheKey.KEY_AVATAR)
//
//        if (nickname == null) {
//            val s = UUID.randomUUID().toString()
//            cache.put(CacheKey.KEY_NICK, s)
//            binding.stuIndexInfoName.text = s
//        } else {
//            binding.stuIndexInfoName.text = nickname
//        }
//        if (avatar == null) {
//            binding.stuIndexInfoPhoto.loadUrl(this, R.drawable.ic_student_index_photo)
//        } else {
//            binding.stuIndexInfoPhoto.loadUrl(this, avatar)
//        }
//    }
//
//    override fun bindListener(binding: ActivityMainBinding) {
//        binding.btnQrScan.setOnClickListener {
//
//            RxPermissions(this).request(Manifest.permission.CAMERA)
//                .subscribe {
//                    if (it) {
//                        val intent = Intent(this, QRActivity::class.java)
//                        startActivityForResult(intent, 333)
//                    } else {
//                        toast("你拒绝了使用权限，无法使用相机功能")
//                    }
//                }
//
//        }
//
//        binding.stuIndexBagBg.setOnClickListener {
//            //            startScreenShort()
////            testSend()
////            ARouter.getInstance()
////                    .build("/app/aty/remote")
////                    .navigation()
//
////            doStartApplicationWithPackageName("org.qimon.launcher6")
//        }
//
//        binding.stuIndexInfoBg.setOnClickListener {
//            navigation("/app/aty/user_info")
//        }
//
//        binding.stuIndexFileBg.setOnClickListener {
//            connet()
////            MySocketClient.get().connect("192.168.1.114", 2021, object : MySocketClient.Socket {
////                override fun connectSuccess(socketOs: OutputStream, socketIs: InputStream) {
////                    while (true) {
////                        Thread.sleep(1000)
////                        val s = String.format("%-16s", Command.SCREENS_REQUEST).toByteArray()
////                        socketOs.write(s)
////                        socketOs.flush()
////                        Log.w("Socket连接", "socket发送了一条文本")
////                    }
////
////                }
////            })
//        }
//
//    }
//
//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//
//        if (resultCode == Activity.RESULT_OK && data != null) {
//            if (requestCode == 333) {
//                val result = data.getStringExtra(Intents.Scan.RESULT) ?: return
//
//                val cache = ACache.get(this)
//                cache.put(CacheKey.IP_ADDRESS, result.substring(1))
////                cache.put(CacheKey.IP_ADDRESS, "192.168.1.109")
//                LiveEventBus.get()
//                    .with(Command.COMMAND, String::class.java)
//                    .post(Command.CONNECT_IP)
//                toast(result)
//            }
//
//            if (requestCode == requestScreenShortPermission) {
//                myPermission = true
//                sMediaProjection = mProjectionManager.getMediaProjection(resultCode, data)
//            }
//        }
//    }
//
//
//    // 启动第三方应用
//    private fun doStartApplicationWithPackageName(packagename: String) {
//
//        // 通过包名获取此APP详细信息，包括Activities、services、versioncode、name等等
//        var packageInfo: PackageInfo? = null
//        try {
//            packageInfo = packageManager.getPackageInfo(packagename, 0)
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
//
//        if (packageInfo == null) {
//            toast("您还未安装电子书包")
//            return
//        }
//
//        // 创建一个类别为CATEGORY_LAUNCHER的该包名的Intent
//        val resolveIntent = Intent(Intent.ACTION_MAIN, null)
//        resolveIntent.addCategory(Intent.CATEGORY_LAUNCHER)
//        resolveIntent.setPackage(packageInfo.packageName)
//
//        // 通过getPackageManager()的queryIntentActivities方法遍历
//        val resolveInfoList = packageManager
//            .queryIntentActivities(resolveIntent, 0)
//
//        val resolveInfo = resolveInfoList.iterator().next()
//        if (resolveInfo != null) {
//            // packageName = 参数packName
//            val packageName = resolveInfo.activityInfo.packageName
//            // 这个就是我们要找的该APP的LAUNCHER的Activity[组织形式：packageName.mainActivityName]
//            val className = resolveInfo.activityInfo.name
//            // LAUNCHER Intent
//            val intent = Intent(Intent.ACTION_MAIN)
//            intent.addCategory(Intent.CATEGORY_LAUNCHER)
//
//            // 设置ComponentName参数1:packageName参数2:MainActivity路径
//            val cn = ComponentName(packageName, className)
//
//            intent.component = cn
//            startActivity(intent)
//        }
//    }
//
//    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
//        if (keyCode == KeyEvent.KEYCODE_BACK && event?.action == KeyEvent.ACTION_DOWN) {
//            if (MyIntents.classStatus) {
//                toast("正在上课，请认真听讲")
//                return true
//            }
//        }
//        return super.onKeyDown(keyCode, event)
//    }
//
//    private fun sharedUser(data: String) {
//        val path = Environment.getExternalStorageDirectory().absolutePath.plus("/yunketang/shared")
//        val file = File(path)
//        if (!file.exists()) {
//            file.mkdirs()
//        }
//
//        val mFile = File(path, "user")
//        val outStream = FileOutputStream(mFile)
//        outStream.write(data.toByteArray())
//        outStream.close()
//    }
//
//
//    // 读取本地文件
//    private fun readInfoBySD() {
//        val task = RxPermissions(this).request(
//            Manifest.permission.WRITE_EXTERNAL_STORAGE,
//            Manifest.permission.READ_EXTERNAL_STORAGE
//        )
//            .subscribe {
//                if (it) {
//                    readUser()
//                } else {
//                    toast("你拒绝了使用权限")
//                }
//            }
//        mDisposablePool.add(task)
//    }
//
//    private fun readUser() {
//        val path = Environment.getExternalStorageDirectory().absolutePath.plus("/yunketang/shared/user")
//        val file = File(path)
//        if (file.exists()) {
//            try {
//                val inputStream = FileInputStream(file)
//                val inputReader = InputStreamReader(inputStream, "utf-8")
//                val bufferReader = BufferedReader(inputReader)
//                val strBuilder = StringBuilder()
//                while (bufferReader.readLine().apply {
//                        if (this != null) {
//                            strBuilder.append(this)
//                        }
//                    } != null) {
//
//                }
//                inputStream.close()
//                Log.d("xiaofu", strBuilder.toString())
//
////                val user: User = Gson().fromJson(strBuilder.toString(), User::class.java)
////                cache.put(CacheKey.KEY_ID, user.ID.toString())
//                cache.put(CacheKey.KEY_USER, strBuilder.toString())
//
//                try {
//                    val user: User = Gson().fromJson(strBuilder.toString(), User::class.java)
//                    cache.put(CacheKey.KEY_AVATAR, user.Avatar)
//                    cache.put(CacheKey.KEY_NICK, user.Name)
//                } catch (e: Exception) {
//
//                }
//
//
//            } catch (e: Exception) {
//                toast("请先打开点点课，进行登陆操作")
//            }
//        }
//
//    }
//
//
//    /**
//     * 本地抓屏
//     */
//    private lateinit var mProjectionManager: MediaProjectionManager
//    private lateinit var sMediaProjection: MediaProjection
//    private var count = 0
//    private val requestScreenShortPermission = 308
//    private var myPermission = false
//
//    private fun screenShort() {
//        mProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
//        startActivityForResult(mProjectionManager.createScreenCaptureIntent(), requestScreenShortPermission)
//    }
//
//    private fun startScreenShort() {
//        if (!myPermission) return
//        if (!::sMediaProjection.isInitialized) return
//
//        val metrics = resources.displayMetrics
//
//        val mImageReader = ImageReader.newInstance(
//            metrics.widthPixels,
//            metrics.heightPixels,
//            PixelFormat.RGB_565,
//            2
//        )
//
//        sMediaProjection.createVirtualDisplay(
//            "screen_short",
//            metrics.widthPixels,
//            metrics.heightPixels,
//            metrics.densityDpi,
//            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
//            mImageReader.surface,
//            null,
//            null
//        )
//
//        mImageReader.setOnImageAvailableListener({ reader ->
//            var image: Image? = null
//            var fos: FileOutputStream? = null
//            var bitmap: Bitmap? = null
//            try {
//                image = reader.acquireLatestImage()
//                if (image != null) {
//                    val planes = image.planes
//                    val buffer = planes[0].buffer
//                    val pixelStride = planes[0].pixelStride
//                    val rowStride = planes[0].rowStride
//                    val rowPadding = rowStride - pixelStride * metrics.widthPixels
//                    bitmap = Bitmap.createBitmap(
//                        metrics.widthPixels + rowPadding / pixelStride,
//                        metrics.heightPixels, Bitmap.Config.RGB_565
//                    )
//                    bitmap.copyPixelsFromBuffer(buffer)
//                    count++
//                    val fileName =
//                        Environment.getExternalStorageDirectory().absolutePath + "/my_capture/" + count + ".png"
//                    fos = FileOutputStream(fileName)
//                    bitmap.compress(Bitmap.CompressFormat.JPEG, 50, fos)
//                    fos.flush()
//                    val localPath = Environment.getExternalStorageDirectory().absolutePath + "/my_capture/1.png"
//                    val localBitmap = BitmapFactory.decodeFile(fileName)
//                    val imgBytes = SocketTool.Bitmap2Bytes(localBitmap)
//
//
//                    mDisposablePool.add(
//                        Flowable.just(1)
//                            .subscribeOn(Schedulers.io())
//                            .observeOn(Schedulers.io())
//                            .subscribe({
//
//                                os?.let {
//
//                                    val file = File(fileName)
//                                    fileInputOs = FileInputStream(file)
//                                    val fileSize = file.length()
//                                    val fileBuffer = ByteArray(4096)
//                                    var read = 0
//                                    var totalRead = 0
//                                    var remaining = fileSize.toInt()
//
//                                    Log.e("screen_capture", "文件大小：$fileSize")
//                                    it.write(KTool.getSendByte(fileSize.toInt(), count))
//                                    it.flush()
//                                    Log.e("screen_capture", "发送了指令")
//
//
//                                    while (totalRead < fileSize && fileInputOs!!.read(
//                                            fileBuffer,
//                                            0,
//                                            Math.min(fileBuffer.size, remaining)
//                                        ).apply {
//                                            read = this
//                                        } > 0
//                                    ) {
//                                        Log.e("screen_capture", "发送了一部分文件~~~~")
//                                        totalRead += read
//                                        remaining -= read
//                                        it.write(fileBuffer, 0, read)
//                                        it.flush()
//                                    }
//                                }
//                            }, {
//                                Log.e("screen_capture", "RX错误${it.message}")
//                            })
//                    )
//
//
////                    messageProcessor.send(mClient, KTool.getSendByte(imgBytes.size,count))
////                    messageProcessor.send(mClient, imgBytes)
////                    messageProcessor.send(mClient, imgBytes)
//                    Log.e("screen_capture", "保存成功：图片名称${fileName}.png,图片大小:${imgBytes.size}")
//
////                    mClient.disconnect()
////                    sMediaProjection.stop()
////                    if (count == 100){
////                        sMediaProjection.stop()
////                    }
//                }
//            } catch (e: Exception) {
//                Log.e("screen_capture", "发生错误，错误内容：${e.message}")
//            } finally {
//                if (fos != null) {
//                    try {
//                        fos.close()
//                    } catch (e: IOException) {
//                        e.printStackTrace()
//                    }
//                }
//                if (bitmap != null) {
//                    bitmap.recycle()
//                }
//                if (image != null) {
//                    image.close()
//                }
//            }
//
//        }, null)
//
//    }
//
//
//    private val messageProcessor = object : BaseMessageProcessor() {
//        override fun onReceiveMessages(mClient: BaseClient?, mQueen: LinkedList<Message>?) {
//
//        }
//    }
//
//    private lateinit var socket: Socket
//    private var os: OutputStream? = null
//    //    private var objectOs: ObjectOutputStream? = null
//    private var fileInputOs: FileInputStream? = null
//    private lateinit var mClient: NioClient// Socket客户端
//    private fun connet() {
//
//        Thread(Runnable {
//            val socketAddress = InetSocketAddress("192.168.1.124", 2022)
//            socket = Socket()
//            socket.connect(socketAddress, 3000)
//            os = socket.getOutputStream()
////            objectOs = ObjectOutputStream(os)
//            Log.e("screen_capture", "连接成功")
//
//            Log.e("screen_capture", "准备发送发送了指令")
//
//
//        }).start()
//
//
////        // 客户端初始化
////        mClient = NioClient(messageProcessor, object : IConnectListener {
////            override fun onConnectionSuccess() {
////                Log.e("send", "socket连接成功")
////            }
////
////            override fun onConnectionFailed() {
////                Log.e("send", "socket连接失败")
////            }
////        })
////
////        mClient.setConnectAddress(arrayOf(TcpAddress("192.168.1.124", 2021)))
////        mClient.setConnectAddress(arrayOf(TcpAddress("192.168.1.208", 2021)))
////        mClient.connect()
//    }
//
//    private fun testSend() {
//        val disposable = MySocketClient.get().connect("192.168.1.124", 2022, object : MySocketClient.Socket {
//            override fun connectSuccess(socketOs: OutputStream, socketIs: InputStream) {
//
//                for (i in 0..1000) {
//                    val localPath = Environment.getExternalStorageDirectory().absolutePath + "/my_capture/1.png"
//                    val file = File(localPath)
//                    fileInputOs = FileInputStream(file)
//                    val fileSize = file.length()
//                    val fileBuffer = ByteArray(4096)
//                    var read = 0
//                    var totalRead = 0
//                    var remaining = fileSize.toInt()
//
//                    while (totalRead < fileSize && fileInputOs!!.read(
//                            fileBuffer,
//                            0,
//                            Math.min(fileBuffer.size, remaining)
//                        ).apply {
//                            read = this
//                        } > 0
//                    ) {
//                        socketOs.write(KTool.getSendByteShareHot(fileSize.toInt(), i, read))
//                        socketOs.flush()
//                        totalRead += read
//                        remaining -= read
//                        socketOs.write(fileBuffer, 0, read)
//                        socketOs.flush()
//                    }
//                }
//            }
//        }) ?: return
//        mDisposablePool.add(disposable)
//    }
//
//}
