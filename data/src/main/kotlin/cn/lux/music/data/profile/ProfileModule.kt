package cn.lux.music.data.profile

import cn.lux.music.api.ProfileSession
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class ProfileModule {
    @Binds abstract fun session(implementation: ProfileStore): ProfileSession
}
