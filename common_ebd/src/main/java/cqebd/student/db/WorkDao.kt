package cqebd.student.db

import androidx.lifecycle.LiveData
import androidx.room.*
import cqebd.student.vo.StudentLocalAnswer
import cqebd.student.vo.TaskFlag

/**
 *
 * Created by @author xiaofu on 2019/2/21.
 */
@Dao
interface WorkDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertStudentLocalAnswer(studentLocalAnswer: StudentLocalAnswer)

    @Update
    fun updateStudentLocalAnswer(studentLocalAnswer: StudentLocalAnswer)

    /**
     * 会持续监听数据库变化
     */
    @Query("SELECT * FROM StudentLocalAnswer WHERE taskId =:taskId")
    fun queryStudentLocalAnswerByTask(taskId: Long): LiveData<StudentLocalAnswer>

    /**
     * 仅加载数据库答案
     */
    @Query("SELECT * FROM StudentLocalAnswer WHERE taskId =:taskId")
    fun onlyLoadAnswer(taskId: Long): StudentLocalAnswer

    @Delete
    fun deleteStudentLocalAnswer(studentLocalAnswer: StudentLocalAnswer)

    @Query("DELETE FROM StudentLocalAnswer WHERE taskId =:taskId")
    fun deleteLocalAnswerById(taskId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertTaskFlag(taskFlag: TaskFlag)

    @Query("SELECT * FROM TaskFlag WHERE taskId =:taskId")
    fun onlyLoadTaskFlag(taskId: Long): TaskFlag

    @Update
    fun updateTaskFlag(taskFlag: TaskFlag)

    @Query("DELETE FROM TaskFlag WHERE taskId =:taskId")
    fun deleteTaskFlagById(taskId: Long)
}