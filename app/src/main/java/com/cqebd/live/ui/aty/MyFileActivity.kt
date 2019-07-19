package com.cqebd.live.ui.aty

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.alibaba.android.arouter.facade.annotation.Route
import com.cqebd.live.R
import com.cqebd.live.databinding.ActivityMyFileBinding
import com.cqebd.live.socketTool.KTool.getFilePath
import com.cqebd.live.ui.adapter.FileAdapter
import com.cqebd.live.utils.OpenFileUtil
import com.example.zhouwei.library.CustomPopWindow
import org.jetbrains.anko.toast
import xiaofu.lib.base.activity.BaseBindActivity
import xiaofu.lib.view.drawable.DrawableBuilder
import java.io.File

@Route(path = "/app/aty/my_file")
class MyFileActivity : BaseBindActivity<ActivityMyFileBinding>() {

    private lateinit var fileAdapter: FileAdapter

    override fun isFullScreen(): Boolean = true

    override fun getLayoutRes(): Int = R.layout.activity_my_file

    override fun initialize(binding: ActivityMyFileBinding) {

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        fileAdapter = FileAdapter()

        binding.fileRv.layoutManager = LinearLayoutManager(this)
        // 分割线初始化
        val divider = DividerItemDecoration(applicationContext, LinearLayoutManager.VERTICAL)
        val mDrawable = DrawableBuilder()
                .solidColor(ContextCompat.getColor(applicationContext, R.color.skeleton))
                .height(2)
                .build()
        divider.setDrawable(mDrawable)
        binding.fileRv.addItemDecoration(divider)
        binding.fileRv.adapter = fileAdapter


        val directory = File(getFilePath())
        if (!directory.exists()) {
            directory.mkdirs()
        }


        getFiles()
        fileAdapter.setOnItemClickListener { _, _, pos ->
            val file = fileAdapter.getItem(pos) ?: return@setOnItemClickListener
            OpenFileUtil.openFile(this, file)
        }

        fileAdapter.setOnItemLongClickListener { _, view, pos ->
            delFile(view, fileAdapter.getItem(pos), pos)
            return@setOnItemLongClickListener true
        }
    }

    private fun getFiles() {
        val f = File(getFilePath())
        if (!f.exists()) {
            return
        }
        val files = f.listFiles()
        val new = ArrayList<File>()
        files.forEach {
            new.add(it)
        }
        fileAdapter.setNewData(new)
    }

    private fun delFile(v: View, file: File?, pos: Int) {
        val popRoot = LayoutInflater.from(this).inflate(R.layout.pop_menu_item, null, false)
        val delBtn: TextView = popRoot.findViewById(R.id.btn_item_del)
        val pop = CustomPopWindow.PopupWindowBuilder(this)
                .setView(popRoot)
                .setOutsideTouchable(true)
                .create()
                .showAtLocation(binding.root,Gravity.CENTER,0,0)

        delBtn.setOnClickListener {
            if (file?.delete() == true) {
                toast("文件刪除成功")
                fileAdapter.remove(pos)
            } else {
                toast("文件刪除失败，请重新尝试")
            }
            pop.dissmiss()
        }
    }
}
