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
import xyz.chunkstories.api.particles.ParticleType
import xyz.chunkstories.api.particles.ParticleTypeDefinition
import xyz.chunkstories.content.GameContentStore
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

            val json = JsonValue.readHjson(a.reader()).toString()
            val map = gson.fromJson(json, LinkedTreeMap::class.java) as LinkedTreeMap<Any?, Any?>

            val materialsTreeMap = map["particles"] as LinkedTreeMap<*, *>

            for (definition in materialsTreeMap.entries) {
                val name = definition.key as String
                val properties = (definition.value as LinkedTreeMap<String, *>).extractProperties()

                properties["name"] = name

                try {
                    val particleTypeDefinition = ParticleTypeDefinition(this, name, properties)

                    val className = particleTypeDefinition.resolveProperty("class") ?: throw Exception("no 'class' property set")
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
        /*try (BufferedReader reader = new BufferedReader(f.reader());) {
			String line = "";
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("#")) {
					// It's a comment, ignore.
				} else {
					if (line.startsWith("end")) {
						logger().warn("Syntax error in file : " + f + " : ");
						continue;
					}
					String splitted[] = line.split(" ");
					if (splitted.length == 2 && splitted[0].startsWith("particle")) {
						// int id = Integer.parseInt(splitted[2]);
						String particleName = splitted[1];

						try {
							ParticleTypeDefinitionImplementation type = new ParticleTypeDefinitionImplementation(this,
									particleName, reader);
							ParticleTypeHandler handler = type.handler();

							particleTypesByName.put(particleName, handler);
							// particleTypesById.put(id, handler);

						} catch (IllegalParticleDeclarationException e) {
							this.store.getContext().logger()
									.error("Could not load particle type " + particleName + " : \n" + e.getMessage());
							e.printStackTrace();
						}
					}
				}
			}
		} catch (IOException e) {
			logger().warn(e.getMessage());
		}*/
    }

    override fun <T : ParticleType.Particle> getParticleType(string: String): ParticleType<T>? {
        return particleTypesByName[string] as? ParticleType<T>
    }

    override val all: Collection<ParticleType<*>>
        get() = particleTypesByName.values
}
