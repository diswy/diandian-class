package cqebd.student.viewmodel

import android.content.Context
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import cqebd.student.repository.UserRepository
import cqebd.student.vo.BaseResponse
import cqebd.student.vo.MessageData
import cqebd.student.vo.User
import xiaofu.lib.network.Resource
import java.io.File
import javax.inject.Inject

/**
 *
 * Created by @author xiaofu on 2018/12/20.
 */
class UserViewModel @Inject constructor(private val userRepository: UserRepository) : ViewModel() {


    val messageList = MediatorLiveData<xiaofu.lib.network.Resource<BaseResponse<MessageData>>>()

    val liveUser = MutableLiveData<User>()

    fun login(account: String, password: String) = userRepository.getUser(account, password)

    fun saveAccountAndPwd(account: String, password: String) = userRepository.saveAccountAndPwd(account, password)

    fun getAccount(): String? = userRepository.getAccount()

    fun getPassword(): String? = userRepository.getPassword()
    /**
     * 获取学科
     */
    fun getSubjectList() = userRepository.getSubjectList()

    /**
     * 问题反馈
     */
    fun submitFeedBk(userId: Int, userName: String, title: String, content: String, classfiy: String, type: Int, sourceType: String) = userRepository.submitFeedBk(userId, userName, title, content, classfiy, type, sourceType)

    fun getUserCache() {
        liveUser.value = userRepository.getUserCache()
    }

    fun clearUserCache() {
        userRepository.clearUserCache()
    }

    fun getUserId() = userRepository.getUserId()

    fun getUserFromCache() = userRepository.getUserCache()

    fun findAccount(idCard: String) = userRepository.findAccount(idCard)

    fun modifyPwd(pwd: String, newPwd: String, userId: Int) = userRepository.modifyPwd(pwd, newPwd, userId)

    fun getMsgList(index: Int) {
        messageList.addSource(userRepository.getMsgList(index)) {
            messageList.removeSource(userRepository.getMsgList(index))
            messageList.value = it
        }
    }

    fun readMsg(type: Int, id: Int, studentId: Int) = userRepository.readMsg(type, id, studentId)

    fun getPhoneCode(loginName: String, type: Int) = userRepository.getPhoneCode(loginName, type)

    fun updatePwd(loginName: String, newPwd: String, code: String) = userRepository.updatePwd(loginName, newPwd, code)

    fun uploadImage(context: Context, file: File) = userRepository.uploadUserAvatar(context, file)

    fun updatePhCode(status: Int, code: String, tel: String, userId: Int, pwd: String? = null) = userRepository.updatePhCode(status, code, tel, userId, pwd)

    fun updatePhone(phoneNumber: String) = userRepository.updatePhone(phoneNumber)

    fun getSafflower(studentId: Int) = userRepository.getSafflower(studentId)

    fun getHonor(studentId: Int) = userRepository.getHonor(studentId)

    fun isFirstUse() = userRepository.isFistUse()

    fun loginSuccess() = userRepository.loginSuccess()
}