package io.xol.chunkstories.api.events;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)

public @interface EventHandler
{
	/** Good ol' Bukkit-style priority handling */
	EventPriority priority() default EventPriority.NORMAL;
	
	public enum EventPriority {
		LOWEST,
		LOW,
		NORMAL,
		HIGH,
		HIGHER,
		HIGHEST;
	}
	
	/** Should we listen for child events ( extended classes of the event ) ? 
	 *  Example use case: a voxel protection plugin that bans touching a location by any means with a single event, unlike what we used to have to do*/
	ListenToChildEvents listenToChildEvents() default  ListenToChildEvents.NO;
	
	public enum ListenToChildEvents {
		/** Only care about this very event **/ NO,
		/** Only do for 1st class extended classes */ YES,
		/** Care about every single child event class */ RECURSIVE;
	}
}