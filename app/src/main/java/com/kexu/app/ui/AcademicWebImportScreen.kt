package com.kexu.app.ui

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Message
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceError
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.School
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import com.kexu.app.ui.MotionIconButton as IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.kexu.app.importer.AcademicWebAdapter
import com.kexu.app.BuildConfig
import com.kexu.app.importer.CourseDraft
import com.kexu.app.importer.EducationSystem
import com.kexu.app.importer.SchoolCatalog
import com.kexu.app.importer.SchoolSpec
import com.kexu.app.ui.theme.Coral
import com.kexu.app.ui.theme.Forest

@Composable
fun AcademicWebImportDialog(
    onDismiss: () -> Unit,
    onImport: (List<CourseDraft>) -> Unit,
) {
    var selectedSchool by remember { mutableStateOf<SchoolSpec?>(null) }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            if (selectedSchool == null) {
                SchoolChooser(
                    onDismiss = onDismiss,
                    onSchoolSelected = { selectedSchool = it },
                )
            } else {
                AcademicBrowser(
                    school = selectedSchool!!,
                    onBack = { selectedSchool = null },
                    onDismiss = onDismiss,
                    onImport = onImport,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SchoolChooser(
    onDismiss: () -> Unit,
    onSchoolSelected: (SchoolSpec) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var customName by rememberSaveable { mutableStateOf("") }
    var customUrl by rememberSaveable { mutableStateOf("") }
    var customSystem by rememberSaveable { mutableStateOf(EducationSystem.AUTO) }
    var showCustom by rememberSaveable { mutableStateOf(false) }
    val schools = remember(query, showCustom) { if (showCustom) emptyList() else SchoolCatalog.search(query) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("选择学校") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "关闭") }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Text("教务网导入", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("导入内核 ${BuildConfig.VERSION_NAME}", color = Forest, style = MaterialTheme.typography.labelMedium)
                Text(
                    "选择学校后进入校方网页。请自行登录并打开学期理论课表，再点击确认导入。带“已验证”标记的学校经过真机页面测试，其余学校会自动尝试多种解析器。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = {
                        query = it
                        showCustom = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("搜索学校、简称或拼音") },
                    singleLine = true,
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { showCustom = !showCustom }) {
                        Text(if (showCustom) "返回学校列表" else "学校不在列表？使用自定义地址")
                    }
                }
            }
            items(schools, key = { it.id }) { school ->
                Card(onClick = { onSchoolSelected(school) }, modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.School, contentDescription = null, tint = Forest)
                        Spacer(Modifier.padding(6.dp))
                        Column(Modifier.weight(1f)) {
                            Text(school.name, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (school.verified) "已验证 · ${school.system.label}" else "自动识别 · 试验性",
                                color = if (school.verified) Forest else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Text("进入", color = Forest)
                    }
                }
            }
            item {
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                Text(
                    if (showCustom) "自定义学校" else "学校尚未收录？",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "可以输入学校官网或教务网地址。不了解系统类型时保持“自动识别”，导入前会依次尝试常见教务结构。",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(customName, { customName = it }, Modifier.fillMaxWidth(), label = { Text("学校名称") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(customUrl, { customUrl = it }, Modifier.fillMaxWidth(), label = { Text("教务网登录网址") }, singleLine = true)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    listOf(
                        EducationSystem.AUTO,
                        EducationSystem.QIANGZHI,
                        EducationSystem.ZHENGFANG,
                        EducationSystem.URP,
                        EducationSystem.QINGGUO,
                        EducationSystem.UNKNOWN,
                    ).forEach { system ->
                        FilterChip(
                            selected = customSystem == system,
                            onClick = { customSystem = system },
                            label = { Text(system.label.replace("教务", "").trim()) },
                        )
                    }
                }
                Button(
                    onClick = { SchoolCatalog.custom(customName, customUrl, customSystem)?.let(onSchoolSelected) },
                    enabled = customUrl.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                ) { Text("打开此教务网") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun AcademicBrowser(
    school: SchoolSpec,
    onBack: () -> Unit,
    onDismiss: () -> Unit,
    onImport: (List<CourseDraft>) -> Unit,
) {
    val context = LocalContext.current
    var webView by remember { mutableStateOf<WebView?>(null) }
    var rootWebView by remember { mutableStateOf<WebView?>(null) }
    var popupWebView by remember { mutableStateOf<WebView?>(null) }
    var popupStack by remember { mutableStateOf(emptyList<WebView>()) }
    var webContainer by remember { mutableStateOf<FrameLayout?>(null) }
    var currentUrl by rememberSaveable { mutableStateOf(school.loginUrl) }
    var loading by remember { mutableStateOf(false) }
    var parsing by remember { mutableStateOf(false) }
    var message by rememberSaveable { mutableStateOf<String?>(null) }
    var preview by remember { mutableStateOf<List<CourseDraft>?>(null) }
    var showDiagnostics by remember { mutableStateOf(false) }
    var diagnostics by remember { mutableStateOf(emptyList<String>()) }

    fun recordDiagnostic(event: String) {
        val line = "${java.time.LocalTime.now().withNano(0)} ${sanitizeDiagnosticText(event)}"
        diagnostics = (diagnostics + line).takeLast(100)
        // Application treasure engine defaults to INFO and would discard Log.d, which made
        // the exported Androws package useless for diagnosing import selection.
        Log.i("KexuWeb", line)
    }

    fun closePopup() {
        val popup = popupStack.lastOrNull() ?: return
        webContainer?.removeView(popup)
        popup.stopLoading()
        popup.destroy()
        popupStack = popupStack.dropLast(1)
        popupWebView = popupStack.lastOrNull()
        webView = popupWebView ?: rootWebView
        webView?.url?.let { currentUrl = it }
        recordDiagnostic("WINDOW RESTORE depth=${popupStack.size} ${safeDiagnosticUrl(webView?.url)}")
    }

    BackHandler {
        when {
            preview != null -> preview = null
            webView?.canGoBack() == true -> webView?.goBack()
            popupWebView != null -> closePopup()
            else -> onBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(school.name, maxLines = 1)
                        Text(
                            (if (currentUrl.startsWith("http://")) "不安全 HTTP · " else "") + Uri.parse(currentUrl).host.orEmpty(),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (currentUrl.startsWith("http://")) Coral else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            when {
                                webView?.canGoBack() == true -> webView?.goBack()
                                popupWebView != null -> closePopup()
                                else -> onBack()
                            }
                        },
                    ) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "网页后退") }
                },
                actions = {
                    IconButton(onClick = { showDiagnostics = true }) {
                        Icon(Icons.Outlined.BugReport, contentDescription = "网页诊断")
                    }
                    IconButton(
                        onClick = {
                            closePopup()
                            rootWebView?.loadUrl(school.loginUrl)
                        },
                    ) { Icon(Icons.Outlined.Home, contentDescription = "返回学校官网") }
                    IconButton(onClick = { webView?.reload() }) { Icon(Icons.Outlined.Refresh, contentDescription = "刷新网页") }
                    TextButton(onClick = onDismiss) { Text("关闭") }
                },
            )
        },
        bottomBar = {
            Column(Modifier.fillMaxWidth().padding(12.dp)) {
                message?.let {
                    Text(it, color = if (preview == null) Coral else Forest, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(6.dp))
                }
                Button(
                    onClick = {
                        val page = webView ?: return@Button
                        parsing = true
                        message = "正在本机读取当前课表页面…"
                        val adapters = AcademicWebAdapter.adaptersFor(school.system)
                        var adapterIndex = 0
                        val adapterErrors = mutableListOf<String>()
                        lateinit var evaluateNext: () -> Unit
                        evaluateNext = {
                            val adapter = adapters[adapterIndex++]
                            val attempt = adapterIndex
                            message = "正在尝试${adapter.label} $attempt/${adapters.size}…"
                            page.evaluateJavascript(adapter.script) { raw ->
                                recordDiagnostic("IMPORT adapter=${adapter.id} attempt=$attempt ${AcademicWebAdapter.diagnosticSummary(raw)}")
                                val result = AcademicWebAdapter.parseEvaluationResult(raw, school)
                                if (result.errors.isEmpty()) {
                                    parsing = false
                                    preview = result.courses
                                    message = "识别到 ${result.courses.size} 条课程安排"
                                } else {
                                    adapterErrors += "${adapter.label}: ${result.errors.first()}"
                                    if (adapterIndex < adapters.size) {
                                        evaluateNext()
                                    } else {
                                        recordDiagnostic("IMPORT FAILED ${adapterErrors.joinToString(" | ")}")
                                        val detectedTimetable = adapterErrors.any {
                                            it.contains("找到了课表") ||
                                                it.contains("检测到了") ||
                                                it.contains("没有读取到完整")
                                        }
                                        val failureMessage = if (detectedTimetable) {
                                            "检测到疑似课表，但页面结构尚未适配完整。请先确认课程是否以完整学期显示；仍失败可复制“网页诊断”反馈。"
                                        } else {
                                            "没有在当前页检测到课表。请进入本学期的“我的课表/学期理论课表”页面后重试；仍失败可复制“网页诊断”反馈。"
                                        }
                                        message = "正在生成不含课程内容的结构诊断…"
                                        page.evaluateJavascript(AcademicWebAdapter.structureDiagnosticScript) { structureRaw ->
                                            recordDiagnostic(
                                                "STRUCTURE ${AcademicWebAdapter.structureDiagnosticSummary(structureRaw)}",
                                            )
                                            parsing = false
                                            message = failureMessage
                                        }
                                    }
                                }
                            }
                        }
                        evaluateNext()
                    },
                    enabled = !parsing,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (parsing) CircularProgressIndicator(Modifier.padding(end = 8.dp))
                    else Icon(Icons.Outlined.CloudDownload, contentDescription = null)
                    Spacer(Modifier.padding(4.dp))
                    Text("我已打开学期课表，确认导入")
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    FrameLayout(context).also { container ->
                        webContainer = container
                        lateinit var configureWebView: (WebView) -> Unit
                        configureWebView = { target ->
                            target.settings.javaScriptEnabled = true
                            target.settings.domStorageEnabled = true
                            target.settings.allowFileAccess = false
                            target.settings.allowContentAccess = false
                            target.settings.javaScriptCanOpenWindowsAutomatically = true
                            target.settings.setSupportMultipleWindows(true)
                            target.settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                            target.settings.useWideViewPort = true
                            target.settings.loadWithOverviewMode = true
                            CookieManager.getInstance().setAcceptCookie(true)
                            CookieManager.getInstance().setAcceptThirdPartyCookies(target, true)
                            target.webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    loading = true
                                    message = null
                                    url?.let { currentUrl = it }
                                    recordDiagnostic("PAGE START ${safeDiagnosticUrl(url)}")
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    loading = false
                                    url?.let { currentUrl = it }
                                    recordDiagnostic("PAGE FINISH ${safeDiagnosticUrl(url)} title=${view?.title.orEmpty()}")
                                }

                                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                    val uri = request?.url ?: return true
                                    if (uri.scheme == "about") return false
                                    // Browsing must behave like a normal browser because campus SSO,
                                    // WebVPN and the academic system can legitimately use different hosts.
                                    // Non-web schemes remain blocked; page extraction still runs only
                                    // after the user explicitly taps Confirm Import.
                                    val blocked = uri.scheme !in listOf("http", "https")
                                    recordDiagnostic("NAV ${request.method} ${safeDiagnosticUrl(uri.toString())} blocked=$blocked gesture=${request.hasGesture()}")
                                    return blocked
                                }

                                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                                    if (request?.isForMainFrame == true) {
                                        loading = false
                                        message = "页面加载失败：${error?.description ?: "网络或证书错误"}"
                                        recordDiagnostic("MAIN ERROR code=${error?.errorCode} description=${error?.description} url=${safeDiagnosticUrl(request.url.toString())}")
                                    }
                                }

                                override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
                                    if (request?.isForMainFrame == true) {
                                        recordDiagnostic("HTTP ERROR status=${errorResponse?.statusCode} reason=${errorResponse?.reasonPhrase} url=${safeDiagnosticUrl(request.url.toString())}")
                                    }
                                }

                                override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
                                    recordDiagnostic("SSL ERROR primary=${error?.primaryError} url=${safeDiagnosticUrl(error?.url)}")
                                    message = "证书校验失败，已停止加载；可在网页诊断中查看详情"
                                    handler?.cancel()
                                }
                            }
                            target.webChromeClient = object : WebChromeClient() {
                                override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean {
                                    val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return false
                                    recordDiagnostic("WINDOW CREATE depth=${popupStack.size + 1} dialog=$isDialog gesture=$isUserGesture opener=${safeDiagnosticUrl(view?.url)}")
                                    val popup = WebView(context)
                                    configureWebView(popup)
                                    container.addView(
                                        popup,
                                        FrameLayout.LayoutParams(
                                            FrameLayout.LayoutParams.MATCH_PARENT,
                                            FrameLayout.LayoutParams.MATCH_PARENT,
                                        ),
                                    )
                                    popupStack = popupStack + popup
                                    popupWebView = popup
                                    webView = popup
                                    transport.webView = popup
                                    resultMsg.sendToTarget()
                                    return true
                                }

                                override fun onCloseWindow(window: WebView?) {
                                    recordDiagnostic("WINDOW CLOSE ${safeDiagnosticUrl(window?.url)}")
                                    if (window != null && window in popupStack) {
                                        container.removeView(window)
                                        window.stopLoading()
                                        window.destroy()
                                        popupStack = popupStack.filterNot { it == window }
                                        popupWebView = popupStack.lastOrNull()
                                        webView = popupWebView ?: rootWebView
                                        webView?.url?.let { currentUrl = it }
                                    }
                                }

                                override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                                    if (consoleMessage?.messageLevel() in setOf(ConsoleMessage.MessageLevel.ERROR, ConsoleMessage.MessageLevel.WARNING)) {
                                        recordDiagnostic(
                                            "CONSOLE ${consoleMessage?.messageLevel()} ${consoleMessage?.message()} " +
                                                "source=${safeDiagnosticUrl(consoleMessage?.sourceId())}:${consoleMessage?.lineNumber()}",
                                        )
                                    }
                                    return super.onConsoleMessage(consoleMessage)
                                }
                            }
                        }

                        val main = WebView(context)
                        configureWebView(main)
                        container.addView(
                            main,
                            FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT,
                            ),
                        )
                        rootWebView = main
                        webView = main
                        main.loadUrl(school.loginUrl)
                    }
                },
            )
            if (loading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            }
            preview?.let { courses ->
                ImportPreview(
                    school = school,
                    courses = courses,
                    onBack = { preview = null },
                    onCoursesChange = { preview = it },
                    onConfirm = { onImport(preview.orEmpty()) },
                )
            }
            if (showDiagnostics) {
                WebDiagnosticsDialog(
                    currentUrl = currentUrl,
                    events = diagnostics,
                    onDismiss = { showDiagnostics = false },
                    onClear = { diagnostics = emptyList() },
                )
            }
        }
    }

    DisposableEffect(Unit) {
        val webViewPackage = WebView.getCurrentWebViewPackage()
        recordDiagnostic("SESSION app=${BuildConfig.VERSION_NAME} webview=${webViewPackage?.packageName}/${webViewPackage?.versionName}")
        onDispose {
            popupStack.forEach { popup ->
                popup.stopLoading()
                popup.destroy()
            }
            rootWebView?.stopLoading()
            rootWebView?.destroy()
            webContainer?.removeAllViews()
            popupStack = emptyList()
            popupWebView = null
            rootWebView = null
            webContainer = null
            webView = null
        }
    }
}

