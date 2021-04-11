//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package xyz.chunkstories.server.commands

import xyz.chunkstories.api.server.Host
import xyz.chunkstories.server.DedicatedServer
import xyz.chunkstories.server.commands.admin.*
import xyz.chunkstories.server.commands.debug.*
import xyz.chunkstories.server.commands.player.*
import xyz.chunkstories.server.commands.system.InfoCommands
import xyz.chunkstories.server.commands.system.ListPlayersCommand
import xyz.chunkstories.server.commands.world.SpawnEntityCommand
import xyz.chunkstories.server.commands.world.TimeCommand
import xyz.chunkstories.server.commands.world.WeatherCommand

fun installHostCommands(host: Host) {
    if(host is DedicatedServer) {
        ServerConfigCommands(host)
        StopServerCommand(host)
    }

    ModerationCommands(host)
    SaveCommand(host)
    SayCommand(host)

    // Debug
    DebugIOCommand(host)
    DebugTasksCommand(host)
    DebugWorldDataCommands(host)
    EntitiesDebugCommands(host)
    MiscDebugCommands(host)

    // Player
    ClearCommand(host)
    CreativeCommand(host)
    FlyCommand(host)
    GiveCommand(host)
    HealthCommand(host)
    SpawnCommand(host)
    TpCommand(host)

    // System
    InfoCommands(host)
    ListPlayersCommand(host)

    // World
    SpawnEntityCommand(host)
    TimeCommand(host)
    WeatherCommand(host)
}
