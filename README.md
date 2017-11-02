# Chunk Stories
## A Moddable voxel game  engine

Chunk Stories is a fully-featured voxel game engine written in Java, aiming to replace Minecraft as a modding framework to build mods and total conversions on. It features almost everything the original game could do, but is built in a modular and open fashion so as to accept modifications naturally and hassle-free.

It features an API, an implementation of that API in the form of client and server executables and everything else is merely mods loaded at runtime. A base mod called "core" is bundled to give content creators a solid base upon which to build the gamemode of their dreams.

#### Why yet another clone ?

It may have occured to you that a while ago, some company of the name 'Microsoft' spent a respectable country's GPD to aquire the rights to the videogame 'Minecraft', to the astonishment of most observers and fears of some faitfull players. More recently their strategy has become increasingly clear, as the proper Minecraft is being renamed 'Java Edition', as a way to slowly but surely phase it out. Replacing it is effectively a port of the C++/C# console remake, exclusive to Windows 10 and while bearing much ressemblance to the actual thing, is absolutely not geared towards modding nor is welcoming to anyone brave enough to go ahead and decompile it.

For the author, Chunk Stories is and always has been a side project, made for fun and for learning to become a better programmer, but with the turns of events it also happens that there is some kind of niche opening-up: frustrated 'Java Edition' Minecraft modders, left homeless in the grand plans of Microsoft, or just unhappy with having to run a glorified spyware as their main OS. Chunk Stories borrows heavily from one of two best things about the Minecraft modding ecosystem: The Bukkit-style of plugins, and the ressource-pack style of mods packaging, adding in it's own ideas and paradigms.

### Technical notes

Chunk Stories is written in Java 8, using LWJGL3 as it's OpenGL wrapper. It uses Gradle in it's wrapper fashion to automate the build process, JOML for mathematical functions, lz4 for chunk compression, jansi for console coloring, for the full list of librairies used, see CREDITS.

Here's a bullet points list:

 * Configuration files allow to redefine almost anything using a simplistic syntax
 * Customizable network packets, world generation, inputs, GUI*, fonts, localization, ...
 * Up to 65536 voxel types, with 8-bit metadata and support for [even more data](http://chunkstories.xyz/wiki)
 * Support for items that take up more than 1 slot
 * Component-based entity system
 * Self-wrapping worlds built out of cubical 32³ chunks, (un)loaded explicitly by reference counting
 * Heightmap summaries for far terrain rendering up to 1km
 * Modern-ish renderer in GL3.3 core, supporting deffered rendering, hotloading shaders, instanced rendering, particles, SSR, bloom, you name it
 * Skeleton-based animation system with hitboxes
 * Support for basically loading everything at runtime, including downloading on server connection
 * Works on Windows and Linux ( osx may work but unsupported )

\* some stuff *might* not be quite done right now

### Building & Eclipse setup

####Working on the core engine *(select contributors only)*
Clone the `chunkstories` repo and run `gradlew buildAll` ( `./gradlew buildAll`on unixes ). 
`gradlew client:run`and `gradlew server:run`respectively will compile and launch an up-to-date client or server. The build script takes care of setting up the right arguments for you.

Using Eclipse, do `Import->Gradle Project` and select the directory where you cloned chunkstories in, wait for it to rebuild the project and then you can create a new launch configuration. For the game to work properly, you must set the work directory to the root 'chunkstories' project, use `--dir=.` to tell the game to use the local directory and not .chunkstories, and finally you must use `--core=path/to/core_content.zip` to point to the directory or zipfile containing the base content you wish to use.

####Modifying the core content
Clone the `chunkstories-core` repo and run `gradlew install` ( `./gradlew install`on unixes ). This will build you a copy of the latest official content in `build/distributions/core_content.zip`as well as updating the `res/` folder in the root of the project to include the built .jars, meaning you can use that directory instead of the zipfile when working on your content to iterate quickly.

#### Modifiying the API
Clone the `chunkstories-api` repo and run `gradlew install` ( `./gradlew install`on unixes ). This will build you a copy of the latest API

#### Creating mods and new content

Clone the `chunkstories-template`repo and check out the instructions. It comes with a basic build.gradle configuration file, you just have to edit in your mod name, author, version and description, and run `gradlew mod`to package your mod.

#### What If I just want to play the game ?

You did not quite come to the right place for this. If you're just interested in playing Chunk Stories, you should check out the [Official Website](https://chunkstories.xyz)

### Licensing

This project was entirely developed by Hugo "Gobrosse" Devillers on his spare time. The API is licensed under -, meaning anyone can do whatever the fuck they want with it. 

The core content **code** is released under
The core content **assets** that aren't specified otherwise in ATTRIBUTION.md are released under Creative Commons NON-COMMERCIAL ATTRIBUTION REDISTRIBUABLE, meaning you can use them with your mods, hack them up and have fun, but you can't use them in commercial projects nor claim you made them. 

The implementation code remains private for the moment. It's not as clean, features angry french curse words, bloated build scripts and maybe even an evil bitcoin miner. Maybe not. I'm actually mostly cool with showing the code, but letting people lead my project in another direction isn't what I want, and publishing on github would distract me while not bringing anything to the table. This can of course change once I no longer work on this project "full-free-time"
