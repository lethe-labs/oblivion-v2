package oblivion.v2.core.voice

import android.content.Context
import android.content.res.AssetManager
import oblivion.v2.core.log.SecLog
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

class VoiceRecognizer(private val appCtx: Context) {
    private var speechService: SpeechService? = null
    private var model: Model? = null
    private var recognizer: Recognizer? = null

    fun unpackModel(language: String = "fr"): String {
        val assetDir = "model-$language"
        val target = File(appCtx.filesDir, assetDir)
        val marker = File(target, MARKER_FILE)

        if (target.exists() && marker.exists()) {
            SecLog.d(TAG, "unpackModel($language) → already unpacked at ${target.absolutePath}")
            return target.absolutePath
        }

        SecLog.d(TAG, "unpackModel($language) → copying assets/$assetDir → ${target.absolutePath}")
        target.deleteRecursively()
        copyAssetDir(appCtx.assets, assetDir, target)

        if (!marker.exists()) {
            throw IllegalStateException(
                "Model unpack failed: ${marker.absolutePath} not found after copy. " +
                    "Check that assets/$assetDir/ contains model files."
            )
        }
        SecLog.d(TAG, "unpackModel($language) OK")
        return target.absolutePath
    }

    @Throws(Exception::class)
    fun start(
        modelPath: String,
        phrase: String,
        strict: Boolean,
        onPhraseDetected: () -> Unit,
    ) {
        if (speechService != null) {
            SecLog.d(TAG, "start() called while already running → ignore")
            return
        }
        SecLog.d(TAG, "start() modelPath=$modelPath phrase='$phrase' strict=$strict")

        val normalizedPhrase = stripAccents(normalize(phrase))
        SecLog.d(TAG, "normalizedPhrase='$normalizedPhrase'")
        val m = Model(modelPath).also { model = it }
        val r = Recognizer(m, SAMPLE_RATE_F).also { recognizer = it }

        val listener = object : RecognitionListener {
            @Volatile private var fired = false

            override fun onPartialResult(hypothesis: String?) {
                if (fired) return
                val text = extractPartial(hypothesis).orEmpty()
                if (text.isNotBlank()) {
                    SecLog.d(TAG, "partial: '$text'")
                }

                if (!strict) checkMatch(text)
            }

            override fun onResult(hypothesis: String?) {
                if (fired) return
                val text = extractText(hypothesis).orEmpty()
                if (text.isNotBlank()) {
                    SecLog.d(TAG, "result: '$text'")
                }
                checkMatch(text)
            }

            override fun onFinalResult(hypothesis: String?) {
                if (fired) return
                val text = extractText(hypothesis).orEmpty()
                if (text.isNotBlank()) {
                    SecLog.d(TAG, "final: '$text'")
                }
                checkMatch(text)
            }

            override fun onError(exception: Exception?) {
                if (exception != null) {
                    SecLog.e(TAG, "Vosk error", exception)
                } else {
                    SecLog.e(TAG, "Vosk error (no exception provided)")
                }
            }

            override fun onTimeout() {}

            private fun checkMatch(rawText: String) {
                val text = stripAccents(normalize(rawText))
                if (text.isEmpty()) return

                val matched = text.contains(normalizedPhrase)
                if (matched) {
                    fired = true
                    SecLog.d(TAG, "PHRASE MATCH! rawText='$rawText' normalized='$text' target='$normalizedPhrase'")
                    onPhraseDetected()
                }
            }
        }

        speechService = SpeechService(r, SAMPLE_RATE_F).also {
            it.startListening(listener)
        }
    }

    fun stop() {
        SecLog.d(TAG, "stop()")
        runCatching { speechService?.stop() }
        runCatching { speechService?.shutdown() }
        speechService = null
        runCatching { recognizer?.close() }
        recognizer = null
        runCatching { model?.close() }
        model = null
    }

    fun isRunning(): Boolean = speechService != null

    private fun copyAssetDir(am: AssetManager, assetPath: String, targetDir: File) {
        val entries = am.list(assetPath)
        if (entries.isNullOrEmpty()) {
            copyAssetFile(am, assetPath, targetDir)
            return
        }

        targetDir.mkdirs()
        for (entry in entries) {
            val childAsset = "$assetPath/$entry"
            val childTarget = File(targetDir, entry)

            val sub = am.list(childAsset)
            if (!sub.isNullOrEmpty()) {
                copyAssetDir(am, childAsset, childTarget)
            } else {
                copyAssetFile(am, childAsset, targetDir)
            }
        }
    }

    private fun copyAssetFile(am: AssetManager, assetPath: String, destDir: File) {
        destDir.mkdirs()
        val fileName = assetPath.substringAfterLast('/')
        val destFile = File(destDir, fileName)
        var input: InputStream? = null
        var output: FileOutputStream? = null
        try {
            input = am.open(assetPath)
            output = FileOutputStream(destFile)
            val buf = ByteArray(8192)
            var len: Int
            while (input.read(buf).also { len = it } != -1) {
                output.write(buf, 0, len)
            }
            SecLog.d(TAG, "copied $assetPath → ${destFile.absolutePath} (${destFile.length()} bytes)")
        } finally {
            runCatching { input?.close() }
            runCatching { output?.close() }
        }
    }

    private fun extractPartial(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        return runCatching { JSONObject(raw).optString("partial", "") }.getOrNull()
    }

    private fun extractText(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        return runCatching { JSONObject(raw).optString("text", "") }.getOrNull()
    }

    private fun normalize(s: String): String =
        s.lowercase()
            .replace(PUNCTUATION_REGEX, "")
            .trim()
            .replace(WHITESPACE_REGEX, " ")

    private fun stripAccents(s: String): String {
        val normalized = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFD)
        return DIACRITICS_REGEX.replace(normalized, "")
    }

    companion object {
        private const val TAG = "VoiceRecognizer"
        private const val SAMPLE_RATE_F = 16_000f

        private const val MARKER_FILE = "final.mdl"
        private val WHITESPACE_REGEX = Regex("\\s+")
        private val PUNCTUATION_REGEX = Regex("[^\\p{L}\\p{N}\\s]")
        private val DIACRITICS_REGEX = Regex("\\p{InCombiningDiacriticalMarks}+")
    }
}
