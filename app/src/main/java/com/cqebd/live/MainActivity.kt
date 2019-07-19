package com.cqebd.live

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Environment
import android.util.Log
import android.view.KeyEvent
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.cqebd.live.databinding.ActivityMainBinding
import com.cqebd.live.databinding.DialogDownloadLoadingBinding
import com.cqebd.live.service.FlyScreenService
import com.cqebd.live.socketTool.KTool
import com.cqebd.live.ui.QRActivity
import com.google.gson.Gson
import com.jeremyliao.liveeventbus.LiveEventBus
import com.king.zxing.Intents
import com.liulishuo.filedownloader.BaseDownloadTask
import com.liulishuo.filedownloader.FileDownloadListener
import com.liulishuo.filedownloader.FileDownloader
import com.qingmei2.rximagepicker.core.RxImagePicker
import com.qingmei2.rximagepicker_extension.MimeType
import com.qingmei2.rximagepicker_extension_zhihu.ZhihuConfigurationBuilder
import com.tbruyelle.rxpermissions2.RxPermissions
import cqebd.student.BaseApp
import cqebd.student.commandline.CacheKey
import cqebd.student.commandline.Command
import cqebd.student.viewmodel.ClassViewModel
import cqebd.student.vo.MyIntents
import cqebd.student.vo.User
import io.reactivex.Flowable
import io.reactivex.schedulers.Schedulers
import org.jetbrains.anko.toast
import xiaofu.lib.base.activity.BaseBindActivity
import xiaofu.lib.cache.ACache
import xiaofu.lib.inline.loadUrl
import xiaofu.lib.picture.FileHelper
import xiaofu.lib.picture.ZhihuImagePicker
import xiaofu.lib.view.dialog.FancyDialogFragment
import java.io.*
import java.net.*
import java.util.*


@Route(path = "/app/main")
class MainActivity : BaseBindActivity<ActivityMainBinding>() {

    @Autowired
    @JvmField
    var classing: Boolean = false

    private lateinit var cache: ACache

