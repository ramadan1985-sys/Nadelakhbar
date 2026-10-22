package com.example.data.repository

import com.example.data.local.ArticleDao
import com.example.data.local.ArticleEntity
import com.example.data.model.CountryItem
import com.example.data.model.NewsArticle
import com.example.data.remote.GeminiNewsService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NewsRepository(
    private val apiService: GeminiNewsService,
    private val articleDao: ArticleDao
) {

    val bookmarkedArticles: Flow<List<NewsArticle>> = articleDao.getBookmarkedArticles()
        .map { entities -> entities.map { it.toModel() } }

    val cachedArticles: Flow<List<NewsArticle>> = articleDao.getAllCachedArticles()
        .map { entities -> entities.map { it.toModel() } }

    suspend fun getTodayWires(category: String? = null, region: String? = null): List<NewsArticle> {
        val remoteArticles = apiService.fetchTodayWires(category, region)
        // Check bookmark status against database
        val syncedArticles = remoteArticles.map { article ->
            val isBookmarked = articleDao.isBookmarked(article.id) ?: false
            article.copy(isBookmarked = isBookmarked)
        }
        // Cache to Room
        val entities = syncedArticles.map { ArticleEntity.fromModel(it, it.isBookmarked) }
        articleDao.insertArticles(entities)
        return syncedArticles
    }

    suspend fun searchAdvanced(
        keywords: String,
        category: String,
        dateRange: String,
        region: String
    ): List<NewsArticle> {
        val remoteResults = apiService.searchWiresAdvanced(keywords, category, dateRange, region)
        val synced = remoteResults.map { article ->
            val isBookmarked = articleDao.isBookmarked(article.id) ?: false
            article.copy(isBookmarked = isBookmarked)
        }
        val entities = synced.map { ArticleEntity.fromModel(it, it.isBookmarked) }
        articleDao.insertArticles(entities)
        return synced
    }

    suspend fun getCountryNews(country: CountryItem): List<NewsArticle> {
        val remoteArticles = apiService.fetchCountryNews(country)
        val synced = remoteArticles.map { article ->
            val isBookmarked = articleDao.isBookmarked(article.id) ?: false
            article.copy(isBookmarked = isBookmarked)
        }
        val entities = synced.map { ArticleEntity.fromModel(it, it.isBookmarked) }
        articleDao.insertArticles(entities)
        return synced
    }

    suspend fun toggleBookmark(article: NewsArticle): Boolean {
        val newStatus = !article.isBookmarked
        val entity = ArticleEntity.fromModel(article, newStatus)
        articleDao.insertArticle(entity)
        articleDao.updateBookmarkStatus(article.id, newStatus)
        return newStatus
    }

    suspend fun askAgent(articleTitle: String, articleContent: String, question: String): String {
        return apiService.askAgentAboutArticle(articleTitle, articleContent, question)
    }
}
