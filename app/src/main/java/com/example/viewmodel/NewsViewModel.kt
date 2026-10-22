package com.example.viewmodel

import android.app.Application
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.CountryItem
import com.example.data.model.DateRangeFilter
import com.example.data.model.NewsArticle
import com.example.data.model.NewsCategory
import com.example.data.model.SupportedCountries
import com.example.data.model.WorldRegion
import com.example.data.remote.GeminiNewsService
import com.example.data.repository.NewsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale

enum class ScreenDestination {
    HOME,
    ADVANCED_SEARCH,
    COUNTRIES,
    ARTICLE_READER,
    BOOKMARKS,
    AGENCIES
}

data class SearchFilterState(
    val keywords: String = "",
    val category: NewsCategory = NewsCategory.ALL,
    val dateRange: DateRangeFilter = DateRangeFilter.TODAY,
    val region: WorldRegion = WorldRegion.ALL
)

class NewsViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {

    private val repository: NewsRepository
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false

    private val _currentScreen = MutableStateFlow(ScreenDestination.HOME)
    val currentScreen: StateFlow<ScreenDestination> = _currentScreen.asStateFlow()

    private val _screenHistory = mutableListOf<ScreenDestination>()

    private val _todayArticles = MutableStateFlow<List<NewsArticle>>(emptyList())
    val todayArticles: StateFlow<List<NewsArticle>> = _todayArticles.asStateFlow()

    private val _isLoadingFeed = MutableStateFlow(false)
    val isLoadingFeed: StateFlow<Boolean> = _isLoadingFeed.asStateFlow()

    private val _selectedCategory = MutableStateFlow(NewsCategory.ALL)
    val selectedCategory: StateFlow<NewsCategory> = _selectedCategory.asStateFlow()

    private val _selectedRegion = MutableStateFlow(WorldRegion.ALL)
    val selectedRegion: StateFlow<WorldRegion> = _selectedRegion.asStateFlow()

    // Advanced Search State
    private val _searchFilters = MutableStateFlow(SearchFilterState())
    val searchFilters: StateFlow<SearchFilterState> = _searchFilters.asStateFlow()

    private val _searchResults = MutableStateFlow<List<NewsArticle>>(emptyList())
    val searchResults: StateFlow<List<NewsArticle>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _hasPerformedSearch = MutableStateFlow(false)
    val hasPerformedSearch: StateFlow<Boolean> = _hasPerformedSearch.asStateFlow()

    // Countries News State
    private val _selectedCountry = MutableStateFlow<CountryItem?>(SupportedCountries.list.first())
    val selectedCountry: StateFlow<CountryItem?> = _selectedCountry.asStateFlow()

    private val _countryArticles = MutableStateFlow<List<NewsArticle>>(emptyList())
    val countryArticles: StateFlow<List<NewsArticle>> = _countryArticles.asStateFlow()

    private val _isLoadingCountryNews = MutableStateFlow(false)
    val isLoadingCountryNews: StateFlow<Boolean> = _isLoadingCountryNews.asStateFlow()

    // Article Reader State
    private val _currentArticle = MutableStateFlow<NewsArticle?>(null)
    val currentArticle: StateFlow<NewsArticle?> = _currentArticle.asStateFlow()

    private val _readerFontSizeMultiplier = MutableStateFlow(1.0f)
    val readerFontSizeMultiplier: StateFlow<Float> = _readerFontSizeMultiplier.asStateFlow()

    private val _isTtsSpeaking = MutableStateFlow(false)
    val isTtsSpeaking: StateFlow<Boolean> = _isTtsSpeaking.asStateFlow()

    // Reader Agent Q&A
    private val _agentAnswer = MutableStateFlow<String?>(null)
    val agentAnswer: StateFlow<String?> = _agentAnswer.asStateFlow()

    private val _isAgentAnswering = MutableStateFlow(false)
    val isAgentAnswering: StateFlow<Boolean> = _isAgentAnswering.asStateFlow()

    // Bookmarked articles flow from Room
    val bookmarkedArticles: StateFlow<List<NewsArticle>>

