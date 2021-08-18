rootProject.name = "chunkstories"

include("api")
project(":api").projectDir = File("../chunkstories-api")

include("common", "server", "client", "converter", "launcher")