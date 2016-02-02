package io.xol.chunkstories.item;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface ItemStorage
{
	public void load(InputStream is) throws IOException;
	
	public void save(OutputStream os) throws IOException;
	
}
