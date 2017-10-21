# Chunk Stories
## A Moddable voxel game 

Chunk Stories is a fully-featured voxel game engine written in Java, aiming to replace Minecraft as a modding framework to build total conversions on. It features almost everything the original game could do, but is built in a modular and open fashion so as to accept modifications naturally and hassle-free.

It features an API, an implementation of that API in the form of client and server executables and everything else is merely mods loaded at runtime. For convinience, a base mod called "core_content" is provided to give content creators a solid base upon which to build the gamemode of their dreams.

#### Why yet another clone ?

It may have occured to you that a while ago, some company of the name 'Microsoft' spent a respectable country's GPD to aquire the rights to the videogame 'Minecraft', to the astonishment of most observers and fears of some faitfull players. More recently their strategy has become increasingly clear, as the proper Minecraft is being renamed 'Java Edition', as a way to slowly but surely phase it out. Replacing it is effectively a port of the C++/C# console remake, exclusive to Windows 10 and while bearing much ressemblance to the actual thing, is absolutely not geared towards modding nor is welcoming to anyone brave enough to go ahead and decompile it.

For the author, Chunk Stories is and always has been a side project, made for fun and for learning to become a better programmer, but with the turns of events it also happens that there is some kind of niche opening-up: frustrated 'Java Edition' Minecraft modders, left homeless in the grand plans of Microsoft, or just unhappy with having to run a glorified spyware as their main OS. Chunk Stories borrows heavily from one of two best things about the Minecraft modding ecosystem: The Bukkit-style of plugins, and the ressource-pack style of mods packaging, adding in it's own ideas and paradigms.

### Technical notes

Chunk Stories is written in Java 8, using LWJGL3 as it's OpenGL wrapper. It uses Gradle in it's wrapper fashion to automate the build process, JOML for mathematical functions, lz4 for chunk compression, jansi for console coloring, for the full list of librairies used, see CREDITS.

Some of the things Chunk Stories is able to do:

 * Fully configurable Voxels, Entities, Items, Packets, Particles and GUI sub-systems
 * Mods can redefine basically any element of the game, and can include custom code that is hotloaded at runtime.
 * A bukkit-inspired plugin system
 * A modern OpenGL 3.3+ renderer, supporting custom shaders and looks like it was made this decade
 * Support for hot-downloading mods upon server connection
 * A Minecraft map importer

### Building & Eclipse setup

####Working on the core engine
Clone the `chunkstories` repo and run `gradlew buildAll` ( `./gradlew buildAll`on unixes ). 
`gradlew client:run`and `gradlew server:run`respectively will compile and launch an up-to-date client or server.

Using Eclipse, do `Import->Gradle Project` and select the directory where you cloned chunkstories in, wait for it to rebuild the project and then you can create a new launch configuration. For the game to work properly, you must set the work directory to the root 'chunkstories' project, use `--dir=.` to tell the game to use the local directory and not .chunkstories, and finally you must use `--core=path/to/core_content.zip` to point to the directory or zipfile containing the base content you wish to use.

####Modifying the core content
Clone the `chunkstories-core` repo and run `gradlew install` ( `./gradlew install`on unixes ). This will build you a copy of the latest official content in `build/distributions/core_content.zip`as well as updating the `res/` folder in the root of the project to include the build .jars, meaning you can use that directory instead of the zipfile when working on your content to iterate quickly.

#### Modifiying the API
Clone the `chunkstories-api` repo and run `gradlew install` ( `./gradlew install`on unixes ). This will build you a copy of the latest API

#### Creating mods and new content

Clone the `chunkstories-template`repo and check out the instructions. It comes with a basic build.gradle configuration file, you just have to edit in your mod name, author, version and description, and run `gradlew mod`to package your mod.

#### What If I just want to play the game ?

You did not quite come to the right place for this. If you're just interested in playing Chunk Stories, you should check out the [Official Website](https://chunkstories.xyz)

### Licensing (wip, ignore)

This project was entirely developed by Hugo "Gobrosse" Devillers on his spare time. The API is licensed under -, meaning anyone can do whatever the fuck they want with it. The core content **code** is released under - too, under the assumption that people won't be morons about it and will learn from it. The core content **assets** that aren't specified otherwise in ATTRIBUTION.md are released under Creative Commons NON-COMMERCIAL ATTRIBUTION REDISTRIBUABLE, meaning you can use them with your mods, hack them up and have fun, but you can't use them in commercial projects nor claim you made them. The implementation code remains strictly private for the moment.