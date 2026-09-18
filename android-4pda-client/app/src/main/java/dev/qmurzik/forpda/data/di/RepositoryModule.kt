package dev.qmurzik.forpda.data.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.qmurzik.forpda.data.repository.FakeForumRepository
import dev.qmurzik.forpda.data.repository.ForumRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    // TODO: swap for the real HTML/endpoint-backed implementation once
    // ForpdaHtmlParser's selectors are filled in against live markup.
    @Binds
    @Singleton
    abstract fun bindForumRepository(impl: FakeForumRepository): ForumRepository
}
