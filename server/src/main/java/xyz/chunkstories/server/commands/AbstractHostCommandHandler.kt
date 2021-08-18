//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//
package xyz.chunkstories.server.commands

import xyz.chunkstories.api.plugin.commands.CommandHandler
import xyz.chunkstories.api.server.Host

abstract class AbstractHostCommandHandler(open val host: Host) : CommandHandler