    init {
        val database = AppDatabase.getInstance(application)
        val apiService = GeminiNewsService()
        repository = NewsRepository(apiService, database.articleDao())

        bookmarkedArticles = repository.bookmarkedArticles
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

        // Initialize Android TextToSpeech for Arabic / native speech
        textToSpeech = TextToSpeech(application, this)

        // Load today's wires
        loadTodayWires()

        // Preload country news
        loadCountryNews(SupportedCountries.list.first())
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(Locale.forLanguageTag("ar"))
            isTtsReady = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
            if (!isTtsReady) {
                textToSpeech?.setLanguage(Locale.getDefault())
                isTtsReady = true
            }
        }
    }

    fun navigateTo(destination: ScreenDestination) {
        if (_currentScreen.value != destination) {
            _screenHistory.add(_currentScreen.value)
            _currentScreen.value = destination
            if (destination != ScreenDestination.ARTICLE_READER) {
                stopTts()
            }
        }
    }

    fun navigateBack(): Boolean {
        stopTts()
        if (_screenHistory.isNotEmpty()) {
            val prev = _screenHistory.removeAt(_screenHistory.size - 1)
            _currentScreen.value = prev
            return true
        } else if (_currentScreen.value != ScreenDestination.HOME) {
            _currentScreen.value = ScreenDestination.HOME
            return true
        }
        return false
    }

    fun loadTodayWires(category: NewsCategory = _selectedCategory.value, region: WorldRegion = _selectedRegion.value) {
        _selectedCategory.value = category
        _selectedRegion.value = region
        viewModelScope.launch {
            _isLoadingFeed.value = true
            try {
                val catQuery = if (category == NewsCategory.ALL) null else category.arabicLabel
                val regQuery = if (region == WorldRegion.ALL) null else region.arabicLabel
                val articles = repository.getTodayWires(catQuery, regQuery)
                _todayArticles.value = articles
            } catch (e: Exception) {
                Log.e("NewsViewModel", "Error loading wires: ${e.message}")
            } finally {
                _isLoadingFeed.value = false
            }
        }
    }

    fun openArticle(article: NewsArticle) {
        // Sync bookmark status with current room bookmarks
        val isBookmarked = bookmarkedArticles.value.any { it.id == article.id }
        _currentArticle.value = article.copy(isBookmarked = isBookmarked)
        _agentAnswer.value = null
        navigateTo(ScreenDestination.ARTICLE_READER)
    }

    fun toggleBookmark(article: NewsArticle) {
        viewModelScope.launch {
            val newStatus = repository.toggleBookmark(article)
            // Update in today's articles
            _todayArticles.value = _todayArticles.value.map {
                if (it.id == article.id) it.copy(isBookmarked = newStatus) else it
            }
            // Update in search results
            _searchResults.value = _searchResults.value.map {
                if (it.id == article.id) it.copy(isBookmarked = newStatus) else it
            }
            // Update in country articles
            _countryArticles.value = _countryArticles.value.map {
                if (it.id == article.id) it.copy(isBookmarked = newStatus) else it
            }
            // Update in current article reader
            if (_currentArticle.value?.id == article.id) {
                _currentArticle.value = _currentArticle.value?.copy(isBookmarked = newStatus)
            }
        }
    }

    // Country Operations
    fun selectCountry(country: CountryItem) {
        _selectedCountry.value = country
        loadCountryNews(country)
        navigateTo(ScreenDestination.COUNTRIES)
    }

    fun loadCountryNews(country: CountryItem) {
        _selectedCountry.value = country
        viewModelScope.launch {
            _isLoadingCountryNews.value = true
            try {
                val articles = repository.getCountryNews(country)
                _countryArticles.value = articles
            } catch (e: Exception) {
                Log.e("NewsViewModel", "Error loading country news: ${e.message}")
            } finally {
                _isLoadingCountryNews.value = false
            }
        }
    }

    fun refreshCountryNews() {
        val country = _selectedCountry.value ?: SupportedCountries.list.first()
        loadCountryNews(country)
    }

    // Advanced Search Operations
    fun updateSearchKeywords(keywords: String) {
        _searchFilters.value = _searchFilters.value.copy(keywords = keywords)
    }

    fun updateSearchCategory(category: NewsCategory) {
        _searchFilters.value = _searchFilters.value.copy(category = category)
    }

    fun updateSearchDateRange(dateRange: DateRangeFilter) {
        _searchFilters.value = _searchFilters.value.copy(dateRange = dateRange)
    }

    fun updateSearchRegion(region: WorldRegion) {
        _searchFilters.value = _searchFilters.value.copy(region = region)
    }

    fun performAdvancedSearch() {
        val filters = _searchFilters.value
        viewModelScope.launch {
            _isSearching.value = true
            _hasPerformedSearch.value = true
            try {
                val results = repository.searchAdvanced(
                    keywords = filters.keywords,
                    category = filters.category.arabicLabel,
                    dateRange = filters.dateRange.arabicLabel,
                    region = filters.region.arabicLabel
                )
                _searchResults.value = results
            } catch (e: Exception) {
                Log.e("NewsViewModel", "Search error: ${e.message}")
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun clearSearchFilters() {
        _searchFilters.value = SearchFilterState()
        _searchResults.value = emptyList()
        _hasPerformedSearch.value = false
    }

    // Text to Speech
    fun toggleTts(text: String) {
        if (_isTtsSpeaking.value) {
            stopTts()
        } else {
            speakText(text)
        }
    }

    private fun speakText(text: String) {
        if (textToSpeech != null) {
            textToSpeech?.stop()
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "article_utterance")
            _isTtsSpeaking.value = true
        }
    }

    private fun stopTts() {
        textToSpeech?.stop()
        _isTtsSpeaking.value = false
    }

    // Font size adjustments in Reader
    fun increaseFontSize() {
        if (_readerFontSizeMultiplier.value < 1.4f) {
            _readerFontSizeMultiplier.value += 0.1f
        }
    }

    fun decreaseFontSize() {
        if (_readerFontSizeMultiplier.value > 0.85f) {
            _readerFontSizeMultiplier.value -= 0.1f
        }
    }

    // Ask AI Agent about current article
    fun askAgentAboutCurrentArticle(question: String) {
        val article = _currentArticle.value ?: return
        if (question.isBlank()) return

        viewModelScope.launch {
            _isAgentAnswering.value = true
            _agentAnswer.value = null
            try {
                val answer = repository.askAgent(
                    articleTitle = article.title,
                    articleContent = article.content,
                    question = question
                )
                _agentAnswer.value = answer
            } catch (e: Exception) {
                _agentAnswer.value = "حدث خطأ أثناء التواصل مع الوكيل: ${e.message}"
            } finally {
                _isAgentAnswering.value = false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
    }
}