@Composable
private fun WebDiagnosticsDialog(
    currentUrl: String,
    events: List<String>,
    onDismiss: () -> Unit,
    onClear: () -> Unit,
) {
    val context = LocalContext.current
    val report = buildString {
        appendLine("不着急网页诊断 ${BuildConfig.VERSION_NAME}")
        appendLine("当前页面：${safeDiagnosticUrl(currentUrl)}")
        appendLine("事件：")
        events.forEach(::appendLine)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("网页诊断") },
        text = {
            SelectionContainer {
                LazyColumn(Modifier.fillMaxWidth().height(420.dp)) {
                    item {
                        Text(
                            "结构诊断只记录表格尺寸、选择器和页面路径，不记录账号、课程名、教师、教室、网页正文或 Cookie。",
                            color = Forest,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (events.isEmpty()) "暂无事件。关闭诊断后重现一次白屏，再回来查看。" else report,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onClear) { Text("清空") }
                TextButton(onClick = onDismiss) { Text("关闭") }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("不着急网页诊断", report))
                },
            ) { Text("复制诊断") }
        },
    )
}

private fun safeDiagnosticUrl(value: String?): String {
    if (value.isNullOrBlank()) return "(none)"
    return runCatching {
        val uri = Uri.parse(value)
        val safePath = uri.path.orEmpty()
            .replace(Regex("\\d{5,}"), "<number>")
            .replace(Regex("[A-Za-z0-9_-]{24,}"), "<redacted>")
        "${uri.scheme.orEmpty()}://${uri.host.orEmpty()}$safePath"
    }.getOrDefault("(invalid-url)")
}

