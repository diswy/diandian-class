package cqebd.student.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import cqebd.student.viewmodel.*
import dagger.Binds
import dagger.Module
import dagger.multibindings.IntoMap
import xiaofu.lib.network.di.ViewModelFactory
import xiaofu.lib.network.di.ViewModelKey

/**
 *
 * Created by @author xiaofu on 2018/12/20.
 */
@Suppress("unused")
@Module
abstract class EbdViewModelModule {

    @Binds
    @IntoMap
    @ViewModelKey(UserViewModel::class)
    abstract fun bindUserViewModel(userViewModel: UserViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(VideoViewModel::class)
    abstract fun bindVideoViewModel(videoViewModel: VideoViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(WorkViewModel::class)
    abstract fun bindWorkViewModel(workViewModel: WorkViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(AnswerCardViewModel::class)
    abstract fun bindToDoWorkViewModel(answerCardViewModel: AnswerCardViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(ClassViewModel::class)
    abstract fun bindToClassViewModel(classViewModel: ClassViewModel): ViewModel

    @Binds
    abstract fun bindViewModelFactory(factory: ViewModelFactory): ViewModelProvider.Factory
}