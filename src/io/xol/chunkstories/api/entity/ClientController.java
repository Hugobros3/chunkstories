package io.xol.chunkstories.api.entity;

import io.xol.chunkstories.api.input.KeyBind;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface ClientController extends Controller
{
	public boolean hasFocus();
	
	public KeyBind getKeyBind(String bindName);
}