private fun sanitizeDiagnosticText(value: String): String = value
    .replace(Regex("(?i)(token|ticket|code|session|password|pwd)=([^&\\s]+)"), "\$1=<redacted>")
    .take(500)

@Composable
private fun ImportPreview(
    school: SchoolSpec,
    courses: List<CourseDraft>,
    onBack: () -> Unit,
    onCoursesChange: (List<CourseDraft>) -> Unit,
    onConfirm: () -> Unit,
) {
    var editingIndex by remember { mutableStateOf<Int?>(null) }
    val issueSummary = remember(courses) { analyzeImportIssues(courses) }
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回网页") }
                Column(Modifier.weight(1f)) {
                    Text("课表预览", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "${school.name} · ${courses.size} 条安排 · 请确认课程、周次和时间",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 16.dp)) {
                if (issueSummary.duplicatePairs > 0 || issueSummary.conflictPairs > 0) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                            Text(
                                buildString {
                                    append("导入前请检查：")
                                    if (issueSummary.duplicatePairs > 0) append(" ${issueSummary.duplicatePairs} 组疑似重复")
                                    if (issueSummary.duplicatePairs > 0 && issueSummary.conflictPairs > 0) append("，")
                                    if (issueSummary.conflictPairs > 0) append(" ${issueSummary.conflictPairs} 处时间冲突")
                                },
                                modifier = Modifier.padding(14.dp),
                                color = Coral,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                }
                itemsIndexed(
                    courses,
                    key = { index, course -> "$index:${course.name}:${course.day}:${course.startMinutes}:${course.weekMask}" },
                ) { index, course ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(course.name, fontWeight = FontWeight.SemiBold)
                            Text(
                                "周${course.day.value} · ${minutesTextForImport(course.startMinutes)}-${minutesTextForImport(course.endMinutes)} · ${formatWeeks(course)}" +
                                    course.location.takeIf(String::isNotBlank)?.let { " · $it" }.orEmpty(),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            issueSummary.messages[index]?.takeIf { it.isNotEmpty() }?.let { messages ->
                                Text(
                                    messages.distinct().take(2).joinToString("；"),
                                    color = Coral,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                        IconButton(onClick = { editingIndex = index }) {
                            Icon(Icons.Outlined.Edit, contentDescription = "编辑课程")
                        }
                    }
                    HorizontalDivider()
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) { Text("返回检查") }
                Button(onClick = onConfirm, enabled = courses.isNotEmpty(), modifier = Modifier.weight(1f)) { Text("课表信息正确") }
            }
        }
    }
    editingIndex?.let { index ->
        courses.getOrNull(index)?.let { course ->
            ImportCourseEditDialog(
                course = course,
                onDismiss = { editingIndex = null },
                onDelete = {
                    onCoursesChange(courses.filterIndexed { itemIndex, _ -> itemIndex != index })
                    editingIndex = null
                },
                onSave = { changed ->
                    onCoursesChange(courses.mapIndexed { itemIndex, item -> if (itemIndex == index) changed else item })
                    editingIndex = null
                },
            )
        }
    }
}

