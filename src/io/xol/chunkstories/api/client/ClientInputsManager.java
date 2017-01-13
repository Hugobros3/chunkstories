package io.xol.chunkstories.api.client;

import io.xol.chunkstories.api.input.Input;
import io.xol.chunkstories.api.input.InputsManager;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface ClientInputsManager extends InputsManager
{
	public boolean onInputPressed(Input input);

	public boolean onInputReleased(Input input);
}
