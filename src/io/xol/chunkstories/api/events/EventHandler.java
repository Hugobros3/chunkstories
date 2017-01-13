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

}
