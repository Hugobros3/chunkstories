package io.xol.engine.misc;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

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