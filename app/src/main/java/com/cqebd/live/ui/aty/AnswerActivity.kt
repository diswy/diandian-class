package com.cqebd.live.ui.aty

import android.Manifest
import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.MediaRecorder
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.widget.RadioGroup
import android.widget.SeekBar
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Autowired
import com.alibaba.android.arouter.facade.annotation.Route
import com.alibaba.android.arouter.launcher.ARouter
import com.cqebd.live.R
import com.cqebd.live.databinding.ActivityAnswerBinding
import com.cqebd.live.socketTool.KTool.getRecordPath
import com.cqebd.live.ui.adapter.BlankAdapter
import com.cqebd.live.ui.adapter.MultipleAdapter
import com.cqebd.live.ui.adapter.SingleChoiceAdapter
import com.cqebd.live.ui.adapter.TrueOrFalseAdapter
import com.example.zhouwei.library.CustomPopWindow
import com.google.gson.Gson
import com.jeremyliao.liveeventbus.LiveEventBus
import com.qingmei2.rximagepicker.core.RxImagePicker
import com.qingmei2.rximagepicker_extension.MimeType
import com.qingmei2.rximagepicker_extension_zhihu.ZhihuConfigurationBuilder
import com.tbruyelle.rxpermissions2.RxPermissions
import cqebd.student.commandline.CacheKey
import cqebd.student.commandline.Command
import cqebd.student.vo.CAnswerCommit
import cqebd.student.vo.CAnswerInfo
import cqebd.student.vo.CAnswerType
import cqebd.student.vo.MyIntents
import net.gotev.uploadservice.ServerResponse
import net.gotev.uploadservice.UploadInfo
import net.gotev.uploadservice.UploadStatusDelegate
import net.gotev.uploadservice.ftp.FTPUploadRequest
import org.jetbrains.anko.toast
import xiaofu.lib.base.activity.BaseBindActivity
import xiaofu.lib.cache.ACache
import xiaofu.lib.doodle.DoodleView
import xiaofu.lib.inline.loadUrl
import xiaofu.lib.picture.ZhihuImagePicker
import xiaofu.lib.utils.base64
import xiaofu.lib.utils.base64ToS
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


@Route(path = "/app/aty/answer")
class AnswerActivity : BaseBindActivity<ActivityAnswerBinding>() {

    @Autowired
    @JvmField
    var commands: String? = null

    private lateinit var singleAdapter: SingleChoiceAdapter
    private lateinit var multipleAdapter: MultipleAdapter
    private lateinit var trueOrFalseAdapter: TrueOrFalseAdapter
    private lateinit var blankAdapter: BlankAdapter
    private var currentType = -1
    private var userId = -1
    private lateinit var popPaintOptions: View

    private val observer = Observer<String> {
        val commands = it.split(" ")
        when (commands[0]) {
            Command.ANSWER_STOP -> {// 答题结束，如果学生此刻答案不为空的话可以帮助学生提交
                this.finish()
            }
        }
    }

    private val answerCommit = "%s %d %s"

    override fun isFullScreen(): Boolean = true
    override fun isKeepScreenOn(): Boolean = true

    override fun getLayoutRes(): Int = R.layout.activity_answer

