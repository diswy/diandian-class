package com.cqebd.live

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.graphics.PixelFormat
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.LinearLayout
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.cqebd.live.databinding.ActivityMainBinding
import com.cqebd.live.databinding.DialogClassingRoomBinding
import com.cqebd.live.databinding.DialogDownloadLoadingBinding
import com.cqebd.live.databinding.DialogSystemBinding
import com.cqebd.live.service.FlyScreenService
import com.cqebd.live.socketTool.KTool
import com.cqebd.live.ui.QRActivity
import com.cqebd.live.ui.adapter.DiscoverAdapter
import com.cqebd.live.vo.ClassingRoomInfo
import com.example.zhouwei.library.CustomPopWindow
import com.google.gson.Gson
import com.jeremyliao.liveeventbus.LiveEventBus
import com.king.zxing.Intents
import com.liulishuo.filedownloader.BaseDownloadTask
import com.liulishuo.filedownloader.FileDownloadListener
import com.liulishuo.filedownloader.FileDownloader
import com.tbruyelle.rxpermissions2.RxPermissions
import cqebd.student.BaseApp
import cqebd.student.commandline.CacheKey
import cqebd.student.commandline.Command
import cqebd.student.service.ClassService
import cqebd.student.viewmodel.ClassViewModel
import cqebd.student.vo.MyIntents
import cqebd.student.vo.User
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import org.jetbrains.anko.toast
import xiaofu.lib.base.activity.BaseBindActivity
import xiaofu.lib.cache.ACache
import xiaofu.lib.doodle.DoodleView
import xiaofu.lib.inline.loadUrl
import xiaofu.lib.view.dialog.FancyDialogFragment
import java.io.*
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket
import java.text.NumberFormat
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet


@Route(path = "/app/main")
class MainActivity : BaseBindActivity<ActivityMainBinding>() {

    @Autowired
    @JvmField
    var classing: Boolean = false

    private lateinit var cache: ACache
    private lateinit var flyIntent: Intent// 学生飞屏
    private lateinit var viewModel: ClassViewModel
    private val raceHandFrag by lazy { navigationAsFrag("/app/frag/race") }
    private val observer = Observer<String> {
        Log.e("xiaofu", it)
        when (it) {
            Command.EAGER_ANSWER_START -> {
                binding.containerMain.visibility = View.VISIBLE
                supportFragmentManager.beginTransaction()
                    .replace(R.id.container_main, raceHandFrag)
                    .commitAllowingStateLoss()
            }
            Command.EAGER_ANSWER_STOP -> {
                binding.containerMain.visibility = View.GONE
                supportFragmentManager.beginTransaction()
                    .remove(raceHandFrag)
                    .commitAllowingStateLoss()
            }
            Command.CLASS_END -> {
                binding.tvHintStatus.visibility = View.GONE
            }
            Command.SHARE_DESKTOP -> {// 开始截图
                Log.e("屏幕分享", "收到分享指令")
                flyIntent = Intent(this, FlyScreenService::class.java)
                startService(flyIntent)
                screenShort()
            }
            Command.SHARE_DESK_STOP -> {// 停止
                Log.e("屏幕分享", "收到停止指令")
                removeAllFloatView()
                if (::flyIntent.isInitialized) {
                    FlyScreenService.stopScreenShot()
                    stopService(flyIntent)
                }
            }
            else -> {
                try {
                    val commands = it.split(" ")
                    if (commands[0] == Command.SEND_FILE) {
                        receiveClassFile(commands[2])
                    } else if (commands[0] == Command.PRAISE) {
                        binding.stuIndexInfoGrade.text = "累计获赞:${commands[3]}分"
                    }
                } catch (e: Exception) {// 防止指令错误引发崩溃
                    e.printStackTrace()
                }
            }
        }
    }

    override fun isFullScreen(): Boolean = true
    override fun isKeepScreenOn(): Boolean = true

    override fun getLayoutRes(): Int = R.layout.activity_main

