//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.mesh;

import java.io.BufferedReader;
import java.io.InputStream;

import assimp.IOStream;
import assimp.IOSystem;
import io.xol.chunkstories.api.content.Asset;
import io.xol.chunkstories.api.content.Content;

/** Helper class to translate chunkstories virtual FS into something assimp understands */
public class AssetIOSystem implements IOSystem {

	final Content content;
	public AssetIOSystem(Content content) {
		this.content = content;
	}

	@Override
	public void close(IOStream arg0) {
		//osef
	}

	@Override
	public boolean exists(String arg0) {
		return	content.getAsset(arg0) != null;
	}

	@Override
	public IOStream open(String arg0) {
		Asset asset = content.getAsset(arg0);
		return new AssetIOStream(asset);
	}
	
	class AssetIOStream implements IOStream {
		final Asset a;

		public AssetIOStream(Asset a) {
			this.a = a;
		}

		@Override
		public String getFilename() {
			String n = a.getName();
			int i = n.lastIndexOf("/");
			return i != -1 ? n.substring(i + 1) : n;
		}

		@Override
		public String getPath() {
			return a.getName();
		}

		@Override
		public InputStream read() {
			//System.out.println("read:"+a);
			//Thread.dumpStack();
			return a.read();
		}

		@Override
		public BufferedReader reader() {
			//System.out.println("reader:"+a);
			//Thread.dumpStack();
			return new BufferedReader(a.reader());
		}

		@Override
		public String parentPath() {
			String n = a.getName();
			int i = n.lastIndexOf("/");
			return i == -1 ? "." : n.substring(0, i);
		}
	}

	@Override
	public String getOsSeperator() {
		return "/";
	}

}
