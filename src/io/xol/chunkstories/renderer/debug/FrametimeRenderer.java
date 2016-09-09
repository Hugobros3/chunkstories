package io.xol.chunkstories.renderer.debug;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import io.xol.chunkstories.api.rendering.ShaderInterface;
import io.xol.chunkstories.api.rendering.RenderingInterface.Primitive;
import io.xol.engine.base.GameWindowOpenGL;
import io.xol.engine.graphics.GLCalls;
import io.xol.engine.graphics.RenderingContext;
import io.xol.engine.graphics.geometry.VertexFormat;
import io.xol.engine.graphics.geometry.VerticesObject;
import io.xol.engine.graphics.shaders.ShaderProgram;
import io.xol.engine.graphics.shaders.ShadersLibrary;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class FrametimeRenderer
{
	//Draws a fps graph of frameTime
	
	static FloatBuffer data = BufferUtils.createFloatBuffer(4 * 1000);
	static VerticesObject dataGpu = new VerticesObject();
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

		renderingContext.useShader("fps_graph");
		//ShaderProgram overlayProgram = ShadersLibrary.getShaderProgram("fps_graph");
		ShaderInterface overlayProgram = renderingContext.currentShader();
		//overlayProgram.use(true);
		overlayProgram.setUniform1f("currentTiming", lel);
		overlayProgram.setUniform2f("screenSize", GameWindowOpenGL.windowWidth, GameWindowOpenGL.windowHeight);
		//System.out.println(XolioWindow.frameW);
		
		//int vertexIn = overlayProgram.getVertexAttributeLocation("vertexIn");
		//System.out.println("ntm"+vertexIn);
		//renderingContext.enableVertexAttribute(vertexIn);
		//renderingContext.setVertexAttributePointerLocation(vertexIn, 3, GL_FLOAT, false, 0, 0);
		data.rewind();
		dataGpu.uploadData(data);
		
		//glBindBuffer(GL_ARRAY_BUFFER, 0);
		
		renderingContext.bindAttribute("vertexIn", dataGpu.asAttributeSource(VertexFormat.FLOAT, 2));
		renderingContext.draw(Primitive.LINE, 0, 2000);
		//renderingContext.setVertexAttributePointerLocation(vertexIn, 2, false, 0, data);
		//GLCalls.drawArrays(GL_LINES, 0, 2000);
		
		//renderingContext.disableVertexAttribute(vertexIn);
	}
}