    override fun initialize(binding: ActivityMainBinding) {
        ARouter.getInstance().inject(this)
        cache = ACache.get(this)

        LiveEventBus.get()
            .with(Command.COMMAND, String::class.java)
            .observeForever(observer)

        initToolbar()

        // 初始化时间
        viewModel = ViewModelProviders.of(this, BaseApp.instance.factory).get(ClassViewModel::class.java)
        viewModel.getTime().observe(this, Observer {
            binding.stuIndexDatetime.text = it
        })
        mDisposablePool.add(viewModel.startTime())
        binding.stuIndexDate.text = viewModel.getStringData()
        // 读取共享的缓存，点点课的ID、姓名、头像
        readInfoBySD()


        val directory = File(KTool.getSDPath())
        if (!directory.exists()) {
            directory.mkdirs()
        }

        // 上课文字状态以及累计点赞UI更新
        binding.tvHintStatus.visibility = if (classing) View.VISIBLE else View.GONE
        val total = cache.getAsString(CacheKey.TOTAL_SUB)
        if (total != null) {
            binding.stuIndexInfoGrade.text = "累计获赞:${total}分"
        } else {
            binding.stuIndexInfoGrade.text = "加油，可以获得表扬哦"
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val bool = intent?.getBooleanExtra("classing", false) ?: return
        binding.tvHintStatus.visibility = if (bool) View.VISIBLE else View.GONE
    }

    override fun onResume() {
        super.onResume()

        // 初始化昵称和头像
        val nickname: String? = cache.getAsString(CacheKey.KEY_NICK)
        val avatar: String? = cache.getAsString(CacheKey.KEY_AVATAR)

        if (nickname == null) {
            val s = UUID.randomUUID().toString()
            cache.put(CacheKey.KEY_NICK, s)
            binding.stuIndexInfoName.text = s
        } else {
            binding.stuIndexInfoName.text = nickname
        }
        if (avatar == null) {
            binding.stuIndexInfoPhoto.loadUrl(this, R.drawable.ic_student_index_photo)
        } else {
            binding.stuIndexInfoPhoto.loadUrl(this, avatar)
        }
    }

    override fun bindListener(binding: ActivityMainBinding) {

        binding.btnSystem.setOnLongClickListener {
            FancyDialogFragment.create()
                .setLayoutRes(R.layout.dialog_system)
                .setWidth(400)
                .setViewListener { dialog, systemBinding ->
                    systemBinding as DialogSystemBinding
                    systemBinding.btnCancel.setOnClickListener {
                        dialog.dismiss()
                    }

                    systemBinding.btnConfirm.setOnClickListener {
                        if (systemBinding.etPwd.text.toString() == "123456") {
                            val i = Intent(this, ClassService::class.java)
                            stopService(i)
                            this.finish()
                        } else {
                            toast("密码错误，请认真上课")
                        }
                    }
                }
                .show(supportFragmentManager, "system")

            return@setOnLongClickListener true
        }

        binding.stuIndexBagBg.setOnClickListener {
            doStartApplicationWithPackageName("org.qimon.launcher6")
//            removeAllFloatView()
        }

        binding.stuIndexFileBg.setOnClickListener {
            navigation("/app/aty/my_file")
//            test()
//            navigation("/app/aty/lockScreen")
//            showDoodleView()
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && data != null) {
            if (requestCode == 333) {
                val result = data.getStringExtra(Intents.Scan.RESULT) ?: return

                cache.put(CacheKey.IP_ADDRESS, result.substring(1))
                LiveEventBus.get()
                    .with(Command.COMMAND, String::class.java)
                    .post(Command.CONNECT_IP)
            }

            if (requestCode == requestScreenShortPermission) {
                myPermission = true
                sMediaProjection = mProjectionManager.getMediaProjection(resultCode, data)

                val ip = cache.getAsString(CacheKey.IP_ADDRESS) ?: return
                FlyScreenService.connectServer(this, ip, sMediaProjection)
                showDoodleView()
            }
        }
    }

    // 初始化toolbar
    private fun initToolbar() {
        binding.mainToolbar.inflateMenu(R.menu.menu_main_settings)
        binding.mainToolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_qr_scan -> {
                    RxPermissions(this).request(Manifest.permission.CAMERA)
                        .subscribe {
                            if (it) {
                                val intent = Intent(this, QRActivity::class.java)
                                startActivityForResult(intent, 333)
                            } else {
                                toast("你拒绝了使用权限，无法使用相机功能")
                            }
                        }
                }
                R.id.menu_switch_room -> {
                    showClassingRoom()
                }
            }
            return@setOnMenuItemClickListener false
        }
    }

    // 启动第三方应用
    private fun doStartApplicationWithPackageName(pkgName: String) {

        // 通过包名获取此APP详细信息，包括Activities、services、versioncode、name等等
        var packageInfo: PackageInfo? = null
        try {
            packageInfo = packageManager.getPackageInfo(pkgName, 0)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (packageInfo == null) {
            toast("您还未安装电子书包")
            return
        }

        // 创建一个类别为CATEGORY_LAUNCHER的该包名的Intent
        val resolveIntent = Intent(Intent.ACTION_MAIN, null)
        resolveIntent.addCategory(Intent.CATEGORY_LAUNCHER)
        resolveIntent.setPackage(packageInfo.packageName)

        // 通过getPackageManager()的queryIntentActivities方法遍历
        val resolveInfoList = packageManager
            .queryIntentActivities(resolveIntent, 0)

        val resolveInfo = resolveInfoList.iterator().next()
        if (resolveInfo != null) {
            // packageName = 参数packName
            val packageName = resolveInfo.activityInfo.packageName
            // 这个就是我们要找的该APP的LAUNCHER的Activity[组织形式：packageName.mainActivityName]
            val className = resolveInfo.activityInfo.name
            // LAUNCHER Intent
            val intent = Intent(Intent.ACTION_MAIN)
            intent.addCategory(Intent.CATEGORY_LAUNCHER)

            // 设置ComponentName参数1:packageName参数2:MainActivity路径
            val cn = ComponentName(packageName, className)

            intent.component = cn
            startActivity(intent)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && event?.action == KeyEvent.ACTION_DOWN) {
            if (MyIntents.classStatus) {
                toast("正在上课，请认真听讲")
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    // 读取本地文件
    private fun readInfoBySD() {
        val task = RxPermissions(this).request(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
            .subscribe {
                if (it) {
                    readUser()
                } else {
                    toast("你拒绝了使用权限")
                }
            }
        mDisposablePool.add(task)
    }

    private fun readUser() {
        val path = Environment.getExternalStorageDirectory().absolutePath.plus("/yunketang/shared/user")
        val file = File(path)
        if (file.exists()) {
            try {
                val inputStream = FileInputStream(file)
                val inputReader = InputStreamReader(inputStream, "utf-8")
                val bufferReader = BufferedReader(inputReader)
                val strBuilder = StringBuilder()
                while (bufferReader.readLine().apply {
                        if (this != null) {
                            strBuilder.append(this)
                        }
                    } != null) {

                }
                inputStream.close()

                cache.put(CacheKey.KEY_USER, strBuilder.toString())

                try {
                    val user: User = Gson().fromJson(strBuilder.toString(), User::class.java)
                    cache.put(CacheKey.KEY_AVATAR, user.Avatar)
                    cache.put(CacheKey.KEY_NICK, user.Name)
                } catch (e: Exception) {

                }

            } catch (e: Exception) {
                toast("请先打开点点课，进行登陆操作")
            }
        }

    }

    /**
     * 本地抓屏
     */
    private lateinit var mProjectionManager: MediaProjectionManager
    private lateinit var sMediaProjection: MediaProjection
    private var mShareHotCount = 0// 发送截图的编号
    private val requestScreenShortPermission = 308
    private var myPermission = false

    private var mSocketOs: OutputStream? = null
    private var mSocketIs: InputStream? = null
    private val imgBuffer = ByteArray(4096)// 图片缓存区


    private fun screenShort() {
        Log.e("屏幕分享", "准备分享")
        mProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(mProjectionManager.createScreenCaptureIntent(), requestScreenShortPermission)
    }

//    private fun startScreenShort() {
//        if (!myPermission) {
//            toast("您拒绝了权限，无法截图")
//            return
//        }
//        if (!::sMediaProjection.isInitialized) return
//
//        mShareHotCount = 0
//        val metrics = resources.displayMetrics
//
//        mImageReader = ImageReader.newInstance(
//                metrics.widthPixels,
//                metrics.heightPixels,
//                PixelFormat.RGBA_8888,
//                2
//        )
//        mVirtualDisplay = sMediaProjection.createVirtualDisplay(
//                "screen_short" + System.currentTimeMillis(),
//                metrics.widthPixels,
//                metrics.heightPixels,
//                metrics.densityDpi,
//                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
//                mImageReader!!.surface,
//                null,
//                null
//        )
//
//        mImageReader?.setOnImageAvailableListener({ reader ->
//            Log.e("屏幕分享", "一直在走---")
//
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
//                            metrics.widthPixels + rowPadding / pixelStride,
//                            metrics.heightPixels, Bitmap.Config.ARGB_8888
//                    )
//                    Log.e("屏幕分享", "收到一张截图，待发送")
//
//                    bitmap.copyPixelsFromBuffer(buffer)
//                    val fileName = KTool.getScreenShortPath()
//                    fos = FileOutputStream(fileName)
//                    bitmap.compress(Bitmap.CompressFormat.JPEG, 60, fos)
//                    fos.flush()
//
//                    val disposable = Flowable.just(1)
//                            .subscribeOn(Schedulers.io())
//                            .subscribe({
//                                mSocketOs?.let { os ->
//                                    val file = File(fileName)
//                                    val fileInputOs = FileInputStream(file)
//                                    val fileSize = file.length().toInt()
//
//                                    var read = 0
//                                    var totalRead = 0
//                                    var remaining = fileSize
//                                    Log.e("屏幕分享", "文件大小：$fileSize")
//                                    while (totalRead < fileSize && fileInputOs.read(
//                                                    imgBuffer,
//                                                    0,
//                                                    Math.min(imgBuffer.size, remaining)
//                                            ).apply {
//                                                read = this
//                                            } > 0
//                                    ) {
//                                        Log.e("屏幕分享", "开始发送文件")
//                                        os.write(KTool.getSendByteShareHot(fileSize, mShareHotCount, read))
//                                        os.flush()
//                                        os.write(imgBuffer, 0, read)
//                                        os.flush()
//                                        totalRead += read
//                                        remaining -= read
//                                    }
//                                    Log.e("屏幕分享", "发送完毕~")
//                                    mShareHotCount++
//                                }
//                            }, {
//                                Log.e("屏幕分享", "RX错误${it.message}")
//                            }, {
//                                if (fos != null) {
//                                    try {
//                                        Log.e("屏幕分享", "fos被关闭了")
//                                        fos.close()
//                                    } catch (e: IOException) {
//                                        e.printStackTrace()
//                                    }
//                                }
//                            })
//                }
//            } catch (e: Exception) {
//                Log.e("屏幕分享", "发生错误，错误内容：${e.message}")
//            } finally {
//                bitmap?.recycle()
//                image?.close()
//            }
//        }, null)
//    }
//
//    private fun connectShare() {
//        val ip = cache.getAsString(CacheKey.IP_ADDRESS) ?: return
//        Thread(Runnable {
//            try {
//                val socketAddress = InetSocketAddress(ip, 2022)
//                val socket = Socket()
//                socket.connect(socketAddress, 3000)
//                mSocketOs = socket.getOutputStream()
//                mSocketIs = socket.getInputStream()
//
//                runOnUiThread {
//                    screenShort()
//                }
//            } catch (e: Exception) {
//
//            }
//        }).start()
//    }

    private var mVirtualDisplay: VirtualDisplay? = null
    private var mImageReader: ImageReader? = null

    // UDP组播技术，监听IP
    private val roomSet: HashSet<ClassingRoomInfo> = HashSet()
    private var udpDiscoverFlag = true

    private fun broadcastDiscover(adapter: DiscoverAdapter) {
        udpDiscoverFlag = true
        val disposable = Flowable.just(1)
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .subscribe({
                val host = "239.0.0.1"
                val ds = MulticastSocket(8002)
                ds.soTimeout = 5000
                val receiveAddress = InetAddress.getByName(host)
                ds.joinGroup(receiveAddress)
                val mBuffer = ByteArray(128)
                val dp = DatagramPacket(mBuffer, 128, receiveAddress, 8002)

                while (udpDiscoverFlag) {
                    try {
                        ds.receive(dp)
                        val command = String(mBuffer, 0, dp.length)
                        Log.d("xiaofu", "命令：$command")
                        val commands = command.split(" ")
                        if (commands.size >= 4) {
                            val room = ClassingRoomInfo(commands[2], commands[3])
                            roomSet.add(room)
                        }

                        val roomList = ArrayList<ClassingRoomInfo>()
                        roomSet.forEach {
                            roomList.add(it)
                        }
                        runOnUiThread {
                            adapter.setNewData(roomList)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Log.d("xiaofu", "错误：${e.message}")
                    }
                }
                ds.leaveGroup(receiveAddress)
                Log.d("xiaofu", "等待接收  UDP已退出")
            }, {})
        mDisposablePool.add(disposable)
    }

    private fun showClassingRoom() {
        val classingDialog = FancyDialogFragment
            .create()
            .setLayoutRes(R.layout.dialog_classing_room)
            .setWidth(730)
            .setDismissListener { udpDiscoverFlag = false }
            .setViewListener { dialog, classBinding ->
                val adapter = DiscoverAdapter(cache.getAsString(CacheKey.IP_ADDRESS))
                classBinding as DialogClassingRoomBinding
                classBinding.rvClassing.layoutManager = LinearLayoutManager(this)
                classBinding.rvClassing.adapter = adapter
                adapter.setEmptyView(R.layout.empty_class_room, classBinding.rvClassing)

                adapter.setOnItemClickListener { _, _, pos ->
                    val info = adapter.getItem(pos) ?: return@setOnItemClickListener
                    cache.put(CacheKey.IP_ADDRESS, info.ip)
                    LiveEventBus.get()
                        .with(Command.COMMAND, String::class.java)
                        .post(Command.CONNECT_IP)

                    dialog.dismiss()
                }
                roomSet.clear()
                broadcastDiscover(adapter)
            }
        classingDialog.show(supportFragmentManager, "classing")
    }

    // 接收文件
    private fun receiveClassFile(url: String) {
        val progressFragment = FancyDialogFragment
            .create()
            .setCanCancelOutside(false)
            .setLayoutRes(R.layout.dialog_download_loading)
            .setWidth(1000)
            .setViewListener { fancyDialog, fragBinding ->
                fragBinding as DialogDownloadLoadingBinding

                fragBinding.downloadProgress.max = 100

                val idx = url.lastIndexOf("/")
                val fileName = url.substring(idx + 1, url.length)
                Log.e("xiaofu", "下载地址：$url,文件名称：$fileName")
                FileDownloader.setup(this)
                FileDownloader.getImpl().create(url)
                    .setPath(KTool.getFilePath() + fileName)
                    .setListener(object : FileDownloadListener() {
                        override fun warn(task: BaseDownloadTask?) {
                        }

                        override fun completed(task: BaseDownloadTask?) {
                            toast("下载完成,请在我的文件中查看")
                            fancyDialog.dismiss()
                        }

                        override fun pending(task: BaseDownloadTask?, soFarBytes: Int, totalBytes: Int) {
                        }

                        override fun error(task: BaseDownloadTask?, e: Throwable?) {
                            toast("下载出错")
                            fancyDialog.dismiss()
                        }

                        override fun progress(task: BaseDownloadTask?, soFarBytes: Int, totalBytes: Int) {
                            val current = ((soFarBytes / totalBytes.toFloat()) * 100).toInt()

                            Log.e("xiaofu", "total=$totalBytes,sofar=$soFarBytes,比例=$current")
                            fragBinding.downloadProgress.progress = current
//                            val current = String.format("%.2fkb", soFarBytes / 1024.0)
//                            val total = String.format("%.2fkb", totalBytes / 1024.0)
                            fragBinding.tvHintInfo.text = current.toString().plus("%/100%")
                        }

                        override fun paused(task: BaseDownloadTask?, soFarBytes: Int, totalBytes: Int) {
                        }
                    })
                    .start()
            }
        progressFragment.show(supportFragmentManager, "download")
    }

    // 演示用画笔
    private fun showDoodleView() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {
                realShowDoodle()
            } else {
                val i = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                startActivity(i)
            }
        } else {
            realShowDoodle()
        }
    }

    private lateinit var floatView1: View
    private lateinit var floatView2: View
    private lateinit var mWindowManager: WindowManager
    private var isFloatShowing = false

    private fun removeAllFloatView() {
        isFloatShowing = false
        if (::mWindowManager.isInitialized) {
            try {
                mWindowManager.removeView(floatView1)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            try {
                mWindowManager.removeView(floatView2)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun realShowDoodle() {
        if (isFloatShowing) {
            return
        }
        mWindowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val wmParams = WindowManager.LayoutParams()
        wmParams.format = PixelFormat.RGBA_8888
        wmParams.gravity = Gravity.CENTER
        wmParams.packageName = packageName
        wmParams.type = WindowManager.LayoutParams.TYPE_PHONE
        wmParams.flags = (WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                or WindowManager.LayoutParams.FLAG_SCALED
                or WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR
                or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN)
        wmParams.width = 156
        wmParams.height = 156
        wmParams.x = Gravity.CENTER
        wmParams.y = Gravity.END

        // view 1
        floatView1 = LayoutInflater.from(this).inflate(R.layout.fragment_doodle_btn, null, false)
        // view 2
        floatView2 = LayoutInflater.from(this).inflate(R.layout.fragment_doodle, null, false)
        val mLL: LinearLayout = floatView2.findViewById(R.id.ll_controller)
        val mDoodle: DoodleView = floatView2.findViewById(R.id.float_doodle)
        val btnClosePaint: TextView = floatView2.findViewById(R.id.btn_exit)
        val btnChoose: TextView = floatView2.findViewById(R.id.btn_choose_paint)
        val btnBack: TextView = floatView2.findViewById(R.id.btn_paint_back)
        val btnClear: TextView = floatView2.findViewById(R.id.btn_paint_clear)

        mDoodle.setColor("#F13510")
        mDoodle.setSize(16)

        // 监听Listener
        floatView1.setOnClickListener {
            mWindowManager.removeView(floatView1)
            wmParams.width = 1920
            wmParams.height = 1080
            wmParams.x = 0
            wmParams.y = 0
            mWindowManager.addView(floatView2, wmParams)
        }
        // 画笔相关
        btnClosePaint.setOnClickListener {
            mWindowManager.removeView(floatView2)
            wmParams.width = 156
            wmParams.height = 156
            wmParams.x = Gravity.CENTER
            wmParams.y = Gravity.END
            mWindowManager.addView(floatView1, wmParams)
        }
        btnBack.setOnClickListener {
            mDoodle.back()
        }
        btnClear.setOnClickListener {
            mDoodle.reset()
        }
        btnChoose.setOnClickListener {
            showPaintOptions(mDoodle, mLL)
        }

        mWindowManager.addView(floatView1, wmParams)

        isFloatShowing = true
    }

    private lateinit var popPaintOptions: View

    /**
     * 弹窗方式弹出画笔设置
     */
    private fun initPopPaint(mDoodle: DoodleView) {
        popPaintOptions = LayoutInflater.from(this).inflate(R.layout.pop_paint_options, null)
        val rgPaintColor: RadioGroup = popPaintOptions.findViewById(R.id.rg_paint_color)
        val rgPaintShape: RadioGroup = popPaintOptions.findViewById(R.id.rg_paint_shape)
        val paintSeek: SeekBar = popPaintOptions.findViewById(R.id.paint_seek_bar)

        rgPaintColor.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rb_color_red -> mDoodle.setColor("#F13510")
                R.id.rb_color_yellow -> mDoodle.setColor("#FEBB4A")
                R.id.rb_color_green -> mDoodle.setColor("#04B10A")
                R.id.rb_color_blue -> mDoodle.setColor("#157BF7")
                R.id.rb_color_purple -> mDoodle.setColor("#935CF8")
                R.id.rb_color_black -> mDoodle.setColor("#000000")
            }
        }

        rgPaintShape.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rb_shape_pen -> mDoodle.setType(DoodleView.ActionType.Path)
                R.id.rb_shape_line -> mDoodle.setType(DoodleView.ActionType.Line)
                R.id.rb_shape_rect -> mDoodle.setType(DoodleView.ActionType.Rect)
                R.id.rb_shape_circle -> mDoodle.setType(DoodleView.ActionType.Circle)
            }
        }

        paintSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                mDoodle.setSize(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

        })
    }

    private fun showPaintOptions(mDoodle: DoodleView, ll: LinearLayout) {
        if (!::popPaintOptions.isInitialized) {
            initPopPaint(mDoodle)
        }
        val pop = CustomPopWindow.PopupWindowBuilder(this)
            .setView(popPaintOptions)
            .setOutsideTouchable(true)
            .create()
            .showAtLocation(ll, Gravity.CENTER, 0, 100)
    }

    //-----------------------------test
    private fun test() {
//        val s = "ANSWER_START 1419 eyJRdWVzdGlvblR5cGUiOjUsIlF1ZXN0aW9uRGVzYyI6Imh0dHA6Ly8xOTIuMTY4LjEuMTI4OjI3MjcyL1JlY29yZC8yMDE5LzA3LzI5LzIwMTkwNzI5MTYxNTEzMDk1NC1jdXQuanBnIiwiUXVlc3Rpb25Db3VudCI6MCwiUXVlc3Rpb25PcHRpb25zIjozLCJEb3duQ291bnRNaW51dGUiOjB9"
//        ARouter.getInstance()
//                .build("/app/aty/answer")
//                .withString("commands", s)
//                .navigation()

        val a = 124241
        val b = 346347
        val numberFormat = NumberFormat.getInstance()
        numberFormat.maximumFractionDigits = 8
        val c = a / b.toFloat()
        val d = numberFormat.format(c * 100)
        val e = (c * 100).toInt()
        Log.d("xiaofu", c.toString())
        Log.d("xiaofu", d)
        Log.d("xiaofu", e.toString())

    }

}
