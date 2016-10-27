package io.xol.chunkstories.api.rendering;

import io.xol.chunkstories.api.exceptions.AttributeNotPresentException;
import io.xol.chunkstories.api.exceptions.InvalidShaderException;
import io.xol.chunkstories.api.exceptions.ShaderCompileException;
import io.xol.chunkstories.api.rendering.lightning.Light;
import io.xol.chunkstories.api.rendering.pipeline.AttributeSource;
import io.xol.chunkstories.api.rendering.pipeline.AttributesConfiguration;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration;
import io.xol.chunkstories.api.rendering.pipeline.ShaderInterface;
import io.xol.chunkstories.api.rendering.pipeline.TexturingConfiguration;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.BlendMode;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.CullingMode;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.DepthTestMode;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.PolygonFillMode;

//TODO: make interface for those
import io.xol.engine.graphics.fonts.TrueTypeFontRenderer;
import io.xol.engine.graphics.textures.Cubemap;
import io.xol.engine.graphics.textures.Texture1D;
import io.xol.engine.graphics.textures.Texture2D;
import io.xol.engine.graphics.util.GuiRenderer;

import io.xol.engine.math.lalgb.Matrix4f;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface RenderingInterface
{
	public CameraInterface getCamera();

	public boolean isThisAShadowPass();
	
	/* shaders */
	
	public ShaderInterface useShader(String shaderName) throws InvalidShaderException, ShaderCompileException;
	
	public ShaderInterface currentShader();
	
	/* Texturing configuration */
	
	public TexturingConfiguration getTexturingConfiguration();
	
	public TexturingConfiguration bindAlbedoTexture(Texture2D texture);
	
	public TexturingConfiguration bindNormalTexture(Texture2D texture);
	
	public TexturingConfiguration bindMaterialTexture(Texture2D texture);

	public TexturingConfiguration bindTexture1D(String textureSamplerName, Texture1D texture);
	
	public TexturingConfiguration bindTexture2D(String textureSamplerName, Texture2D texture);
	
	public TexturingConfiguration bindCubemap(String cubemapSamplerName, Cubemap cubemapTexture);
	
	/* Object location */
	
	/**
	 * returns the current object matrix
	 */
	public Matrix4f getObjectMatrix();
	
	/**
	 * Feeds the 'objectMatrix' and 'objectMatrixNormal' shader inputs ( either uniform or texture-based instanced if shader has support )
	 */
	public Matrix4f setObjectMatrix(Matrix4f objectMatrix);
	
	/**
	 * Feeds the 'worldLight' shader inputs ( either uniform or texture-based instanced if shader has support )
	 */
	public void setWorldLight(int sunLight, int blockLight);
	
	/* Pipeline configuration */
	
	/**
	 * @return The current PipelineConfiguration
	 */
	public PipelineConfiguration getPipelineConfiguration();

	public PipelineConfiguration setDepthTestMode(DepthTestMode depthTestMode);
	
	public PipelineConfiguration setCullingMode(CullingMode cullingMode);

	public PipelineConfiguration setBlendMode(BlendMode blendMode);

	public PipelineConfiguration setPolygonFillMode(PolygonFillMode polygonFillMode);
	
	/* Attributes */
	
	/**
	 * Returns the configuration of the bound vertex shader inputs
	 */
	public AttributesConfiguration getAttributesConfiguration();
	
	/**
	 * If attributeSource != null, setups the currently bound vertex shader attribute input 'attributeName' with it
	 * If attibuteSource == null, disables the shader input 'attributeName'
	 * @throws AttributeNotPresentException If 'attributeName' doesn't resolve to a real attribute location
	 * Returns the configuration of the bound vertex shader inputs
	 */
	public AttributesConfiguration bindAttribute(String attributeName, AttributeSource attributeSource) throws AttributeNotPresentException;
	
	/**
	 * Ensures no attributes are bound left over from previous draw instructions
	 * @return
	 */
	public AttributesConfiguration unbindAttributes();
	
	/**
	 * Draws N primitives made of 'count' vertices, offset at vertice 'startAt', using data specified in the AttributesConfiguration
	 * @return Returns a RenderingCommand object, containing a snapshot of the current state of the RenderingInterface and adds it to the rendering queue
	 */
	public RenderingCommand draw(Primitive primitive, int startAt, int count);
	
	/**
	 * Executes ALL commands in the queue up to this point before continuing
	 */
	public void flush();
	
	public default long getTotalVramUsage()
	{
		return getVertexDataVramUsage() + getTextureDataVramUsage();
	}
	
	public long getVertexDataVramUsage();
	
	public long getTextureDataVramUsage();

	public void addLight(Light light);

	public GuiRenderer getGuiRenderer();

	public TrueTypeFontRenderer getTrueTypeFontRenderer();
}
