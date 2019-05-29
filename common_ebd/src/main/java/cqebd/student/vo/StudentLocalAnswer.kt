package cqebd.student.vo

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 学生本地数据库
 * Created by @author xiaofu on 2019/3/26.
 */
@Entity
data class StudentLocalAnswer(
        @PrimaryKey val taskId: Long,
        @ColumnInfo(name = "answerList") val answerList: String
)


@Entity
data class TaskFlag(
        @PrimaryKey val taskId: Long,
        @ColumnInfo(name = "taskFlag") val taskFlag: List<Boolean>
)