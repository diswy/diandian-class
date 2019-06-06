package cqebd.student.repository

import xiaofu.lib.AppExecutors
import javax.inject.Inject
import javax.inject.Singleton

/**
 *
 * Created by @author xiaofu on 2019/6/5.
 */
@Singleton
class ClassRepository @Inject constructor(
        private val appExecutors: AppExecutors
) {
}