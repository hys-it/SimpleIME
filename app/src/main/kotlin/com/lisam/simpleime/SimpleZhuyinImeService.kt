package com.lisam.simpleime

import android.graphics.Color
import android.inputmethodservice.InputMethodService
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.TextView

class SimpleZhuyinImeService : InputMethodService() {

    private lateinit var rootView: LinearLayout
    private lateinit var candidateScroll: HorizontalScrollView
    private lateinit var candidateRow: LinearLayout
    private lateinit var keyboardRowsContainer: LinearLayout
    private lateinit var controlRow: LinearLayout

    private var currentZhuyin = StringBuilder()
    private var inputMode = InputMode.ZHUYIN
    private var previousAlphaMode = InputMode.ZHUYIN
    private var isEnglishUppercase = false
    private var isHardwareZhuyinMode = false

    private val phraseDictionary = mapOf(
        "ㄓㄨㄥ" to listOf("中", "種", "重", "終", "鐘"),
        "ㄍㄨㄛˊ" to listOf("國"),
        "ㄊㄞˊ" to listOf("台", "臺", "抬"),
        "ㄅㄟˇ" to listOf("北"),
        "ㄊㄞˊㄅㄟˇ" to listOf("台北", "臺北"),
        "ㄋㄧˇ" to listOf("你", "妳"),
        "ㄏㄠˇ" to listOf("好"),
        "ㄋㄧˇㄏㄠˇ" to listOf("你好", "妳好"),
        "ㄨㄛˇ" to listOf("我"),
        "ㄕˋ" to listOf("是", "事", "市", "世", "試"),
        "ㄉㄜ˙" to listOf("的", "得", "地"),
        "ㄌㄜ˙" to listOf("了"),
        "ㄇㄣˊ" to listOf("們", "門"),
        "ㄖㄣˊ" to listOf("人"),
        "ㄉㄚˋ" to listOf("大"),
        "ㄒㄧㄠˇ" to listOf("小"),
        "ㄕㄨㄟˇ" to listOf("水"),
        "ㄏㄨㄛˇ" to listOf("火"),
        "ㄕㄢ" to listOf("山"),
        "ㄏㄞˇ" to listOf("海")
    )

    private val fullDictionary by lazy { loadDictionaryFromAssets() }
    private val normalizedDictionary by lazy { buildNormalizedDictionary(fullDictionary, phraseDictionary) }
    private val selectionCountPrefs by lazy { getSharedPreferences(SELECTION_COUNT_PREFS, MODE_PRIVATE) }
    private val chineseSelectionCounts by lazy { loadChineseSelectionCounts() }

    private enum class InputMode {
        ZHUYIN,
        ENGLISH,
        SYMBOL
    }

    companion object {
        private const val SELECTION_COUNT_PREFS = "chinese_selection_counts"
        private const val SELECTION_COUNT_PREFIX = "ch_"
        private val TONE_MARKS = setOf('ˊ', 'ˇ', 'ˋ', '˙')

        private val ZHUYIN_ROWS = listOf(
            listOf("ㄅ", "ㄆ", "ㄇ", "ㄈ", "ㄉ", "ㄊ", "ㄋ", "ㄌ", "ㄍ", "ㄎ", "ㄏ"),
            listOf("ㄐ", "ㄑ", "ㄒ", "ㄓ", "ㄔ", "ㄕ", "ㄖ", "ㄗ", "ㄘ", "ㄙ"),
            listOf("ㄧ", "ㄨ", "ㄩ", "ㄚ", "ㄛ", "ㄜ", "ㄝ", "ㄞ", "ㄟ", "ㄠ"),
            listOf("ㄡ", "ㄢ", "ㄣ", "ㄤ", "ㄥ", "ㄦ", "ˊ", "ˇ", "ˋ", "˙")
        )

        private val ENGLISH_ROWS = listOf(
            listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
            listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
            listOf("z", "x", "c", "v", "b", "n", "m"),
            listOf(",", ".", "?", "!")
        )

        private val SYMBOL_ROWS = listOf(
            listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "0"),
            listOf("@", "#", "$", "%", "&", "*", "-", "+", "(", ")"),
            listOf("/", ";", ":", "'", "\"", "?", "!", "_", "=", "\\"),
            listOf(",", ".", "，", "。", "、", "？", "！", "「", "」", "～")
        )

