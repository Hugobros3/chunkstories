//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.world;

public class WorldLoadingException extends Exception {

	private static final long serialVersionUID = -7131921980416653390L;

	public WorldLoadingException(String message, Throwable cause) {
		super(message, cause);
	}

	@Override
	public String getMessage() {
		if(getCause() != null)
			return super.getMessage()+": "+getCause().getClass().getSimpleName()+"\n"+getCause().getMessage();
		return super.getMessage();
	}

	public WorldLoadingException(String message) {
		super(message);
	}

}
