package cqebd.student.network

import androidx.lifecycle.LiveData
import cqebd.student.vo.BaseResponse
import cqebd.student.vo.CourseDetail
import cqebd.student.vo.CourseInfo
import cqebd.student.vo.VideoInfo
import io.reactivex.Flowable
import retrofit2.http.*
import xiaofu.lib.network.ApiResponse

/**
 *
 * Created by @author xiaofu on 2018/12/20.
 */
interface EbdVideoService {
    /**
     * 获取课程列表
     */
    @FormUrlEncoded
    @POST("api/CoursePeriod/GetCourseList")
    fun getCourseList(
            @Field("studentid") userId: Int,
            @Field("Type") type: Int = 2)
            : LiveData<xiaofu.lib.network.ApiResponse<BaseResponse<List<VideoInfo>>>>


    /**
     * 获取课程下子课时列表
     */
    @FormUrlEncoded
    @POST("api/CoursePeriod/GetPeriodList")
    fun getPeriodCourseList(
            @Field("CourseId") id: Int,
            @Field("studentid") userId: Int)
            : LiveData<xiaofu.lib.network.ApiResponse<BaseResponse<List<CourseInfo>>>>


    @GET("api/CoursePeriod/GetPeriodByID")
    fun getCourseDetailByID(
            @Query("id") id: Int,
            @Query("studentid") userId: Int)
            : LiveData<xiaofu.lib.network.ApiResponse<BaseResponse<CourseDetail>>>

    /**
     * 获取收藏列表
     */
    @GET("api/CoursePeriod/GetCollectByStudentId")
    fun getCollectList(
            @Query("studentid") studentId: Int)
            : LiveData<xiaofu.lib.network.ApiResponse<BaseResponse<List<VideoInfo>>>>

    /**
     *
     */
    @GET("api/Account/UpdateStudentPhoto")
    fun updateStudentAvatar(
            @Query("id") studentId: Int,
            @Query("photo") imgUrl: String)
            : Flowable<BaseResponse<Unit>>

}