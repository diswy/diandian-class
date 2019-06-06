package com.cqebd.live.ui.frag


import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.alibaba.android.arouter.facade.annotation.Route
import com.cqebd.live.R
import com.cqebd.live.databinding.FragmentLockBinding
import cqebd.student.BaseApp
import cqebd.student.viewmodel.ClassViewModel
import xiaofu.lib.base.fragment.BaseBindFragment


/**
 * 锁屏
 *
 */
@Route(path = "/app/frag/lockScreen")
class LockFragment : BaseBindFragment<FragmentLockBinding>() {

    private lateinit var viewModel: ClassViewModel

    override fun getLayoutRes(): Int = R.layout.fragment_lock

    override fun initialize(activity: FragmentActivity, binding: FragmentLockBinding) {
        viewModel = ViewModelProviders.of(activity, BaseApp.instance.factory).get(ClassViewModel::class.java)

        viewModel.getTime().observe(this, Observer {
            Log.d("xiaofu", it)
        })
    }

}