    override fun initialize(binding: ActivityAnswerBinding) {
        ARouter.getInstance().inject(this)
        LiveEventBus.get()
            .with(Command.COMMAND, String::class.java)
            .observe(this, observer)

        initPopPaint()
        initMyRecordPath()

        val cache = ACache.get(this)
        val ids = cache.getAsString(CacheKey.KEY_ID)
        try {
            userId = Integer.parseInt(ids)
        } catch (e: Exception) {

        }

        binding.rv.layoutManager = LinearLayoutManager(this)

        commands?.let {
            Log.i("xiaofu", it)
            val mCommands = it.split(" ")

            try {
                Log.wtf("xiaofu", mCommands[2].base64ToS())
                val mAnswerInfo: CAnswerInfo = Gson().fromJson(mCommands[2].base64ToS(), CAnswerInfo::class.java)
                // 加载图片答题卡
                binding.ivQuestion.loadUrl(this, mAnswerInfo.QuestionDesc)

                val options = ArrayList<Int>()// 选项数
                for (i in 1..mAnswerInfo.QuestionCount) {
                    options.add(mAnswerInfo.QuestionOptions)
                }

                currentType = mAnswerInfo.QuestionType
                when (mAnswerInfo.QuestionType) {
                    CAnswerType.SINGLE.value -> {
                        binding.tvTitle.text = "单选题"
                        singleAdapter = SingleChoiceAdapter()
                        binding.rv.adapter = singleAdapter
                        singleAdapter.setNewData(options)
                    }
                    CAnswerType.MULTIPLE.value -> {
                        binding.tvTitle.text = "多选题"
                        multipleAdapter = MultipleAdapter()
                        binding.rv.adapter = multipleAdapter
                        multipleAdapter.setNewData(options)
                    }
                    CAnswerType.TRUE_OR_FALSE.value -> {
                        binding.tvTitle.text = "判断题"
                        trueOrFalseAdapter = TrueOrFalseAdapter()
                        binding.rv.adapter = trueOrFalseAdapter
                        trueOrFalseAdapter.setNewData(options)
                    }
                    CAnswerType.BLANK.value -> {
                        binding.tvTitle.text = "填空题"
                        blankAdapter = BlankAdapter()
                        binding.rv.adapter = blankAdapter
                        blankAdapter.setNewData(options)
                    }
                    CAnswerType.SUBJECTIVE.value -> {
                        binding.tvTitle.text = "主观题"
                        binding.llGroup.visibility = View.GONE
                        binding.llController.visibility = View.VISIBLE
                        binding.answerDoodleView.visibility = View.VISIBLE
                        initPaint()
                        requestRecord()
                    }
                    CAnswerType.PHOTO.value -> {
                        binding.tvTitle.text = "实拍题"
                        binding.llGroup.visibility = View.GONE
                        binding.llController.visibility = View.VISIBLE
                        binding.answerDoodleView.visibility = View.VISIBLE
                        initPaint()
                    }
                    else -> {
                        toast("没有对应类型的题目")
                    }
                }


            } catch (e: Exception) {
                Log.e("xiaofu", e.message)
                toast("数据解析失败")
            }

        }

    }

    override fun bindListener(binding: ActivityAnswerBinding) {
        binding.btnCommit.setOnClickListener {
            var content = ""
            when (currentType) {
                CAnswerType.SINGLE.value -> {
                    content = singleAdapter.getSingleAnswerPack()
                }
                CAnswerType.MULTIPLE.value -> {
                    content = multipleAdapter.getMultipleAnswerPack()
                }
                CAnswerType.TRUE_OR_FALSE.value -> {
                    content = trueOrFalseAdapter.getTrueOrFalseAnswerPack()
                }
                CAnswerType.BLANK.value -> {
                    content = blankAdapter.getBlankAnswerPack()
                }
                CAnswerType.SUBJECTIVE.value -> {

                }
            }

//            LiveEventBus.get()
//                .with(Command.COMMAND, String::class.java)
//                .post(answerCommit.format(Command.ANSWER_SUBMIT, content))
//            Logger.d(answerCommit.format(Command.ANSWER_SUBMIT, content).base64())

            LiveEventBus.get()
                .with(Command.COMMAND, String::class.java)
                .post(
                    answerCommit.format(
                        Command.ANSWER_SUBMIT,
                        userId,
                        content.base64().replace("[\\s*\t\n\r]".toRegex(), "")
                    )
                )

            toast("答案已提交")
            binding.btnCommit.visibility = View.GONE

        }

        binding.btnTakePhoto.setOnClickListener {
            RxPermissions(this).request(Manifest.permission.CAMERA)
                .subscribe { permission ->
                    if (permission) {
                        choosePic()
                    } else {
                        toast("您拒绝了拍照权限，无法使用相机")
                    }
                }
        }

        binding.btnCommitPic.setOnClickListener {
            binding.answerDoodleView.canDoodle(false)
            binding.btnCommitPic.isEnabled = false// 避免重复点击
            stopRecord()
            saveAndCommit()
        }

        binding.btnPaintClear.setOnClickListener {
            binding.answerDoodleView.reset()
        }

        binding.btnPaintBack.setOnClickListener {
            binding.answerDoodleView.back()
        }

        binding.btnChoosePaint.setOnClickListener {
            showPaintOptions()
        }
    }

    private fun getAnswer(name: String): String {
        val answers = ArrayList<CAnswerCommit>()
        answers.add(CAnswerCommit(1, getFTPPath(name)))
        return Gson().toJson(answers)
    }

    private fun getVideoAnswer(imgName: String, videoName: String): String {
        val answers = ArrayList<CAnswerCommit>()
        answers.add(CAnswerCommit(1, getFTPPath(imgName), getFTPPath(videoName)))
        return Gson().toJson(answers)
    }

