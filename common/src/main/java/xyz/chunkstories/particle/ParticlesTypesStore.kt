//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.particle

import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import org.hjson.JsonValue
import org.slf4j.LoggerFactory
import xyz.chunkstories.api.content.Asset
import xyz.chunkstories.api.content.Content
import xyz.chunkstories.api.content.json.asDict
import xyz.chunkstories.api.content.json.asString
import xyz.chunkstories.api.particles.ParticleType
import xyz.chunkstories.api.particles.ParticleTypeDefinition
import xyz.chunkstories.content.GameContentStore
import xyz.chunkstories.content.eat
import xyz.chunkstories.content.extractProperties
import java.util.*

class ParticlesTypesStore(override val parent: GameContentStore) : Content.ParticlesTypes {
    override val logger = LoggerFactory.getLogger("content.particles")

    private val particleTypesByName = HashMap<String, ParticleType<*>>()

    fun reload() {
        particleTypesByName.clear()

        val gson = Gson()

        fun readDefinitions(a: Asset) {
            logger.debug("Reading particle definitions in : $a")

            val json = JsonValue.readHjson(a.reader()).eat().asDict ?: throw Exception("This json isn't a dict")
            val dict = json["particles"].asDict ?: throw Exception("This json doesn't contain an 'particles' dict")

            for (element in dict.elements) {
                val name = element.key
                val properties = element.value.asDict ?: throw Exception("Definitions have to be dicts")

                try {
                    val particleTypeDefinition = ParticleTypeDefinition(this, name, properties)

                    val className = particleTypeDefinition["class"].asString ?: throw Exception("no 'class' property set")
                    val klass = parent.modsManager.getClassByName(className) ?: throw Exception("Class $className not found")
                    val constructor = klass.getConstructor(ParticleTypeDefinition::class.java)
                            ?: throw Exception("$className doesn't have the right constructor")

                    val particleType = constructor.newInstance(particleTypeDefinition) as ParticleType<*>
                    particleTypesByName.put(name, particleType)

                    logger.debug("Loaded particle type $particleType")
                } catch (e: Exception) {
                    logger.error("Can't load particle $name: ${e.message}")
                }
            }
        }

        for (asset in parent.modsManager.allAssets.filter { it.name.startsWith("particles/") && it.name.endsWith(".hjson") }) {
            readDefinitions(asset)
        }
    }

    private fun loadParticlesFile(f: Asset?) {
        if (f == null)
            return
        throw UnsupportedOperationException("TODO")
    }

    override fun <T : ParticleType.Particle> getParticleType(string: String): ParticleType<T>? {
        return particleTypesByName[string] as? ParticleType<T>
    }

    override val all: Collection<ParticleType<*>>
        get() = particleTypesByName.values
}