internal data class ImportIssueSummary(
    val messages: Map<Int, List<String>>,
    val duplicatePairs: Int,
    val conflictPairs: Int,
)

internal fun analyzeImportIssues(courses: List<CourseDraft>): ImportIssueSummary {
    val messages = mutableMapOf<Int, MutableList<String>>()
    var duplicatePairs = 0
    var conflictPairs = 0
    fun add(index: Int, message: String) {
        messages.getOrPut(index) { mutableListOf() } += message
    }
    for (leftIndex in courses.indices) {
        val left = courses[leftIndex]
        val leftWeeks = effectiveWeekMask(left)
        for (rightIndex in leftIndex + 1 until courses.size) {
            val right = courses[rightIndex]
            if (left.day != right.day) continue
            if (leftWeeks and effectiveWeekMask(right) == 0L) continue
            val sameTime = left.startMinutes == right.startMinutes && left.endMinutes == right.endMinutes
            val duplicate = sameTime && left.name.trim().equals(right.name.trim(), ignoreCase = true)
            if (duplicate) {
                duplicatePairs++
                add(leftIndex, "疑似与“${right.name}”重复")
                add(rightIndex, "疑似与“${left.name}”重复")
            } else if (left.startMinutes < right.endMinutes && right.startMinutes < left.endMinutes) {
                conflictPairs++
                add(leftIndex, "与“${right.name}”时间冲突")
                add(rightIndex, "与“${left.name}”时间冲突")
            }
        }
    }
    return ImportIssueSummary(messages, duplicatePairs, conflictPairs)
}