    private fun getFTPPath(name: String): String {
        val cache = ACache.get(this)
        val ip = cache.getAsString(CacheKey.IP_ADDRESS) ?: return ""
        val date = Date(System.currentTimeMillis())
        val format = SimpleDateFormat("/yyyy/MM/dd/", Locale.CHINA)

        val httpPort = cache.getAsString(CacheKey.HTTP_PORT) ?: "27272"
        return "http://$ip:$httpPort/Record/${format.format(date)}$name"
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK && event?.action == KeyEvent.ACTION_DOWN) {
            if (MyIntents.classStatus) {
                toast("返回后将无法答题，请不要点击返回键")
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun choosePic() {
        val imagePicker = RxImagePicker.create(ZhihuImagePicker::class.java)
        val d = imagePicker.openGalleryAsDracula(
            this,
            ZhihuConfigurationBuilder(
                MimeType.ofImage(), false
            )
                .capture(true)
                .maxSelectable(1)
                .spanCount(4)
                .theme(xiaofu.lib.picture.R.style.Zhihu_Dracula)
                .build()
        )
            .subscribe {
                binding.ivQuestion.loadUrl(this@AnswerActivity, it.uri)
            }
        mDisposablePool.add(d)
    }


    /**
     * FTP上传文件
     */
    private fun ftpUpload(filePath: String, listener: OnFTPCompleteListener) {
        val cache = ACache.get(this)
        val ip = cache.getAsString(CacheKey.IP_ADDRESS) ?: return
        val fileName = filePath.substring(filePath.lastIndexOf("/"))
        val ftpPortS: String? = cache.getAsString(CacheKey.FTP_PORT)
        var ftpPort = 17171
        ftpPortS?.let {
            try {
                ftpPort = Integer.parseInt(it)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage("答案上传中，请稍后...")
        progressDialog.setCancelable(false)
        progressDialog.show()
        FTPUploadRequest(applicationContext, ip, ftpPort)
            .setUsernameAndPassword("ftpd", "password")
            .addFileToUpload(filePath, "/smartClass/Record/" + getFtpRemotePath() + fileName)
            .setMaxRetries(2)
            .setDelegate(object : UploadStatusDelegate {
                override fun onCancelled(context: Context?, uploadInfo: UploadInfo?) {
                    Log.w("ftp", "ftp 取消")
                    toast("答案上传被取消，请重新提交")
                    binding.btnCommitPic.isEnabled = true
                    progressDialog.dismiss()
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
                    toast("答案上传失败，请重新提交")
                    binding.btnCommitPic.isEnabled = true
                    progressDialog.dismiss()
                }

                override fun onCompleted(
                    context: Context?,
                    uploadInfo: UploadInfo?,
                    serverResponse: ServerResponse?
                ) {
                    listener.onComplete()
                    progressDialog.dismiss()
                }

            })
            .startUpload()
    }

    private fun getFtpRemotePath(): String {
        val date = Date(System.currentTimeMillis())
        val format = SimpleDateFormat("/yyyy/MM/dd/", Locale.CHINA)
        return format.format(date)
    }

    private interface OnFTPCompleteListener {
        fun onComplete()
    }

    /**
     * 初始化画笔
     */
    private fun initPaint() {
        binding.answerDoodleView.setSize(16)
        binding.answerDoodleView.setColor("#f13510")
    }

    /**
     * 弹窗方式弹出画笔设置
     */
    private fun initPopPaint() {
        popPaintOptions = LayoutInflater.from(this).inflate(R.layout.pop_paint_options, null)
        val rgPaintColor: RadioGroup = popPaintOptions.findViewById(R.id.rg_paint_color)
        val rgPaintShape: RadioGroup = popPaintOptions.findViewById(R.id.rg_paint_shape)
        val paintSeek: SeekBar = popPaintOptions.findViewById(R.id.paint_seek_bar)

        rgPaintColor.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rb_color_red -> binding.answerDoodleView.setColor("#F13510")
                R.id.rb_color_yellow -> binding.answerDoodleView.setColor("#FEBB4A")
                R.id.rb_color_green -> binding.answerDoodleView.setColor("#04B10A")
                R.id.rb_color_blue -> binding.answerDoodleView.setColor("#157BF7")
                R.id.rb_color_purple -> binding.answerDoodleView.setColor("#935CF8")
                R.id.rb_color_black -> binding.answerDoodleView.setColor("#000000")
            }
        }

        rgPaintShape.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rb_shape_pen -> binding.answerDoodleView.setType(DoodleView.ActionType.Path)
                R.id.rb_shape_line -> binding.answerDoodleView.setType(DoodleView.ActionType.Line)
                R.id.rb_shape_rect -> binding.answerDoodleView.setType(DoodleView.ActionType.Rect)
                R.id.rb_shape_circle -> binding.answerDoodleView.setType(DoodleView.ActionType.Circle)
            }
        }

        paintSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                binding.answerDoodleView.setSize(progress)
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }

        })
    }

    private fun showPaintOptions() {
        if (!::popPaintOptions.isInitialized) {
            initPopPaint()
        }
        val pop = CustomPopWindow.PopupWindowBuilder(this)
            .setView(popPaintOptions)
            .setOutsideTouchable(true)
            .create()
            .showAtLocation(binding.btnChoosePaint, Gravity.CENTER, 0, 100)
    }

    private fun saveAndCommit() {
        ftpSuccessCount = 0

        val mBitmap = Bitmap.createBitmap(binding.bgGroup.width, binding.bgGroup.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(mBitmap)
        binding.bgGroup.draw(canvas)
        val currentPath = binding.answerDoodleView.saveBitmap(binding.answerDoodleView, mBitmap)
        val fileName = currentPath.substring(currentPath.lastIndexOf("/"))
        ftpUpload(currentPath, object : OnFTPCompleteListener {
            override fun onComplete() {
                Log.d("ftp", "答案图片上传成功")
                ftpSuccessCount++
                if (recordMode) {
                    notifyServer(fileName)
                } else {
                    LiveEventBus.get()
                        .with(Command.COMMAND, String::class.java)
                        .post(
                            answerCommit.format(
                                Command.ANSWER_SUBMIT,
                                userId,
                                getAnswer(fileName).base64().replace("[\\s*\t\n\r]".toRegex(), "")
                            )
                        )

                    binding.llController.visibility = View.GONE
                    toast("答案已提交")
                }
            }
        })

        if (recordMode) {
            Log.e("xiaofu","---主观题>  录像地址：$mRecordMP4Path")
            ftpUpload(mRecordMP4Path, object : OnFTPCompleteListener {
                override fun onComplete() {
                    ftpSuccessCount++
                    notifyServer(fileName)
                }
            })
        }


    }

    private var ftpSuccessCount = 0
    private fun notifyServer(imgName: String) {
        if (ftpSuccessCount == 2) {
            val videoName = mRecordMP4Path.substring(mRecordMP4Path.lastIndexOf("/"))
            LiveEventBus.get()
                .with(Command.COMMAND, String::class.java)
                .post(
                    answerCommit.format(
                        Command.ANSWER_SUBMIT,
                        userId,
                        getVideoAnswer(imgName, videoName).base64().replace("[\\s*\t\n\r]".toRegex(), "")
                    )
                )

            binding.llController.visibility = View.GONE
            toast("答案已提交")
        }
    }

    /**
     * 录屏
     */
    private var recordMode = false
    private lateinit var mProjectionManager: MediaProjectionManager
    private lateinit var sMediaProjection: MediaProjection
    private lateinit var mVirtualDisplay: VirtualDisplay
    private val mediaRecorder by lazy { MediaRecorder() }
    private val requestScreenShortPermission = 300

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK
            && requestCode == requestScreenShortPermission
            && data != null
        ) {
            sMediaProjection = mProjectionManager.getMediaProjection(resultCode, data)
            sMediaProjection.registerCallback(object : MediaProjection.Callback() {}, null)
            startRecord()
        }
    }

    private fun requestRecord() {
        mProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(mProjectionManager.createScreenCaptureIntent(), requestScreenShortPermission)
    }

    private fun initRecorder() {
        val metrics = resources.displayMetrics
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.SURFACE)
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
        mediaRecorder.setOutputFile(mRecordMP4Path)
        mediaRecorder.setVideoSize(metrics.widthPixels, metrics.heightPixels)
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
//        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)// 需要音频权限，不设也挺好，节约空间
//        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
        mediaRecorder.setVideoEncodingBitRate(5 * 1024 * 1024)
        mediaRecorder.setVideoFrameRate(15)
        try {
            mediaRecorder.prepare()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun startRecord() {
        if (!::sMediaProjection.isInitialized) return

        Log.e("xiaofu","---主观题>")
        recordMode = true
        initRecorder()

        val metrics = resources.displayMetrics
        mVirtualDisplay = sMediaProjection.createVirtualDisplay(
            "screen_short" + System.currentTimeMillis(),
            metrics.widthPixels,
            metrics.heightPixels,
            metrics.densityDpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            mediaRecorder.surface,
            null,
            null
        )

        mediaRecorder.start()
    }

    private fun stopRecord() {
        if (recordMode) {
            mediaRecorder.stop()
            mediaRecorder.reset()
            if (::mVirtualDisplay.isInitialized) {
                mVirtualDisplay.release()
            }
            if (::sMediaProjection.isInitialized) {
                sMediaProjection.stop()
            }
        }
    }

    private var mRecordMP4Path = ""
    private fun initMyRecordPath() {
        val file = File(getRecordPath())
        if (!file.exists()) {
            if (!file.mkdirs()) {
                return
            }
        }
        mRecordMP4Path = getRecordPath() + UUID.randomUUID().toString() + ".mp4"
    }

}
