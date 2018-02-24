//
// This file is a part of the Chunk Stories API codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.engine.misc;

public class ConnectionStep {
	String text;
	public ConnectionStep(String text) {
		this.text = text;
	}
	public String getStepText() {
		return text;
	}
	public void setStepText(String text) {
		this.text = text;
	}
	public void waitForEnd() {
		return;
	}
}