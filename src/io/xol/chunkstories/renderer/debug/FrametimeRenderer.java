package io.xol.chunkstories.renderer.debug;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import io.xol.engine.base.XolioWindow;
import io.xol.engine.model.RenderingContext;
import io.xol.engine.shaders.ShaderProgram;
import io.xol.engine.shaders.ShadersLibrary;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class FrametimeRenderer
{
	//Draws a fps graph of frameTime
	
	static FloatBuffer data = BufferUtils.createFloatBuffer(4 * 1000);
	static int lel = 0;
	static long lastTime;
	
	public static void draw(RenderingContext renderingContext)
	{
		lel++;
		lel%=1000;
		long elapsedTime = (System.nanoTime() - lastTime);
		lastTime = System.nanoTime();
		data.put(lel * 4, lel);
		data.put(lel * 4 + 1, 0);
		data.put(lel * 4 + 2, lel);
		data.put(lel * 4 + 3, elapsedTime/1000000f);
		//System.out.println("ntm");
		glLineWidth(1);
		glDisable(GL_DEPTH_TEST);
		glDisable(GL_ALPHA_TEST);
		glDisable(GL_CULL_FACE);
		glDepthFunc(GL11.GL_LEQUAL);
		ShaderProgram overlayProgram = ShadersLibrary.getShaderProgram("fps_graph");
		renderingContext.setCurrentShader(overlayProgram);
		//overlayProgram.use(true);
		overlayProgram.setUniformFloat2("screenSize", XolioWindow.frameW, XolioWindow.frameH);
		//System.out.println(XolioWindow.frameW);
		int vertexIn = overlayProgram.getVertexAttributeLocation("vertexIn");
		//System.out.println("ntm"+vertexIn);
		renderingContext.enableVertexAttribute(vertexIn);
		//glVertexAttribPointer(vertexIn, 3, GL_FLOAT, false, 0, 0);
		data.rewind();
		glBindBuffer(GL_ARRAY_BUFFER, 0);
		glVertexAttribPointer(vertexIn, 2, false, 0, data);
		glDrawArrays(GL_LINES, 0, 2000);
		renderingContext.disableVertexAttribute(vertexIn);
	}
}
