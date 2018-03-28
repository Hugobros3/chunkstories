package io.xol.chunkstories.mesh;

import java.io.BufferedReader;

import assimp.IOStream;
import assimp.IOSystem;
import io.xol.chunkstories.api.content.Asset;
import io.xol.chunkstories.api.content.Content;

public class AssetIOSystem implements IOSystem {

	final Content content;
	public AssetIOSystem(Content content) {
		this.content = content;
	}

	@Override
	public void Close(IOStream arg0) {
		//osef
	}

	@Override
	public boolean Exists(String arg0) {
		return	content.getAsset(arg0) != null;
	}

	@Override
	public IOStream Open(String arg0) {
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
		public BufferedReader read() {
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
