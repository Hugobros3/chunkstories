//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.renderer.mesh;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

import org.joml.Vector4f;
import org.lwjgl.BufferUtils;

import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.StateMachine.CullingMode;
import io.xol.chunkstories.api.rendering.text.TextMesh;
import io.xol.chunkstories.api.rendering.textures.Texture2D;
import io.xol.chunkstories.api.rendering.vertex.Primitive;
import io.xol.chunkstories.api.rendering.vertex.VertexBuffer;
import io.xol.chunkstories.api.rendering.vertex.VertexFormat;
import io.xol.chunkstories.renderer.font.TrueTypeFont;
import io.xol.chunkstories.renderer.font.TrueTypeFontRenderer;
import io.xol.chunkstories.renderer.opengl.vbo.VertexBufferGL;

public class TextMeshObject implements TextMesh
{
	private final String text;
	private final TrueTypeFont font;
	
	private boolean done = false;
	private List<VertexBufferGL> verticesObjects = new LinkedList<VertexBufferGL>();

	public TextMeshObject(TrueTypeFontRenderer ttfRenderer, TrueTypeFont font, String text)
	{
		this.text = text;
		this.font = font;
		
		ttfRenderer.drawStringIngame(font, 0, 0, text, 2.0f, 256, this);
	}

	public List<VertexBufferGL> getVerticesObjects()
	{
		return verticesObjects;
	}

	private ByteBuffer tempBuffer = BufferUtils.createByteBuffer(4 * (3 + 2 + 4) * 6 * 128);
	private int temp = 0;

	private Texture2D currentTexture;
	private Vector4f currentColor;

	public void setState(Texture2D texture, Vector4f color)
	{
		//If we changed texture, render the temp stuff
		//TODO: This is retarded, we don't even record the != textures yet (UTF-8 support bitchez)
		if (currentTexture != null && !texture.equals(currentTexture))
			finalizeTemp();
		
		//Useless as colours are recorded per-vertex.
		//else if (currentColor != null && !color.equals(currentColor))
		//	finalizeTemp();

		currentTexture = texture;
		currentColor = color;
	}

	private void finalizeTemp()
	{
		if (done)
			return;
		if (temp <= 0)
			return;

		VertexBufferGL verticesObject = new VertexBufferGL();
		tempBuffer.flip();
		//System.out.println("Cucking"+temp);
		verticesObject.uploadData(tempBuffer);

		//System.out.println("Added " + verticesObject.getVramUsage() + " bytes worth of verticesObject");
		verticesObjects.add(verticesObject);

		tempBuffer = BufferUtils.createByteBuffer(4 * (3 + 2 + 4) * 6 * 128);
		temp = 0;
	}

	public void drawQuad(float startX, float startY, float width, float height, float textureStartX, float textureStartY, float textureEndX, float textureEndY)
	{
		if (tempBuffer.position() == tempBuffer.capacity())
			finalizeTemp();

		float endX = startX + width;
		float endY = startY + height;

		addVertice(startX, startY, textureStartX, textureStartY);
		addVertice(startX, endY, textureStartX, textureEndY);
		addVertice(endX, endY, textureEndX, textureEndY);

		addVertice(startX, startY, textureStartX, textureStartY);
		addVertice(endX, startY, textureEndX, textureStartY);
		addVertice(endX, endY, textureEndX, textureEndY);

		temp += 6;
	}

	private void addVertice(float startX, float startY, float textureStartX, float textureStartY)
	{
		//Vertex
		tempBuffer.putFloat(startX / 256f - 0.5f);
		tempBuffer.putFloat(startY / 256f - 0.5f);
		tempBuffer.putFloat(0f);
		//Texcoord
		tempBuffer.putFloat(textureStartX);
		tempBuffer.putFloat(textureStartY);
		//Color
		tempBuffer.putFloat(currentColor.x());
		tempBuffer.putFloat(currentColor.y());
		tempBuffer.putFloat(currentColor.z());
		tempBuffer.putFloat(currentColor.w());
		//Normals
		tempBuffer.putFloat(0f);
		tempBuffer.putFloat(0f);
		tempBuffer.putFloat(1f);
	}

	public void done()
	{
		finalizeTemp();
		done = true;
	}

	public void render(RenderingInterface renderingContext)
	{
		//TODO use texture pages
		renderingContext.bindAlbedoTexture(font.glTextures[0]);
		
		renderingContext.setCullingMode(CullingMode.DISABLED);
		//glDisable(GL_CULL_FACE);
		//renderingContext.disableVertexAttribute("normalIn");
		
		//renderingContext.currentShader().setUniform1f("useNormalIn", 0f);
		
		//TODO use texture pages
		for (VertexBuffer verticesObject : verticesObjects)
		{
			int stride = 4 * ( 3 + 2 + 4 + 3);
			renderingContext.bindAttribute("vertexIn", verticesObject.asAttributeSource(VertexFormat.FLOAT, 3, stride, 0));
			renderingContext.bindAttribute("texCoordIn", verticesObject.asAttributeSource(VertexFormat.FLOAT, 2, stride, 4 * 3));
			renderingContext.bindAttribute("colorIn", verticesObject.asAttributeSource(VertexFormat.FLOAT, 4, stride, 4 * ( 3 + 2)));
			renderingContext.bindAttribute("normalIn", verticesObject.asAttributeSource(VertexFormat.FLOAT, 4, stride, 4 * ( 3 + 2 + 4)));
			
			renderingContext.draw(Primitive.TRIANGLE, 0, (int) (verticesObject.getVramUsage() / stride));
		}
		
		//renderingContext.currentShader().setUniform1f("useNormalIn", 1f);
	}

	@Override
	public String getText() {
		return text;
	}

	@Override
	public void destroy() {
		for(VertexBuffer vf : verticesObjects) {
			vf.destroy();
		}
	}
}
