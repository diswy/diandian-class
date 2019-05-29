package cqebd.student.network

import android.os.Build
import androidx.lifecycle.LiveData
import cqebd.student.vo.*
import io.reactivex.Flowable
import okhttp3.RequestBody
import retrofit2.http.*
import xiaofu.lib.network.ApiResponse

/**
 *
 * Created by @author xiaofu on 2018/12/20.
 */
interface EbdWorkService {

    /**
     * 用户登录
     */
    @GET("api/Account/Login")
    fun userLogin(
            @Query("loginName") loginName: String,
            @Query("pwd") pwd: String)
            : LiveData<xiaofu.lib.network.ApiResponse<BaseResponse<User>>>

    /**
     * 问题反馈
     */
    @POST("api/Feedback/SubmitFeedback")
    fun submitFeedBk(
            @Query("WriteUserId") WriteUserId: Int,
            @Query("WriteUserName") WriteUserName: String,
            @Query("Title") Title: String,
            @Query("Countent") Countent: String,
            @Query("Classify") Classify: String,
            @Query("Type") type: Int,
            @Query("SourceType") SourceType: String)
            : LiveData<xiaofu.lib.network.ApiResponse<BaseResponse<Unit>>>

    /**
     * 获取作业列表
     */
    @POST("api/Students/GetExaminationTasks")
    fun getWorkList(
            @Query("userid") loginId: Int,
            @Query("SubjectTypeID") SubjectTypeID: Int?,
            @Query("ExaminationPapersTypeID") ExaminationPapersTypeID: Int?,
            @Query("status") status: Int?,
            @Query("pageindex") pageIndex: Int,
            @Query("pagesieze") pageSize: Int = 20)
            : LiveData<xiaofu.lib.network.ApiResponse<BaseResponse<List<WorkInfo>>>>


    //****************答题****************
    /**
     * 上传图片
     */
    @Multipart
    @POST("http://service.student.cqebd.cn/HomeWork/UpdataFile")
    fun uploadFile(@Part("files\"; filename=\"image.jpg\"") files: RequestBody): Flowable<BaseResponse<String>>

    /**
     * 开始答题，需调用的接口
     */
    @GET("api/Students/StartWork")
    fun userStartWork(
            @Query("StudentQuestionsTasksID") taskId: Long)
            : Flowable<BaseResponse<StartWorkInfo>>

    /**
     * 获取试卷相关信息
     */
    @GET("api/Students/GetExaminationPapersByID")
    fun getExamPaper(
            @Query("id") paperId: Long,
            @Query("tasksid") tasksId: Long)
            : Flowable<BaseResponse<List<ExamPaper>>>

    @POST("api/Students/SubmitAnswer")
    fun submitAnswer(
            @Query("Stu_Id") Stu_Id: Int,
            @Query("QuestionId") QuestionId: Int,
            @Query("ExaminationPapersId") ExaminationPapersId: Int,
            @Query("StudentQuestionsTasksId") StudentQuestionsTasksId: Int,
            @Query("QuestionAnswerTypeId") QuestionAnswerTypeId: Int,
            @Query("Answer") Answer: String,
            @Query("Version") version: String,
            @Query("CreateDateTime") CreateDateTime: String,
            @Query("Source") source: String = Build.MODEL
    ): Flowable<BaseResponse<Unit>>


    @POST("api/Students/SubmitAnswerList")
    fun submitAnswers(
            @Query("Taskid") taskId: Long,
            @Query("AnswerList") answerList: String,
            @Query("Type") type: Int = 0,
            @Query("Status") Status: Int = 0
    ): Flowable<BaseResponse<Unit>>

    @GET("api/Students/EndWork")
    fun endWork(@Query("StudentQuestionsTasksID") taskId: Long): Flowable<BaseResponse<Int>>
    //****************答题****************

    /**
     * 分享作业列表
     */
    @POST("api/TaskShare/TaskShareToStudent")
    fun getShareHomeworkList(
            @Query("GradeId") grade: Int?,
            @Query("SubjectTypeid") subject: Int?,
            @Query("QuestionTypeId") question: Int?,
            @Query("Day") date: Int?,
            @Query("Studentid") studentId: Int,
            @Query("PageIndex") pageIndex: Int,
            @Query("PageSize") pageSize: Int = 20)
            : LiveData<xiaofu.lib.network.ApiResponse<BaseResponse<ShareHomework>>>

