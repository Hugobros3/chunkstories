==Chunk Stories Late January / February Roadmap==

I'm back from hell (formally know as the january exams session), so I'm able to get back to working on Chunk Stories !

	Investigate & Fix issues with server running long-term
		- Prevent errors in handling player disconection messing with reclaiming their world data handles ( or else we end up in OOM land )
	Integrate the Bullet physics engine ( using LibGDX's wrapper ) and asess it's viability as a complete physics solution
		- Entity to entity collision is a must-have
	Move away from my custom .obj loader and just use assimp like everyone else
		- Allow voxels to use models too
	[Pending] Investigate legui/other clean gui solutions ( or just clean up my own shit )
		- Expose that GUI to the API
	Make the shader pipeline layout configurable (DSL?)
	Formulate a plan to create a proper UDP-based transport protocol for the game instead of just using TCP
		- Multiplexing multiple channels behind an auth layer, offering both:
		- (Interruptible ?) Streaming of files and map bits
		- Game packets with flags for loss tolerance, out-of-order-delivery tolerance
		- Tie those with server-side world ticks
		- All built atop UDP (with TCP fallback?)
		- Crypto ?
	Release the main engine source code as free software, just like the API and the core content
		- And eventually do so with xolioz as a gamemode ( rebrand it as to keep the name a trademark ? )
	Documentation, documentation, documentation
