package xyz.chunkstories.graphics

import xyz.chunkstories.api.graphics.GraphicsEngine
import xyz.chunkstories.api.graphics.systems.dispatching.RepresentationsProvider

class RepresentationsProvidersImplem : GraphicsEngine.RepresentationsProviders {
    val providers = mutableSetOf<RepresentationsProvider<*>>()

    override fun registerProvider(representationsProvider: RepresentationsProvider<*>) {
        providers.add(representationsProvider)
    }

    override fun unregisterProvider(representationsProvider: RepresentationsProvider<*>) {
        providers.remove(representationsProvider)
    }
}