package io.xol.chunkstories.api.entity;

import io.xol.chunkstories.api.entity.components.Subscriber;
import io.xol.chunkstories.api.input.InputsManager;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

/**
 * The Controller is a special subscriber that don't receive normal tracking updates and can push changes to the controlled entity
 */
public interface Controller extends Subscriber
{
	public InputsManager getInputsManager();
}
