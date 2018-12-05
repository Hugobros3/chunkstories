package io.xol.chunkstories.client.commands

import io.xol.chunkstories.api.client.IngameClient

fun installClientCommands(client: IngameClient) {
    ReloadContentCommand(client)
}