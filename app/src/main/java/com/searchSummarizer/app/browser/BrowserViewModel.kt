package com.searchSummarizer.app.browser

import android.content.Intent
import android.webkit.URLUtil
import android.webkit.WebView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.searchSummarizer.data.enumType.Urls
import com.searchSummarizer.data.model.BrowserHistory
import com.searchSummarizer.data.model.SummarizedUrl
import com.searchSummarizer.data.repo.browser.BrowserRepository
import com.searchSummarizer.data.repo.context.ContextRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.math.exp

class BrowserViewModel(
    private val browserRepository: BrowserRepository,
    contextRepository: ContextRepository,
) : ViewModel() {

    var webView: WebView by mutableStateOf(WebView(contextRepository.createContext()))
    var tabIndex: Int by mutableStateOf(0)
    var urlHistory: MutableList<MutableList<String>> =
        mutableStateListOf(mutableStateListOf(Urls.Default.url))
    var titles: MutableList<String> = mutableStateListOf(defaultTitle)
    var expanded: Boolean by mutableStateOf(true)
    var menuExpanded: Boolean by mutableStateOf(false)
    var keyword: String by mutableStateOf("")
    var backEnabled: Boolean by mutableStateOf(false)

    fun onDismissTabAll() {
        tabIndex = 0
        webView.loadUrl(Urls.Default.url)
        urlHistory = mutableStateListOf(mutableStateListOf(Urls.Default.url))
    }

    fun onMenuDismissRequest() {
        menuExpanded = !menuExpanded
    }

    fun onBack() {
        urlHistory[tabIndex].removeLast()
        webView.loadUrl(urlHistory[tabIndex].last())
    }

    fun onSearch() {
        expanded = !expanded
        val url = when {
            keyword == "" -> return // empty
            URLUtil.isValidUrl(keyword) -> keyword // valid url
            else -> Urls.GoogleSearch(keyword).url // keyword
        }
        webView.loadUrl(url)
    }

    fun onSwitchTab(tabIndex: Int) {
        webView.loadUrl(urlHistory[tabIndex].last())
        this.tabIndex = tabIndex
        expanded = !expanded
    }

    fun onAddTab() {
        urlHistory.add(mutableListOf(Urls.Default.url))
        tabIndex = urlHistory.size - 1
        titles.add(defaultTitle)
        webView.loadUrl(Urls.Default.url)
        expanded = !expanded
    }

    fun onTabClick() {
        if (expanded) {
            keyword = ""
            expanded = !expanded
        }
    }

    fun restoreBrowserHistory() {
        viewModelScope.launch {
            browserRepository.browserHistoryFlow.collect { browserHistory ->
                if (browserHistory.urls.isNotEmpty()) {
                    tabIndex = browserHistory.selectedTabIndex
                    titles = browserHistory.titles.replace(" ", "").split(",").toMutableList()
                    urlHistory = browserHistory.urls.replace(" ", "").split("^").map {
                        it.split(",").toMutableList()
                    }.toMutableList()
                    webView.loadUrl(urlHistory[tabIndex].last())
                } else {
                    webView.loadUrl(urlHistory[0].last())
                }
            }
        }
    }

    fun saveBrowserHistory() = runBlocking {
        browserRepository.saveBrowserHistory(
            BrowserHistory(
                selectedTabIndex = tabIndex,
                titles = titles.joinToString(),
                urls = urlHistory.joinToString("^") // one dimensional array separator is "^"
                { it.joinToString(",") } // two dimensional array separator is ","
            )
        )
    }

    fun findSummarizedUrl(intent: Intent): SummarizedUrl? = runBlocking {
        val id = intent.data?.getQueryParameter("id") ?: return@runBlocking null
        browserRepository.findSummarizedUrl(id)
    }

    fun expandSummarizedUrl(titles: List<String>, urls: List<String>) {
        val expandedUrl = mutableListOf<MutableList<String>>()
        urls.forEach { url ->
            expandedUrl.add(mutableListOf(url))
        }
        this.titles = titles.toMutableList()
        tabIndex = 0
        urlHistory = expandedUrl
    }
}

private const val defaultTitle = "Google"
