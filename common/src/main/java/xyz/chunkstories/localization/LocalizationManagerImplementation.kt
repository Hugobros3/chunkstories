//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//
package xyz.chunkstories.localization

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import xyz.chunkstories.api.content.Asset
import xyz.chunkstories.api.content.Content
import xyz.chunkstories.api.content.Content.LocalizationManager
import xyz.chunkstories.api.content.Content.Translation
import xyz.chunkstories.api.content.mods.ModsManager
import xyz.chunkstories.content.GameContentStore
import java.util.*

class LocalizationManagerImplementation(// This class holds static model info
        private val store: GameContentStore, private val defaultTranslation: String) : LocalizationManager {

    private val modsManager: ModsManager = store.modsManager
    private val translations: MutableMap<String, Asset> = HashMap()
    private var activeTranslation: Translation? = null

    override fun loadTranslation(translationCode: String) {
        activeTranslation = ActualTranslation(translations[translationCode])
    }

    override fun reload() {
        translations.clear()
        for (asset in modsManager.getAllAssetsByPrefix("lang/")) {
            if (asset.name.endsWith("lang.info")) {
                val abrigedName = asset.name.substring(5, asset.name.length - 10)
                if (abrigedName.contains("/")) continue
                translations[abrigedName] = asset
            }
        }
        activeTranslation = if (activeTranslation != null) ActualTranslation((activeTranslation as ActualTranslation).a) else ActualTranslation(translations[defaultTranslation])
    }

    inner class ActualTranslation(var a: Asset?) : Translation {
        private val strings: MutableMap<String, String> = HashMap()

        override fun getLocalizedString(stringName: String): String {
            val locStr = strings[stringName]
            return locStr ?: "#{$stringName}"
        }

        override fun localize(text: String): String {
            val array = text.toCharArray()
            val sb = StringBuilder()
            var i = 0
            while (i < array.size) {
                val c = array[i]
                if (c == '#') {
                    if (i < array.size - 1 && array[i + 1] == '{') {
                        val endIndex = text.indexOf("}", i + 1)
                        val word = text.substring(i + 2, endIndex)
                        // System.out.println("Found word: "+word);
                        val translated = getLocalizedString(word)
                        sb.append(translated ?: word)
                        i += word.length + 2
                    } else sb.append(c)
                } else sb.append(c)
                i++
            }
            return sb.toString()
        }

        init {
            Companion.logger.info("Loading translation from asset asset: $a")
            val prefix = a!!.name.substring(0, a!!.name.length - 9)
            for (asset in modsManager.getAllAssetsByPrefix(prefix)) {
                if (asset.name.endsWith(".lang")) {
                    for (line in asset.reader().readText().lines()) {
                        val name = line.split(" ").toTypedArray()[0]
                        val indexOf = line.indexOf(" ")
                        if (indexOf == -1)
                            continue
                        var text = line.substring(indexOf + 1)
                        text = text.replace("\\n", "\n")

                        strings[name] = text
                    }
                }
            }
        }
    }

    override fun getLocalizedString(stringName: String): String {
        return activeTranslation!!.getLocalizedString(stringName)
    }

    override fun localize(text: String): String {
        return activeTranslation!!.localize(text)
    }

    override fun parent(): Content {
        return store
    }

    override fun listTranslations(): Collection<String> {
        return translations.keys
    }

    override val logger: Logger
        get() = Companion.logger

    companion object {
        private val logger = LoggerFactory.getLogger("content.localization")
    }
}