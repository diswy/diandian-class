package cqebd.student.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import cqebd.student.vo.CourseInfo
import cqebd.student.vo.StudentLocalAnswer
import cqebd.student.vo.TaskFlag
import cqebd.student.vo.VideoInfo

/**
 *
 * Created by @author xiaofu on 2018/12/20.
 */
@Database(
        entities = [VideoInfo::class,
            CourseInfo::class,
            StudentLocalAnswer::class,
            TaskFlag::class],
        version = 1,
        exportSchema = false
)
@TypeConverters(Converter::class)
abstract class EbdDb : RoomDatabase() {
    abstract fun videoDao(): VideoDao
    abstract fun workDao(): WorkDao
}