    /**
     * 获取错题本列表
     */
    @POST("api/Students/ErrorQuestionsList")
    @FormUrlEncoded
    fun getWrongQuestionList(
            @Field("userid") loginId: Int,
            @Field("SubjectTypeID") SubjectTypeID: Int?,
            @Field("ExaminationPapersTypeID") ExaminationPapersTypeID: Int?,
            @Field("status") status: Int?,
            @Field("pageindex") pageIndex: Int,
            @Field("pagesieze") pageSize: Int = 20)
            : LiveData<xiaofu.lib.network.ApiResponse<BaseResponse<List<WrongQuestion>>>>

    /**
     * 找回账号
     */
    @GET("api/Account/FindLoginName")
    fun findAccount(
            @Query("IdSerial") idCard: String)
            : LiveData<xiaofu.lib.network.ApiResponse<BaseResponse<FindAccount>>>

    /**
     * 修改密码
     */
    @POST("api/Account/EditPwd")
    fun modifyPwd(
            @Query("Pwd") Pwd: String,
            @Query("NewPwd") NewPwd: String,
            @Query("UserId") UserId: Int)
            : LiveData<xiaofu.lib.network.ApiResponse<BaseResponse<Unit>>>

    /**
     * 获取消息列表
     */
    @GET("/api/Account/GetMsgList")
    fun getMsgList(
            @Query("index") index: Int,
            @Query("day") day: Int? = null,
            @Query("status") status: Int? = null,
            @Query("studentid") studentid: Int)
            : LiveData<xiaofu.lib.network.ApiResponse<BaseResponse<MessageData>>>

    /**
     * 消息阅读反馈
     */
    @GET("/api/Account/ReadrMsg")
    fun readMsg(
            @Query("type") type: Int,
            @Query("id") id: Int,
            @Query("studentid") studentid: Int)
            : LiveData<xiaofu.lib.network.ApiResponse<BaseResponse<Unit>>>

    /**
     * 获取验证码
     */
    @GET("/api/Account/GetTelCode")
    fun getPhoneCode(
            @Query("loginName") loginName: String,
            @Query("type") type: Int)
            : LiveData<xiaofu.lib.network.ApiResponse<BaseResponse<Unit>>>

    /**
     * 重置密码
     */
    @POST("api/Account/UpdatePwdCode")
    fun updatePwd(
            @Query("LoginName") LoginName: String,
            @Query("NewPwd") NewPwd: String,
            @Query("Code") Code: String)
            : LiveData<xiaofu.lib.network.ApiResponse<BaseResponse<Unit>>>

    /**
     * 修改和绑定手机号
     */
    @POST("api/Account/UpdatePhCode")
    fun updatePhCode(
            @Query("Status") status: Int,
            @Query("Code") code: String,
            @Query("Tel") tel: String,
            @Query("UserId") userId: Int,
            @Query("Pwd") Pwd: String? = null)
            : LiveData<xiaofu.lib.network.ApiResponse<BaseResponse<Unit>>>

    /**
     * 个人荣誉榜
     */
    @GET("/api/Students/Honor")
    fun getSafflower(
            @Query("studentid") studentId: Int)
            : LiveData<xiaofu.lib.network.ApiResponse<BaseResponse<List<Safflower>>>>

    /**
     * 荣誉榜
     */
    @GET("/api/Students/TeamHonors")
    fun getHonor(
            @Query("studentid") studentId: Int)
            : LiveData<xiaofu.lib.network.ApiResponse<BaseResponse<List<HonorInfo>>>>

    /**
     * 错题本 问题详情
     */
    @GET("api/Students/ErrorQuestions")
    fun getErrorQuestions(
            @Query("StudentQuestionsTasksID") StudentQuestionsTasksID: Int)
            : LiveData<xiaofu.lib.network.ApiResponse<BaseResponse<WrongQuestionDetails>>>
}