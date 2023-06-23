rootProject.name = "chunkstories"

// haha what a fucking joke
// https://stackoverflow.com/questions/18676734/gradle-recursive-subprojects
include("api", "enklume")
project(":enklume").projectDir = File("api/enklume")

include("common", "server", "client", "converter", "launcher")