        // IBM Zhuyin physical-key mapping for external hardware keyboards.
        private val HARDWARE_ZHUYIN_KEY_MAP = mapOf(
            KeyEvent.KEYCODE_1 to "ㄅ",
            KeyEvent.KEYCODE_2 to "ㄆ",
            KeyEvent.KEYCODE_3 to "ㄇ",
            KeyEvent.KEYCODE_4 to "ㄈ",
            KeyEvent.KEYCODE_5 to "ㄉ",
            KeyEvent.KEYCODE_6 to "ㄊ",
            KeyEvent.KEYCODE_7 to "ㄋ",
            KeyEvent.KEYCODE_8 to "ㄌ",
            KeyEvent.KEYCODE_9 to "ㄍ",
            KeyEvent.KEYCODE_0 to "ㄎ",
            KeyEvent.KEYCODE_MINUS to "ㄏ",
            KeyEvent.KEYCODE_Q to "ㄐ",
            KeyEvent.KEYCODE_W to "ㄑ",
            KeyEvent.KEYCODE_E to "ㄒ",
            KeyEvent.KEYCODE_R to "ㄓ",
            KeyEvent.KEYCODE_T to "ㄔ",
            KeyEvent.KEYCODE_Y to "ㄕ",
            KeyEvent.KEYCODE_U to "ㄖ",
            KeyEvent.KEYCODE_I to "ㄗ",
            KeyEvent.KEYCODE_O to "ㄘ",
            KeyEvent.KEYCODE_P to "ㄙ",
            KeyEvent.KEYCODE_A to "ㄧ",
            KeyEvent.KEYCODE_S to "ㄨ",
            KeyEvent.KEYCODE_D to "ㄩ",
            KeyEvent.KEYCODE_F to "ㄚ",
            KeyEvent.KEYCODE_G to "ㄛ",
            KeyEvent.KEYCODE_H to "ㄜ",
            KeyEvent.KEYCODE_J to "ㄝ",
            KeyEvent.KEYCODE_K to "ㄞ",
            KeyEvent.KEYCODE_L to "ㄟ",
            KeyEvent.KEYCODE_SEMICOLON to "ㄠ",
            KeyEvent.KEYCODE_Z to "ㄡ",
            KeyEvent.KEYCODE_X to "ㄢ",
            KeyEvent.KEYCODE_C to "ㄣ",
            KeyEvent.KEYCODE_V to "ㄤ",
            KeyEvent.KEYCODE_B to "ㄥ",
            KeyEvent.KEYCODE_N to "ㄦ",
            KeyEvent.KEYCODE_M to "ˊ",
            KeyEvent.KEYCODE_COMMA to "ˇ",
            KeyEvent.KEYCODE_PERIOD to "ˋ",
            KeyEvent.KEYCODE_SLASH to "˙"
        )
    }

    override fun onCreateInputView(): View {
        rootView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.BLACK)
        }

        candidateRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(8), dp(4), dp(8), dp(4))
            minimumHeight = dp(44)
        }
        candidateScroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            addView(
                candidateRow,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            )
        }
        rootView.addView(candidateScroll)

        keyboardRowsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        rootView.addView(keyboardRowsContainer)

        controlRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        rootView.addView(controlRow)

        rootView.addView(
            View(this),
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(54)
            )
        )

        renderKeyboardLayout()
        return rootView
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        // Hotkey: Ctrl + Space toggles hardware Zhuyin mode.
        if (keyCode == KeyEvent.KEYCODE_SPACE && event.isCtrlPressed) {
            toggleHardwareZhuyinMode()
            return true
        }

        if (!isHardwareZhuyinMode) {
            return super.onKeyDown(keyCode, event)
        }

        if (event.isCtrlPressed || event.isAltPressed || event.isMetaPressed) {
            return super.onKeyDown(keyCode, event)
        }

        if (handleHardwareZhuyinKey(keyCode)) {
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (keyCode == KeyEvent.KEYCODE_SPACE && event.isCtrlPressed) {
            return true
        }
        if (isHardwareZhuyinMode && isHardwareZhuyinHandledKey(keyCode)) {
            return true
        }
        return super.onKeyUp(keyCode, event)
    }

    private fun renderKeyboardLayout() {
        renderKeyRows()
        renderControlRow()
        updateComposingText()
        updateCandidates()
    }

    private fun toggleHardwareZhuyinMode() {
        isHardwareZhuyinMode = !isHardwareZhuyinMode
        if (isHardwareZhuyinMode && inputMode != InputMode.ZHUYIN) {
            inputMode = InputMode.ZHUYIN
            currentZhuyin.clear()
            renderKeyboardLayout()
        } else {
            updateCandidates()
        }
    }

    private fun handleHardwareZhuyinKey(keyCode: Int): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DEL -> {
                onBackspace()
                return true
            }

            KeyEvent.KEYCODE_SPACE -> {
                onSpace()
                return true
            }

            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                onEnter()
                return true
            }
        }

        val zhuyin = HARDWARE_ZHUYIN_KEY_MAP[keyCode] ?: return false
        if (inputMode != InputMode.ZHUYIN) {
            inputMode = InputMode.ZHUYIN
            renderKeyboardLayout()
        }
        onZhuyinKey(zhuyin)
        return true
    }

    private fun isHardwareZhuyinHandledKey(keyCode: Int): Boolean {
        return keyCode == KeyEvent.KEYCODE_DEL ||
            keyCode == KeyEvent.KEYCODE_SPACE ||
            keyCode == KeyEvent.KEYCODE_ENTER ||
            keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER ||
            HARDWARE_ZHUYIN_KEY_MAP.containsKey(keyCode)
    }

    private fun renderKeyRows() {
        keyboardRowsContainer.removeAllViews()
        val rows = getRowsForMode()
        val keyTextSize = when (inputMode) {
            InputMode.ZHUYIN -> 20f
            InputMode.ENGLISH -> 18f
            InputMode.SYMBOL -> 18f
        }

        for (row in rows) {
            val rowLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            keyboardRowsContainer.addView(rowLayout)

            for (key in row) {
                val button = Button(this).apply {
                    text = getDisplayKeyText(key)
                    isAllCaps = false
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, keyTextSize)
                    setTextColor(Color.GREEN)
                    setBackgroundColor(Color.rgb(20, 20, 20))
                    setOnClickListener { onMainKeyPressed(key) }
                }
                rowLayout.addView(
                    button,
                    LinearLayout.LayoutParams(0, dp(48), 1f)
                )
            }
        }
    }

    private fun renderControlRow() {
        controlRow.removeAllViews()

        addControlButton(controlRow, "中/英", 0.9f) { toggleInputMode() }
        addControlButton(controlRow, "?123", 0.9f) { toggleSymbolMode() }
        addControlButton(controlRow, "←", 0.7f) { onBackspace() }
        addControlButton(controlRow, "空白", 1.8f) { onSpace() }
        addControlButton(controlRow, "↩", 0.7f) { onEnter() }
        addControlButton(controlRow, "↑", 0.7f, enabled = inputMode == InputMode.ENGLISH) { onShift() }
    }

    private fun getRowsForMode(): List<List<String>> {
        return when (inputMode) {
            InputMode.ZHUYIN -> ZHUYIN_ROWS
            InputMode.ENGLISH -> ENGLISH_ROWS
            InputMode.SYMBOL -> SYMBOL_ROWS
        }
    }

    private fun getDisplayKeyText(key: String): String {
        if (inputMode != InputMode.ENGLISH || key.length != 1 || !key[0].isLetter()) {
            return key
        }
        return if (isEnglishUppercase) key.uppercase() else key
    }

    private fun onMainKeyPressed(key: String) {
        when (inputMode) {
            InputMode.ZHUYIN -> onZhuyinKey(key)
            InputMode.ENGLISH -> onEnglishKey(key)
            InputMode.SYMBOL -> onSymbolKey(key)
        }
    }

    private fun onZhuyinKey(key: String) {
        currentZhuyin.append(key)
        updateComposingText()
        updateCandidates()
    }

    private fun onEnglishKey(key: String) {
        if (key.length == 1 && key[0].isLetter()) {
            val text = if (isEnglishUppercase) key.uppercase() else key
            currentInputConnection?.commitText(text, 1)
        } else {
            currentInputConnection?.commitText(key, 1)
        }
    }

    private fun onSymbolKey(key: String) {
        currentInputConnection?.commitText(key, 1)
    }

    private fun toggleInputMode() {
        if (inputMode == InputMode.ZHUYIN && currentZhuyin.isNotEmpty()) {
            commitCurrentText()
        }

        if (inputMode == InputMode.SYMBOL) {
            inputMode = if (previousAlphaMode == InputMode.ENGLISH) InputMode.ZHUYIN else InputMode.ENGLISH
        } else {
            inputMode = if (inputMode == InputMode.ZHUYIN) InputMode.ENGLISH else InputMode.ZHUYIN
            previousAlphaMode = inputMode
        }

        currentZhuyin.clear()
        renderKeyboardLayout()
    }

    private fun toggleSymbolMode() {
        if (inputMode == InputMode.SYMBOL) {
            inputMode = previousAlphaMode
        } else {
            if (inputMode == InputMode.ZHUYIN && currentZhuyin.isNotEmpty()) {
                commitCurrentText()
            }
            previousAlphaMode = if (inputMode == InputMode.ENGLISH) InputMode.ENGLISH else InputMode.ZHUYIN
            inputMode = InputMode.SYMBOL
        }
        currentZhuyin.clear()
        renderKeyboardLayout()
    }

    private fun onShift() {
        if (inputMode != InputMode.ENGLISH) {
            return
        }
        isEnglishUppercase = !isEnglishUppercase
        renderKeyboardLayout()
    }

    private fun updateComposingText() {
        // Removed composing strip above candidate row as requested.
    }

    private fun onBackspace() {
        if (inputMode == InputMode.ZHUYIN && currentZhuyin.isNotEmpty()) {
            currentZhuyin.deleteCharAt(currentZhuyin.length - 1)
            updateComposingText()
            updateCandidates()
        } else {
            currentInputConnection?.deleteSurroundingText(1, 0)
        }
    }

    private fun onSpace() {
        if (inputMode != InputMode.ZHUYIN) {
            currentInputConnection?.commitText(" ", 1)
            return
        }

        val candidates = getCandidates(currentZhuyin.toString())
        if (candidates.isNotEmpty()) {
            commitText(candidates[0])
        } else {
            currentInputConnection?.commitText(" ", 1)
        }
    }

    private fun commitCurrentText() {
        if (currentZhuyin.isNotEmpty()) {
            val candidates = getCandidates(currentZhuyin.toString())
            if (candidates.isNotEmpty()) {
                commitText(candidates[0])
            } else {
                currentInputConnection?.commitText(currentZhuyin.toString(), 1)
                currentZhuyin.clear()
                updateComposingText()
                updateCandidates()
            }
        }
    }

    private fun onEnter() {
        if (inputMode == InputMode.ZHUYIN && currentZhuyin.isNotEmpty()) {
            commitCurrentText()
        }
        if (!sendDefaultEditorAction(true)) {
            sendKeyChar('\n')
        }
    }

    private fun commitText(text: String) {
        currentInputConnection?.commitText(text, 1)
        recordChineseSelection(text)
        currentZhuyin.clear()
        updateComposingText()
        updateCandidates()
    }

    private fun getCandidates(code: String): List<String> {
        if (code.isBlank() || inputMode != InputMode.ZHUYIN) {
            return emptyList()
        }

        val normalizedCode = normalizeZhuyin(code)
        return sortChineseCandidatesBySelectionCount(
            mergeCandidates(
                phraseDictionary[code],
                fullDictionary[code],
                normalizedDictionary[normalizedCode]
            )
        )
    }

    private fun updateCandidates() {
        candidateRow.removeAllViews()

        if (inputMode != InputMode.ZHUYIN) {
            addCandidatePlaceholder()
            return
        }

        val composing = currentZhuyin.toString()
        val candidates = getCandidates(composing)
        if (candidates.isEmpty() && composing.isNotEmpty()) {
            addCandidateItem(composing) { commitText(composing) }
        } else {
            for (word in candidates) {
                addCandidateItem(word) { commitText(word) }
            }
        }

        if (candidateRow.childCount == 0) {
            addCandidatePlaceholder()
        }
        candidateScroll.post { candidateScroll.scrollTo(0, 0) }
    }

    private fun addCandidateItem(label: String, onClick: () -> Unit) {
        val tv = TextView(this).apply {
            text = label
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
            setTextColor(Color.GREEN)
            setPadding(dp(20), dp(8), dp(20), dp(8))
            setOnClickListener { onClick() }
        }
        candidateRow.addView(tv)
    }

    private fun addCandidatePlaceholder() {
        val placeholder = TextView(this).apply {
            text = " "
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
            setTextColor(Color.GREEN)
            setPadding(dp(20), dp(8), dp(20), dp(8))
            alpha = 0f
        }
        candidateRow.addView(placeholder)
    }

    private fun recordChineseSelection(text: String) {
        if (!isChineseText(text)) {
            return
        }
        val updatedCount = (chineseSelectionCounts[text] ?: 0) + 1
        chineseSelectionCounts[text] = updatedCount
        selectionCountPrefs.edit().putInt(SELECTION_COUNT_PREFIX + text, updatedCount).apply()
    }

    private fun loadChineseSelectionCounts(): MutableMap<String, Int> {
        val loaded = mutableMapOf<String, Int>()
        for ((key, value) in selectionCountPrefs.all) {
            if (!key.startsWith(SELECTION_COUNT_PREFIX)) {
                continue
            }
            val candidate = key.removePrefix(SELECTION_COUNT_PREFIX)
            val count = value as? Int ?: continue
            if (count > 0 && isChineseText(candidate)) {
                loaded[candidate] = count
            }
        }
        return loaded
    }

    private fun sortChineseCandidatesBySelectionCount(candidates: List<String>): List<String> {
        val chineseCandidates = candidates.withIndex().filter { isChineseText(it.value) }
        if (chineseCandidates.size <= 1) {
            return candidates
        }

        val sortedChineseValues = chineseCandidates
            .sortedWith(
                compareByDescending<IndexedValue<String>> { chineseSelectionCounts[it.value] ?: 0 }
                    .thenBy { it.index }
            )
            .map { it.value }

        var chineseCursor = 0
        return candidates.map { candidate ->
            if (isChineseText(candidate)) {
                sortedChineseValues[chineseCursor++]
            } else {
                candidate
            }
        }
    }

    private fun isChineseText(text: String): Boolean {
        if (text.isBlank()) {
            return false
        }
        var offset = 0
        while (offset < text.length) {
            val codePoint = Character.codePointAt(text, offset)
            if (!isChineseCodePoint(codePoint)) {
                return false
            }
            offset += Character.charCount(codePoint)
        }
        return true
    }

    private fun isChineseCodePoint(codePoint: Int): Boolean {
        return codePoint in 0x3400..0x4DBF ||
            codePoint in 0x4E00..0x9FFF ||
            codePoint in 0xF900..0xFAFF ||
            codePoint in 0x20000..0x2A6DF ||
            codePoint in 0x2A700..0x2B73F ||
            codePoint in 0x2B740..0x2B81F ||
            codePoint in 0x2B820..0x2CEAF
    }

    private fun addControlButton(
        row: LinearLayout,
        label: String,
        weight: Float,
        enabled: Boolean = true,
        action: () -> Unit
    ) {
        val button = Button(this).apply {
            text = label
            isAllCaps = false
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setTextColor(if (enabled) Color.GREEN else Color.rgb(0, 110, 0))
            setBackgroundColor(Color.rgb(30, 30, 30))
            alpha = if (enabled) 1.0f else 0.8f
            setOnClickListener { action() }
        }
        row.addView(button, LinearLayout.LayoutParams(0, dp(54), weight))
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun loadDictionaryFromAssets(): Map<String, List<String>> {
        val parsed = mutableMapOf<String, List<String>>()
        return try {
            assets.open("zhuyin_dict.tsv").bufferedReader(Charsets.UTF_8).useLines { lines ->
                lines.forEach { line ->
                    val separator = line.indexOf('\t')
                    if (separator <= 0 || separator >= line.length - 1) {
                        return@forEach
                    }
                    val code = line.substring(0, separator)
                    val candidates = line.substring(separator + 1)
                        .map { it.toString() }
                        .distinct()
                    if (candidates.isNotEmpty()) {
                        parsed[code] = candidates
                    }
                }
            }
            parsed
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun buildNormalizedDictionary(vararg sources: Map<String, List<String>>): Map<String, List<String>> {
        val merged = mutableMapOf<String, MutableList<String>>()
        for (source in sources) {
            for ((code, candidates) in source) {
                val normalizedCode = normalizeZhuyin(code)
                val bucket = merged.getOrPut(normalizedCode) { mutableListOf() }
                for (candidate in candidates) {
                    if (candidate !in bucket) {
                        bucket.add(candidate)
                    }
                }
            }
        }
        return merged
    }

    private fun normalizeZhuyin(code: String): String {
        return buildString {
            for (ch in code) {
                if (ch !in TONE_MARKS) {
                    append(ch)
                }
            }
        }
    }

    private fun mergeCandidates(vararg candidateLists: List<String>?): List<String> {
        val merged = LinkedHashSet<String>()
        for (list in candidateLists) {
            if (list.isNullOrEmpty()) {
                continue
            }
            for (candidate in list) {
                if (candidate.isNotBlank()) {
                    merged.add(candidate)
                }
            }
        }
        return merged.toList()
    }
}