private fun effectiveWeekMask(course: CourseDraft): Long {
    if (course.weekMask != 0L) return course.weekMask
    return (course.startWeek.coerceAtLeast(1)..course.endWeek.coerceIn(1, 63)).fold(0L) { mask, week ->
        val included = when (course.weekPattern) {
            com.kexu.app.data.model.WeekPattern.ALL -> true
            com.kexu.app.data.model.WeekPattern.ODD -> week % 2 == 1
            com.kexu.app.data.model.WeekPattern.EVEN -> week % 2 == 0
        }
        if (included) mask or (1L shl (week - 1)) else mask
    }
}

@Composable
private fun ImportCourseEditDialog(
    course: CourseDraft,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onSave: (CourseDraft) -> Unit,
) {
    var name by remember(course) { mutableStateOf(course.name) }
    var teacher by remember(course) { mutableStateOf(course.teacher) }
    var location by remember(course) { mutableStateOf(course.location) }
    var day by remember(course) { mutableStateOf(course.day) }
    var start by remember(course) { mutableStateOf(minutesTextForImport(course.startMinutes)) }
    var end by remember(course) { mutableStateOf(minutesTextForImport(course.endMinutes)) }
    var weeksText by remember(course) { mutableStateOf(formatWeeks(course, compact = true)) }
    val startMinutes = parseImportMinutes(start)
    val endMinutes = parseImportMinutes(end)
    val weeks = parseImportWeeks(weeksText)
    val valid = name.isNotBlank() && startMinutes != null && endMinutes != null && endMinutes > startMinutes && weeks.isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑识别结果") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item { OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), label = { Text("课程名称") }, singleLine = true) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(teacher, { teacher = it }, Modifier.weight(1f), label = { Text("教师") }, singleLine = true)
                        OutlinedTextField(location, { location = it }, Modifier.weight(1f), label = { Text("教室") }, singleLine = true)
                    }
                }
                item {
                    Text("星期", fontWeight = FontWeight.SemiBold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                listOf(
                                    java.time.DayOfWeek.SUNDAY,
                                    java.time.DayOfWeek.MONDAY,
                                    java.time.DayOfWeek.TUESDAY,
                                    java.time.DayOfWeek.WEDNESDAY,
                                    java.time.DayOfWeek.THURSDAY,
                                    java.time.DayOfWeek.FRIDAY,
                                    java.time.DayOfWeek.SATURDAY,
                                ).forEach { value ->
                            FilterChip(selected = day == value, onClick = { day = value }, label = { Text(value.value.toString()) })
                        }
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(start, { start = it }, Modifier.weight(1f), label = { Text("开始") }, singleLine = true)
                        OutlinedTextField(end, { end = it }, Modifier.weight(1f), label = { Text("结束") }, singleLine = true)
                    }
                }
                item {
                    OutlinedTextField(
                        weeksText,
                        { weeksText = it },
                        Modifier.fillMaxWidth(),
                        label = { Text("周次（如 1-5,8-10）") },
                        supportingText = { if (weeks.isEmpty()) Text("请输入 1 至 63 之间的周次") },
                        singleLine = true,
                    )
                }
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) { Text("删除", color = Coral) }
                TextButton(onClick = onDismiss) { Text("取消") }
            }
        },
        confirmButton = {
            Button(
                enabled = valid,
                onClick = {
                    onSave(
                        course.copy(
                            name = name.trim(),
                            teacher = teacher.trim(),
                            location = location.trim(),
                            day = day,
                            startMinutes = startMinutes!!,
                            endMinutes = endMinutes!!,
                            startWeek = weeks.first(),
                            endWeek = weeks.last(),
                            weekPattern = com.kexu.app.data.model.WeekPattern.ALL,
                            weekMask = weeks.fold(0L) { mask, week -> mask or (1L shl (week - 1)) },
                        ),
                    )
                },
            ) { Text("保存") }
        },
    )
}

