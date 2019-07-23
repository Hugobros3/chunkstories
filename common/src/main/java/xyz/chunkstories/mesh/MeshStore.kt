//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.mesh

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import xyz.chunkstories.api.content.Asset
import xyz.chunkstories.api.content.Content
import xyz.chunkstories.api.content.mods.ModsManager
import xyz.chunkstories.api.exceptions.content.MeshLoadException
import xyz.chunkstories.api.graphics.representation.Model
import xyz.chunkstories.content.GameContentStore

import java.util.HashMap

class MeshStore(gameContentStore: GameContentStore) : Content.Models {

    protected val content: Content
    protected val modsManager: ModsManager

    protected var models: MutableMap<String, Model> = HashMap()

    private val loader: AssimpMeshLoader

    override val defaultModel: Model
        get() = get("models/error.obj")

    init {
        this.content = gameContentStore
        this.loader = AssimpMeshLoader(this)
        this.modsManager = gameContentStore.modsManager
    }

    fun reloadAll() {
        models.clear()
    }

    override fun get(modelName: String): Model {
        var model: Model? = models[modelName]

        if (model == null) {
            val a = modsManager.getAsset(modelName)
            if (a == null) {
                logger().error("model: $modelName not found in assets")
                return defaultModel
            }

            try {
                model = loader.load(a)
            } catch (e: MeshLoadException) {
                e.printStackTrace()
                logger().error("Model " + modelName + " couldn't be load using " + loader.javaClass.name + ", stack trace above.")
                return defaultModel
            }

            models[modelName] = model
        }

        return model ?: defaultModel
    }

    fun parent(): Content {
        return content
    }

    fun logger(): Logger {
        return logger
    }

    override fun getOrLoadModel(s: String): Model {
        return get(s)
    }

    companion object {

        private val logger = LoggerFactory.getLogger("content.meshes")
    }
}
