package cqebd.student.cache

import android.text.TextUtils
import com.google.gson.Gson
import cqebd.student.vo.User
import xiaofu.lib.cache.ACache
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 单例模式
 * 内存缓存以及SharePreference
 * Created by @author xiaofu on 2018/12/18.
 */
@Singleton
class EbdMemoryCache @Inject constructor(private val cache: xiaofu.lib.cache.ACache, private val gson: Gson) {

    /**
     * 用户首次登陆提供新手蒙层
     */
    fun isFirstUse(): Boolean {
        val key: String? = cache.getAsString("key-first-use")
        return TextUtils.isEmpty(key)
    }

    fun loginSuccess() {
        cache.put("key-first-use", "false")
    }


    // 快捷获取用户ID
    fun getId(): Int {
        return try {
            val user = gson.fromJson<User>(cache.getAsString(USER), User::class.java)
            user.ID
        } catch (e: Exception) {
            -1
        }
    }

    // 获取缓存的用户
    fun getUser(): User? {
        return try {
            val user = gson.fromJson<User>(cache.getAsString(USER), User::class.java)
            user
        } catch (e: Exception) {
            null
        }
    }

    fun getSubjectList(): List<User.Subject>? {
        return try {
            val user = gson.fromJson<User>(cache.getAsString(USER), User::class.java)
            val mSubjectList = ArrayList<User.Subject>()
            mSubjectList.add(User.Subject(-1, "全部", 0))
            mSubjectList.addAll(user.SubjectList)
            mSubjectList.filter { subject ->
                subject.Status == 0
            }
        } catch (e: Exception) {
            null
        }
    }

    fun saveUser(mUser: User) {
        cache.put(USER, gson.toJson(mUser))
    }

    fun clearUser() {
        cache.remove(USER)
    }

    fun removeKey(key: String) {
        cache.remove(key)
    }

    fun saveAccountAndPwd(account: String, password: String) {
        cache.put(ACCOUNT, account)
        cache.put(PASSWORD, password)
    }

    fun getAccount():String? = cache.getAsString(ACCOUNT)

    fun getPassword():String? = cache.getAsString(PASSWORD)

}