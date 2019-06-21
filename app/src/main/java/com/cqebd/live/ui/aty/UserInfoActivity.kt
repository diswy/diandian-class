package com.cqebd.live.ui.aty

import android.Manifest
import android.app.Activity
import android.content.Intent
import com.alibaba.android.arouter.facade.annotation.Route
import com.cqebd.live.R
import com.cqebd.live.databinding.ActivityUserInfoBinding
import com.cqebd.live.databinding.LayoutModifyUserInfoBinding
import com.jeremyliao.liveeventbus.LiveEventBus
import com.tbruyelle.rxpermissions2.RxPermissions
import com.yalantis.ucrop.UCrop
import cqebd.student.commandline.CacheKey
import cqebd.student.commandline.Command
import org.jetbrains.anko.toast
import xiaofu.lib.base.activity.BaseBindActivity
import xiaofu.lib.cache.ACache
import xiaofu.lib.inline.loadUrl
import xiaofu.lib.picture.FileHelper
import xiaofu.lib.picture.singleImagePicker
import xiaofu.lib.tools.getVersionName
import xiaofu.lib.view.dialog.FancyDialogFragment
import java.util.*

@Route(path = "/app/aty/user_info")
class UserInfoActivity : BaseBindActivity<ActivityUserInfoBinding>() {

    private lateinit var cache: ACache

    private var nickName = ""
    private var avatar = ""

    override fun isFullScreen(): Boolean = true

    override fun getLayoutRes(): Int = R.layout.activity_user_info

    override fun initialize(binding: ActivityUserInfoBinding) {
        cache = ACache.get(this)

        binding.tvVersion.text = "当前版本：".plus(getVersionName(this))
    }

    override fun onResume() {
        super.onResume()
        // 初始化昵称和头像
        val nickname: String? = cache.getAsString(CacheKey.KEY_NICK)
        val avatar: String? = cache.getAsString(CacheKey.KEY_AVATAR)

        binding.tvNickName.text = nickname ?: UUID.randomUUID().toString()
        if (avatar == null) {
            binding.icUserAvatar.loadUrl(this, R.drawable.ic_student_index_photo)
        } else {
            binding.icUserAvatar.loadUrl(this, avatar)
        }
    }

    override fun bindListener(binding: ActivityUserInfoBinding) {
        binding.icUserAvatar.setOnClickListener {
            RxPermissions(this).request(Manifest.permission.CAMERA)
                    .subscribe { permission ->
                        if (permission) {
                            singleImagePicker()?.let {
                                mDisposablePool.add(it)
                            }
                        } else {
                            toast("您拒绝了拍照权限，无法使用相机")
                        }
                    }
        }

        binding.llNick.setOnClickListener {
            FancyDialogFragment.create()
                    .setLayoutRes(R.layout.layout_modify_user_info)
                    .setWidth(900)
                    .setViewListener { dialog, binding ->
                        binding as LayoutModifyUserInfoBinding
                        binding.btnCancel.setOnClickListener { dialog.dismiss() }
                        binding.btnConfirm.setOnClickListener {
                            nickName = binding.etNick.text.toString()
                            cache.put(CacheKey.KEY_NICK, nickName)
                            this@UserInfoActivity.binding.tvNickName.text = nickName
                            updateUser()
                            dialog.dismiss()
                        }
                    }
                    .show(supportFragmentManager, "nick")
        }
    }

    private fun updateUser() {
        val command = "%s %s %s %s"//1 命令 2 ID 3 名字 4 头像
        LiveEventBus.get()
                .with(Command.COMMAND, String::class.java)
                .post(command.format(Command.STUDENT_INFO_UPDATE, "1", nickName, avatar))
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK && data != null && requestCode == UCrop.REQUEST_CROP) {
            val path = FileHelper.getFileFromUri(UCrop.getOutput(data), this).absolutePath
            cache.put(CacheKey.KEY_AVATAR, path)
            avatar = "localPath//:".plus(path)// 添加标识符

            updateUser()
            toast("头像路径：${FileHelper.getFileFromUri(UCrop.getOutput(data), this).absolutePath}")
        } else if (resultCode == UCrop.RESULT_ERROR) {
            toast("请求错误：请重新拍照，或换张图片")
        }
    }

}