private fun minutesTextForImport(value: Int): String = "%02d:%02d".format(value / 60, value % 60)

private fun parseImportMinutes(value: String): Int? {
    val parts = value.trim().split(':', '：')
    if (parts.size != 2) return null
    val hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    if (hour !in 0..23 || minute !in 0..59) return null
    return hour * 60 + minute
}

private fun parseImportWeeks(value: String): List<Int> = buildSet {
    value.replace('，', ',').replace('、', ',').split(',').forEach { part ->
        val bounds = part.trim().split('-', '~', '至').mapNotNull(String::toIntOrNull)
        if (bounds.isEmpty()) return@forEach
        val end = bounds.getOrElse(1) { bounds[0] }
        if (bounds[0] in 1..63 && end in bounds[0]..63) addAll(bounds[0]..end)
    }
}.sorted()

private fun formatWeeks(course: CourseDraft, compact: Boolean = false): String {
    val weeks = if (course.weekMask != 0L) {
        (1..63).filter { week -> course.weekMask and (1L shl (week - 1)) != 0L }
    } else {
        (course.startWeek..course.endWeek).filter { week ->
            when (course.weekPattern) {
                com.kexu.app.data.model.WeekPattern.ALL -> true
                com.kexu.app.data.model.WeekPattern.ODD -> week % 2 == 1
                com.kexu.app.data.model.WeekPattern.EVEN -> week % 2 == 0
            }
        }
    }
    if (weeks.isEmpty()) return if (compact) "" else "未设置周次"
    val ranges = mutableListOf<String>()
    var start = weeks.first()
    var previous = start
    for (week in weeks.drop(1)) {
        if (week == previous + 1) {
            previous = week
        } else {
            ranges += if (start == previous) "$start" else "$start-$previous"
            start = week
            previous = week
        }
    }
    ranges += if (start == previous) "$start" else "$start-$previous"
    return ranges.joinToString(",") + if (compact) "" else "周"
}
