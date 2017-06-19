package io.xol.chunkstories.api.rendering;

import io.xol.chunkstories.api.client.ClientContent.TexturesLibrary;
import io.xol.chunkstories.api.client.ClientInterface;
import io.xol.chunkstories.api.client.ClientRenderingConfig;
import io.xol.chunkstories.api.exceptions.rendering.AttributeNotPresentException;
import io.xol.chunkstories.api.exceptions.rendering.InvalidShaderException;
import io.xol.chunkstories.api.exceptions.rendering.ShaderCompileException;
import io.xol.chunkstories.api.math.Matrix4f;
import io.xol.chunkstories.api.rendering.lightning.Light;
import io.xol.chunkstories.api.rendering.mesh.ClientMeshLibrary;
import io.xol.chunkstories.api.rendering.pipeline.AttributesConfiguration;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration;
import io.xol.chunkstories.api.rendering.pipeline.ShaderInterface;
import io.xol.chunkstories.api.rendering.pipeline.TexturingConfiguration;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.BlendMode;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.CullingMode;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.DepthTestMode;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration.PolygonFillMode;
import io.xol.chunkstories.api.rendering.target.RenderTargetManager;
import io.xol.chunkstories.api.rendering.text.FontRenderer;
import io.xol.chunkstories.api.rendering.textures.Cubemap;
import io.xol.chunkstories.api.rendering.textures.Texture1D;
import io.xol.chunkstories.api.rendering.textures.Texture2D;
import io.xol.chunkstories.api.rendering.textures.TextureFormat;
import io.xol.chunkstories.api.rendering.vertex.AttributeSource;
import io.xol.chunkstories.api.rendering.vertex.VertexBuffer;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public interface RenderingInterface
{
	public CameraInterface getCamera();

	public ClientInterface getClient();
	
	public ClientRenderingConfig renderingConfig();
	
	public GameWindow getWindow();
	
	public RenderTargetManager getRenderTargetManager();
	
	/* Shaders */
	
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

	public TexturesLibrary textures();
	
	public Texture2D newTexture2D(TextureFormat type, int width, int height);
	
	/* Object location & Instance Data */
	
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
	
	/** If attributeSource != null, setups the currently bound vertex shader attribute input 'attributeName' with it
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
	
	public ClientMeshLibrary meshes();
	
	public VertexBuffer newVertexBuffer();
	
	/**
	 * Draws N primitives made of 'count' vertices, offset at vertice 'startAt', using data specified in the AttributesConfiguration
	 * @return Returns a RenderingCommand object, containing a snapshot of the current state of the RenderingInterface and adds it to the rendering queue
	 */
	public RenderingCommand draw(Primitive primitive, int startAt, int count);
	
	public RenderingCommand drawMany(Primitive primitive, int... startAndCountPairs);

	/** Renders a fullsize quad for whole-screen effects */
	public void drawFSQuad();
	
	/**
	 * Executes ALL commands in the queue up to this point before continuing
	 */
	public void flush();
	
	/* Statistics */
	
	public default long getTotalVramUsage()
	{
		return getVertexDataVramUsage() + getTextureDataVramUsage();
	}
	
	public long getVertexDataVramUsage();
	
	public long getTextureDataVramUsage();
	
	/* Specific renderers / helpers */

	public GuiRenderer getGuiRenderer();

	public FontRenderer getFontRenderer();
	
	public WorldRenderer getWorldRenderer();
	
	public LightsAccumulator getLightsRenderer();
	
	interface LightsAccumulator {
		public void queueLight(Light light);

		public void renderPendingLights(RenderingInterface renderingContext);
	}
}
