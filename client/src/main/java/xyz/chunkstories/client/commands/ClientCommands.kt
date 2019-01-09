package xyz.chunkstories.client.commands

import xyz.chunkstories.api.client.IngameClient

fun installClientCommands(client: IngameClient) {
    ReloadContentCommand(client)
}