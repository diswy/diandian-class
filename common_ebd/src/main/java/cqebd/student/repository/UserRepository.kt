package cqebd.student.repository

import android.content.Context
import androidx.lifecycle.LiveData
import cqebd.student.cache.EbdMemoryCache
import cqebd.student.network.EbdVideoService
import cqebd.student.network.EbdWorkService
import cqebd.student.vo.*
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import okhttp3.MediaType
import okhttp3.RequestBody
import top.zibin.luban.Luban
import xiaofu.lib.network.ApiResponse
import xiaofu.lib.network.NetworkResource
import xiaofu.lib.network.Resource
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 *
 * Created by @author xiaofu on 2018/12/20.
 */
@Singleton
class UserRepository @Inject constructor(
        private val workService: EbdWorkService,
        private val videoService: EbdVideoService,
        private val memoryCache: EbdMemoryCache
) {

    /**
     * 是否是第一次登陆此应用
     */
    fun isFistUse() = memoryCache.isFirstUse()

    fun loginSuccess() = memoryCache.loginSuccess()

    fun getAccount(): String? = memoryCache.getAccount()

    fun getPassword(): String? = memoryCache.getPassword()

    fun saveAccountAndPwd(account: String, password: String) {
        memoryCache.saveAccountAndPwd(account, password)
    }

    /**
     * 用户登陆
     */
    fun getUser(account: String, password: String): LiveData<xiaofu.lib.network.Resource<BaseResponse<User>>> {
        return object : xiaofu.lib.network.NetworkResource<BaseResponse<User>>() {
            override fun saveResult(item: BaseResponse<User>) {
                if (item.isSuccess && item.data != null) {
                    memoryCache.saveUser(item.data)
                }
            }

            override fun createCall(): LiveData<xiaofu.lib.network.ApiResponse<BaseResponse<User>>> {
                return workService.userLogin(account, password)
            }
        }.asLiveData()
    }

    /**
     * 问题反馈
     */
    fun submitFeedBk(userId: Int, userName: String, title: String, content: String, classfiy: String, type: Int, sourceType: String): LiveData<xiaofu.lib.network.Resource<BaseResponse<Unit>>> {
        return object : xiaofu.lib.network.NetworkResource<BaseResponse<Unit>>() {
            override fun createCall(): LiveData<xiaofu.lib.network.ApiResponse<BaseResponse<Unit>>> {
                return workService.submitFeedBk(userId, userName, title, content, classfiy, type, sourceType)
            }
        }.asLiveData()
    }

    // 获取用户缓存
    fun getUserCache() = memoryCache.getUser()

    // 清除账号记录信息
    fun clearUserCache() {
        memoryCache.clearUser()
    }

    fun getUserId() = memoryCache.getId()

    fun getSubjectList() = memoryCache.getSubjectList()


    /**
     * 找回账号
     */
    fun findAccount(idCard: String): LiveData<xiaofu.lib.network.Resource<BaseResponse<FindAccount>>> {
        return object : xiaofu.lib.network.NetworkResource<BaseResponse<FindAccount>>() {
            override fun createCall(): LiveData<xiaofu.lib.network.ApiResponse<BaseResponse<FindAccount>>> {
                return workService.findAccount(idCard)
            }
        }.asLiveData()
    }

    /**
     * 修改密码
     */
    fun modifyPwd(pwd: String, newPwd: String, userId: Int): LiveData<xiaofu.lib.network.Resource<BaseResponse<Unit>>> {
        return object : xiaofu.lib.network.NetworkResource<BaseResponse<Unit>>() {
            override fun createCall(): LiveData<xiaofu.lib.network.ApiResponse<BaseResponse<Unit>>> {
                return workService.modifyPwd(pwd, newPwd, userId)
            }
        }.asLiveData()
    }

    /**
     * 获取消息列表
     */
    fun getMsgList(index: Int): LiveData<xiaofu.lib.network.Resource<BaseResponse<MessageData>>> {
        return object : xiaofu.lib.network.NetworkResource<BaseResponse<MessageData>>() {
            override fun createCall(): LiveData<xiaofu.lib.network.ApiResponse<BaseResponse<MessageData>>> {
                return workService.getMsgList(index, null, null, memoryCache.getId())
            }
        }.asLiveData()
    }

    /**
     * 消息阅读反馈
     */
    fun readMsg(type: Int, id: Int, studentId: Int): LiveData<xiaofu.lib.network.Resource<BaseResponse<Unit>>> {
        return object : xiaofu.lib.network.NetworkResource<BaseResponse<Unit>>() {
            override fun createCall(): LiveData<xiaofu.lib.network.ApiResponse<BaseResponse<Unit>>> {
                return workService.readMsg(type, id, studentId)
            }
        }.asLiveData()
    }

    /**
     * 重置密码之获取验证码
     */
    fun getPhoneCode(loginName: String, type: Int): LiveData<xiaofu.lib.network.Resource<BaseResponse<Unit>>> {
        return object : xiaofu.lib.network.NetworkResource<BaseResponse<Unit>>() {
            override fun createCall(): LiveData<xiaofu.lib.network.ApiResponse<BaseResponse<Unit>>> {
                return workService.getPhoneCode(loginName, type)
            }
        }.asLiveData()
    }

    /**
     * 重置密码之修改密码
     */
    fun updatePwd(loginName: String, newPwd: String, code: String): LiveData<xiaofu.lib.network.Resource<BaseResponse<Unit>>> {
        return object : xiaofu.lib.network.NetworkResource<BaseResponse<Unit>>() {
            override fun createCall(): LiveData<xiaofu.lib.network.ApiResponse<BaseResponse<Unit>>> {
                return workService.updatePwd(loginName, newPwd, code)
            }
        }.asLiveData()
    }

    /**
     * 修改手机号
     */
    fun updatePhCode(status: Int, code: String, tel: String, userId: Int, pwd: String? = null): LiveData<xiaofu.lib.network.Resource<BaseResponse<Unit>>> {
        return object : xiaofu.lib.network.NetworkResource<BaseResponse<Unit>>() {
            override fun createCall(): LiveData<xiaofu.lib.network.ApiResponse<BaseResponse<Unit>>> {
                return workService.updatePhCode(status, code, tel, userId, pwd)
            }
        }.asLiveData()
    }

    /**
     * 上传图片
     */
    fun uploadUserAvatar(context: Context, file: File): Flowable<BaseResponse<Unit>> {
        return Flowable.just(file)
                .observeOn(Schedulers.io())
                .map {
                    return@map Luban.with(context).load(it).get()[0]
                }
                .flatMap {
                    val requestBody = RequestBody.create(MediaType.parse("image/jpeg"), it)
                    return@flatMap workService.uploadFile(requestBody)
                }
                .flatMap {
                    it.data?.let { avatarUrl ->
                        val user = memoryCache.getUser()
                        if (user != null) {
                            user.Avatar = avatarUrl
                            memoryCache.saveUser(user)
                        }
                    }
                    return@flatMap videoService.updateStudentAvatar(memoryCache.getId(), it.data
                            ?: "")
                }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
    }


    fun updatePhone(phoneNumber: String) {
        val user = memoryCache.getUser()
        if (user != null) {
            user.Phone = phoneNumber
            memoryCache.saveUser(user)
        }
    }

    fun getSafflower(studentId: Int): LiveData<xiaofu.lib.network.Resource<BaseResponse<List<Safflower>>>> {
        return object : xiaofu.lib.network.NetworkResource<BaseResponse<List<Safflower>>>() {
            override fun createCall(): LiveData<xiaofu.lib.network.ApiResponse<BaseResponse<List<Safflower>>>> {
                return workService.getSafflower(studentId)
            }
        }.asLiveData()
    }

    fun getHonor(studentId: Int): LiveData<xiaofu.lib.network.Resource<BaseResponse<List<HonorInfo>>>> {
        return object : xiaofu.lib.network.NetworkResource<BaseResponse<List<HonorInfo>>>() {
            override fun createCall(): LiveData<xiaofu.lib.network.ApiResponse<BaseResponse<List<HonorInfo>>>> {
                return workService.getHonor(studentId)
            }
        }.asLiveData()
    }

}