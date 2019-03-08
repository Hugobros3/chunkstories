//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.server.commands

import xyz.chunkstories.api.server.Server
import xyz.chunkstories.server.commands.admin.*
import xyz.chunkstories.server.commands.debug.*
import xyz.chunkstories.server.commands.player.*
import xyz.chunkstories.server.commands.system.InfoCommands
import xyz.chunkstories.server.commands.system.ListPlayersCommand
import xyz.chunkstories.server.commands.world.SpawnEntityCommand
import xyz.chunkstories.server.commands.world.TimeCommand
import xyz.chunkstories.server.commands.world.WeatherCommand

fun installServerCommands(server: Server) {

    // Administration
    ConfigCommands(server)
    ModerationCommands(server)
    SaveCommand(server)
    SayCommand(server)
    StopServerCommand(server)

    // Debug
    DebugIOCommand(server)
    DebugTasksCommand(server)
    DebugWorldDataCommands(server)
    EntitiesDebugCommands(server)
    MiscDebugCommands(server)

    // Player
    ClearCommand(server)
    CreativeCommand(server)
    FlyCommand(server)
    GiveCommand(server)
    HealthCommand(server)
    SpawnCommand(server)
    TpCommand(server)

    // System
    InfoCommands(server)
    ListPlayersCommand(server)

    // World
    SpawnEntityCommand(server)
    TimeCommand(server)
    WeatherCommand(server)
}