    private lateinit var flyIntent: Intent

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
//                connectShare()
                flyIntent = Intent(this, FlyScreenService::class.java)
                startService(flyIntent)
                screenShort()
            }
            Command.SHARE_DESK_STOP -> {// 停止
                Log.e("屏幕分享", "收到停止指令")
                if (::flyIntent.isInitialized) {
                    FlyScreenService.stopScreenShot()
                    stopService(flyIntent)
                }
//                if (::sMediaProjection.isInitialized) {
//                    Log.e("屏幕分享", "收到停止指令")
//                    sMediaProjection.stop()
//                }
            }
            else -> {
                try {
                    val commands = it.split(" ")
                    if (commands[0] == Command.SEND_FILE) {
                        receiveClassFile(commands[2])
                    } else if (commands[0] == Command.PRAISE) {
                        binding.stuIndexInfoGrade.text = "累计获赞:${commands[3]}分"
                    }
                } catch (e: Exception) {
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

        viewModel = ViewModelProviders.of(this, BaseApp.instance.factory).get(ClassViewModel::class.java)
        viewModel.getTime().observe(this, Observer {
            binding.stuIndexDatetime.text = it
        })

        mDisposablePool.add(viewModel.startTime())

        binding.stuIndexDate.text = viewModel.getStringData()

        readInfoBySD()

        binding.tvHintStatus.visibility = if (classing) View.VISIBLE else View.GONE

        val directory = File(KTool.getSDPath())
        if (!directory.exists()) {
            directory.mkdirs()
        }

        broadcastDiscover()// 自动发现IP

        val total = cache.getAsString(CacheKey.TOTAL_SUB)
        if (total != null) {
            binding.stuIndexInfoGrade.text = "累计获赞:${total}分"
        } else {
            binding.stuIndexInfoGrade.text = "你目前还没获得任何表扬"
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val bool = intent?.getBooleanExtra("classing", false) ?: return
        if (bool) {
            binding.tvHintStatus.visibility = if (bool) View.VISIBLE else View.GONE
        }
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
        binding.btnQrScan.setOnClickListener {
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

        binding.stuIndexBagBg.setOnClickListener {
            doStartApplicationWithPackageName("org.qimon.launcher6")

//            FlyScreenService.stopScreenShot()
        }

        binding.stuIndexInfoBg.setOnClickListener {
            navigation("/app/aty/remote_player")
        }

        binding.stuIndexFileBg.setOnClickListener {
            navigation("/app/aty/my_file")
//            screenShort()
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && data != null) {
            if (requestCode == 333) {
                val result = data.getStringExtra(Intents.Scan.RESULT) ?: return

                val cache = ACache.get(this)
                cache.put(CacheKey.IP_ADDRESS, result.substring(1))
                LiveEventBus.get()
                    .with(Command.COMMAND, String::class.java)
                    .post(Command.CONNECT_IP)
                toast(result)
            }

            if (requestCode == requestScreenShortPermission) {
                myPermission = true
                sMediaProjection = mProjectionManager.getMediaProjection(resultCode, data)
//                startScreenShort()

                val ip = cache.getAsString(CacheKey.IP_ADDRESS) ?: return
                FlyScreenService.connectServer(this, ip, sMediaProjection)
            }
        }
    }


    // 启动第三方应用
    private fun doStartApplicationWithPackageName(packagename: String) {

        // 通过包名获取此APP详细信息，包括Activities、services、versioncode、name等等
        var packageInfo: PackageInfo? = null
        try {
            packageInfo = packageManager.getPackageInfo(packagename, 0)
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

    private fun sharedUser(data: String) {
        val path = Environment.getExternalStorageDirectory().absolutePath.plus("/yunketang/shared")
        val file = File(path)
        if (!file.exists()) {
            file.mkdirs()
        }

        val mFile = File(path, "user")
        val outStream = FileOutputStream(mFile)
        outStream.write(data.toByteArray())
        outStream.close()
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
                Log.d("xiaofu", strBuilder.toString())

//                val user: User = Gson().fromJson(strBuilder.toString(), User::class.java)
//                cache.put(CacheKey.KEY_ID, user.ID.toString())
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

    private fun startScreenShort() {
        if (!myPermission) {
            toast("您拒绝了权限，无法截图")
            return
        }
        if (!::sMediaProjection.isInitialized) return

        mShareHotCount = 0
        val metrics = resources.displayMetrics

        mImageReader = ImageReader.newInstance(
            metrics.widthPixels,
            metrics.heightPixels,
            PixelFormat.RGBA_8888,
            2
        )
        mVirtualDisplay = sMediaProjection.createVirtualDisplay(
            "screen_short" + System.currentTimeMillis(),
            metrics.widthPixels,
            metrics.heightPixels,
            metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mImageReader!!.surface,
            null,
            null
        )

        mImageReader?.setOnImageAvailableListener({ reader ->
            Log.e("屏幕分享", "一直在走---")

            var image: Image? = null
            var fos: FileOutputStream? = null
            var bitmap: Bitmap? = null
            try {
                image = reader.acquireLatestImage()
                if (image != null) {
                    val planes = image.planes
                    val buffer = planes[0].buffer
                    val pixelStride = planes[0].pixelStride
                    val rowStride = planes[0].rowStride
                    val rowPadding = rowStride - pixelStride * metrics.widthPixels
                    bitmap = Bitmap.createBitmap(
                        metrics.widthPixels + rowPadding / pixelStride,
                        metrics.heightPixels, Bitmap.Config.ARGB_8888
                    )
                    Log.e("屏幕分享", "收到一张截图，待发送")

                    bitmap.copyPixelsFromBuffer(buffer)
                    val fileName = KTool.getScreenShortPath()
                    fos = FileOutputStream(fileName)
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 60, fos)
                    fos.flush()

                    val disposable = Flowable.just(1)
                        .subscribeOn(Schedulers.io())
                        .subscribe({
                            mSocketOs?.let { os ->
                                val file = File(fileName)
                                val fileInputOs = FileInputStream(file)
                                val fileSize = file.length().toInt()

                                var read = 0
                                var totalRead = 0
                                var remaining = fileSize
                                Log.e("屏幕分享", "文件大小：$fileSize")
                                while (totalRead < fileSize && fileInputOs.read(
                                        imgBuffer,
                                        0,
                                        Math.min(imgBuffer.size, remaining)
                                    ).apply {
                                        read = this
                                    } > 0
                                ) {
                                    Log.e("屏幕分享", "开始发送文件")
                                    os.write(KTool.getSendByteShareHot(fileSize, mShareHotCount, read))
                                    os.flush()
                                    os.write(imgBuffer, 0, read)
                                    os.flush()
                                    totalRead += read
                                    remaining -= read
                                }
                                Log.e("屏幕分享", "发送完毕~")
                                mShareHotCount++
                            }
                        }, {
                            Log.e("屏幕分享", "RX错误${it.message}")
                        }, {
                            if (fos != null) {
                                try {
                                    Log.e("屏幕分享", "fos被关闭了")
                                    fos.close()
                                } catch (e: IOException) {
                                    e.printStackTrace()
                                }
                            }
                        })
                }
            } catch (e: Exception) {
                Log.e("屏幕分享", "发生错误，错误内容：${e.message}")
            } finally {
//                if (fos != null) {
//                    try {
//                        Log.e("屏幕分享", "fos被关闭了")
//                        fos.close()
//                    } catch (e: IOException) {
//                        e.printStackTrace()
//                    }
//                }
                bitmap?.recycle()
                image?.close()
            }
        }, null)
    }

    private fun connectShare() {
        val ip = cache.getAsString(CacheKey.IP_ADDRESS) ?: return
        Thread(Runnable {
            try {
                val socketAddress = InetSocketAddress(ip, 2022)
                val socket = Socket()
                socket.connect(socketAddress, 3000)
                mSocketOs = socket.getOutputStream()
                mSocketIs = socket.getInputStream()

                runOnUiThread {
                    screenShort()
                }
            } catch (e: Exception) {

            }
        }).start()
    }

    private var mVirtualDisplay: VirtualDisplay? = null
    private var mImageReader: ImageReader? = null

    //--------------UDP组播
    private var udpThreadFlag = true

    private fun broadcastDiscover() {
        val disposable = Flowable.just(1)
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io())
            .subscribe({
                val host = "224.0.0.1"
                val ds = MulticastSocket(8003)
                val receiveAddress = InetAddress.getByName(host)
                ds.joinGroup(receiveAddress)
                val mBuffer = ByteArray(1024)
                val dp = DatagramPacket(mBuffer, 1024, receiveAddress, 8003)
                while (udpThreadFlag) {
                    try {
                        ds.receive(dp)
                        val command = String(mBuffer, 0, dp.length)
                        val commands = command.split(" ")
                        if (commands.size >= 3) {
                            val ip = commands[2]
                            cache.put(CacheKey.IP_ADDRESS_AUTO, ip)
                            udpThreadFlag = false
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }, {})
        mDisposablePool.add(disposable)
    }

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
                            fragBinding.downloadProgress.progress = ((soFarBytes / totalBytes.toFloat()) * 100).toInt()
                            val current = String.format("%.2fkb", soFarBytes / 1024.0)
                            val total = String.format("%.2fkb", totalBytes / 1024.0)
                            fragBinding.tvHintInfo.text = current.plus("/").plus(total)
                        }

                        override fun paused(task: BaseDownloadTask?, soFarBytes: Int, totalBytes: Int) {
                        }
                    })
                    .start()
            }
        progressFragment.show(supportFragmentManager, "download")
    }

}
