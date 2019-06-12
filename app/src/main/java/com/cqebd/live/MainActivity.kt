package com.cqebd.live

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageInfo
import android.os.Environment
import android.util.Log
import android.view.KeyEvent
import android.view.View
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.alibaba.android.arouter.facade.annotation.Route
import com.cqebd.live.databinding.ActivityMainBinding
import com.cqebd.live.ui.QRActivity
import com.jeremyliao.liveeventbus.LiveEventBus
import com.king.zxing.Intents
import com.tbruyelle.rxpermissions2.RxPermissions
import cqebd.student.BaseApp
import cqebd.student.commandline.CacheKey
import cqebd.student.commandline.Command
import cqebd.student.viewmodel.ClassViewModel
import cqebd.student.vo.MyIntents
import org.jetbrains.anko.toast
import xiaofu.lib.base.activity.BaseBindActivity
import xiaofu.lib.cache.ACache
import java.io.*


@Route(path = "/app/main")
class MainActivity : BaseBindActivity<ActivityMainBinding>() {

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
            "123" -> {
                binding.containerMain.visibility = View.GONE

                supportFragmentManager.beginTransaction()
                        .remove(raceHandFrag)
                        .commitAllowingStateLoss()
            }
        }
    }

    override fun isFullScreen(): Boolean = true
    override fun isKeepScreenOn(): Boolean = true

    override fun getLayoutRes(): Int = R.layout.activity_main

    override fun initialize(binding: ActivityMainBinding) {
        LiveEventBus.get()
                .with(Command.COMMAND, String::class.java)
                .observeForever(observer)

        viewModel = ViewModelProviders.of(this, BaseApp.instance.factory).get(ClassViewModel::class.java)
        viewModel.getTime().observe(this, Observer {
            binding.stuIndexDatetime.text = it
        })

        mDisposablePool.add(viewModel.startTime())

        binding.stuIndexDate.text = viewModel.getStringData()
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

//            RxPermissions(this).request(
//                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
//                    Manifest.permission.READ_EXTERNAL_STORAGE
//            )
//                    .subscribe {
//                        if (it) {
////                        readUser()
//                        } else {
//                            toast("你拒绝了使用权限")
//                        }
//                    }

        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && data != null) {
            if (requestCode == 333) {
                val result = data.getStringExtra(Intents.Scan.RESULT) ?: return

                val cache = ACache.get(this)
//                cache.put(CacheKey.IP_ADDRESS, result)
                cache.put(CacheKey.IP_ADDRESS, "192.168.1.109")
                LiveEventBus.get()
                        .with(Command.COMMAND, String::class.java)
                        .post(Command.CONNECT_IP)
                toast(result)
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
            } catch (e: Exception) {

            }
        }
    }
}
