package io.xol.engine.graphics;

import io.xol.chunkstories.api.client.ClientContent.TexturesLibrary;
import io.xol.chunkstories.api.client.ClientInterface;
import io.xol.chunkstories.api.client.ClientRenderingConfig;
import io.xol.chunkstories.api.exceptions.rendering.AttributeNotPresentException;
import io.xol.chunkstories.api.exceptions.rendering.RenderingException;
import io.xol.chunkstories.api.math.Matrix4f;
import io.xol.chunkstories.api.rendering.pipeline.AttributesConfiguration;
import io.xol.chunkstories.api.rendering.pipeline.PipelineConfiguration;
import io.xol.chunkstories.api.rendering.pipeline.ShaderInterface;
import io.xol.chunkstories.api.rendering.pipeline.TexturingConfiguration;
import io.xol.chunkstories.api.world.WorldClient;
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
import io.xol.chunkstories.api.rendering.vertex.VertexFormat;
import io.xol.chunkstories.client.Client;
import io.xol.chunkstories.renderer.Camera;
import io.xol.chunkstories.renderer.lights.LightsRenderer;
import io.xol.chunkstories.api.rendering.Primitive;
import io.xol.chunkstories.api.rendering.RenderingCommand;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.WorldRenderer;
import io.xol.chunkstories.api.rendering.mesh.ClientMeshLibrary;
import io.xol.engine.base.GameWindowOpenGL_LWJGL3;
import io.xol.engine.graphics.fbo.OpenGLRenderTargetManager;
import io.xol.engine.graphics.fonts.TrueTypeFontRenderer;
import io.xol.engine.graphics.geometry.VertexBufferGL;
import io.xol.engine.graphics.shaders.ShaderProgram;
import io.xol.engine.graphics.shaders.ShadersLibrary;
import io.xol.engine.graphics.textures.TextureGL;
import io.xol.engine.graphics.textures.Texture2DRenderTargetGL;
import io.xol.engine.graphics.textures.TexturingConfigurationImplementation;
import io.xol.engine.graphics.util.GuiRendererImplementation;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import org.lwjgl.BufferUtils;

//(c) 2015-2017 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

public class RenderingContext implements RenderingInterface
{
	private GameWindowOpenGL_LWJGL3 gameWindow;
	private ShaderProgram currentlyBoundShader = null;

	private final Camera mainCamera = new Camera();
	//private boolean isThisAShadowPass;
	
	private LightsRenderer lightsRenderers = new LightsRenderer(this);

	private GuiRendererImplementation guiRenderer;
	private TrueTypeFontRenderer trueTypeFontRenderer;

	//Texturing
	private TexturingConfigurationImplementation texturingConfiguration = new TexturingConfigurationImplementation();
	//Object matrix
	private Matrix4f currentObjectMatrix = null;
	//Pipeline config
	private PipelineConfigurationImplementation pipelineConfiguration = PipelineConfigurationImplementation.DEFAULT;
	private AttributesConfigurationImplementation attributesConfiguration = new AttributesConfigurationImplementation();

	private int queuedCommandsIndex;
	private RenderingCommand[] queuedCommands = new RenderingCommand[1024];
	
	private final RenderTargetManager renderTargetManager;
	//private Deque<RenderingCommandImplementation> commands = new ArrayDeque<RenderingCommandImplementation>();

	public RenderingContext(GameWindowOpenGL_LWJGL3 windows)
	{
		gameWindow = windows;
		
		renderTargetManager = new OpenGLRenderTargetManager(this);
		
		guiRenderer = new GuiRendererImplementation(this);
		trueTypeFontRenderer = new TrueTypeFontRenderer(this);
	}

	public String toString()
	{
		/*String attributes = "";
		for (int i : enabledAttributes)
		{
			attributes += i;
		}
		attributes += " (" + enabledAttributes.size() + ")";
		return "[RenderingContext shadow:" + isThisAShadowPass + " enabledAttributes: " + attributes + " lights: " + lights.size() + " shader:" + currentShader() + " ]";
		 */
		return "wip";
	}

	public Camera getCamera()
	{
		return mainCamera;
	}

	public GameWindowOpenGL_LWJGL3 getWindow()
	{
		return gameWindow;
	}
	
	public ShaderInterface useShader(String shaderName)
	{
		return setCurrentShader(ShadersLibrary.getShaderProgram(shaderName));
	}

	private ShaderInterface setCurrentShader(ShaderProgram shaderProgram)
	{
		//Save calls
		if (shaderProgram != currentlyBoundShader)
		{
			//When changing shaders, we make sure we disable whatever was enabled
			flush();

			texturingConfiguration = new TexturingConfigurationImplementation();
			TexturingConfigurationImplementation.resetBoundTextures();
			attributesConfiguration = new AttributesConfigurationImplementation();
			//resetAllVertexAttributesLocations();
			//disableUnusedVertexAttributes();
		}
		currentlyBoundShader = shaderProgram;
		return currentlyBoundShader;
	}

	public ShaderInterface currentShader()
	{
		return currentlyBoundShader;
	}

	/* TEXTURING */

	public TexturingConfiguration getTexturingConfiguration()
	{
		return texturingConfiguration;
	}

	public TexturingConfiguration bindTexture1D(String textureSamplerName, Texture1D texture)
	{
		texturingConfiguration = texturingConfiguration.bindTexture1D(textureSamplerName, texture);
		return texturingConfiguration;
	}

	public TexturingConfiguration bindTexture2D(String textureSamplerName, Texture2D texture)
	{
		texturingConfiguration = texturingConfiguration.bindTexture2D(textureSamplerName, texture);
		return texturingConfiguration;
	}

	public TexturingConfiguration bindCubemap(String cubemapSamplerName, Cubemap cubemapTexture)
	{
		texturingConfiguration = texturingConfiguration.bindCubemap(cubemapSamplerName, cubemapTexture);
		return texturingConfiguration;
	}

	public TexturingConfiguration bindAlbedoTexture(Texture2D texture)
	{
		return bindTexture2D("diffuseTexture", texture);
	}

	public TexturingConfiguration bindNormalTexture(Texture2D texture)
	{
		return bindTexture2D("normalTexture", texture);
	}

	public TexturingConfiguration bindMaterialTexture(Texture2D texture)
	{
		return bindTexture2D("materialTexture", texture);
	}

	/*@Deprecated
	public boolean isThisAShadowPass()
	{
		return isThisAShadowPass;
	}

	@Deprecated
	public void setIsShadowPass(boolean isShadowPass)
	{
		isThisAShadowPass = isShadowPass;
	}*/

	public GuiRendererImplementation getGuiRenderer()
	{
		return guiRenderer;
	}

	@Override
	public Matrix4f setObjectMatrix(Matrix4f objectMatrix)
	{
		if (objectMatrix == null)
			objectMatrix = new Matrix4f();
		currentObjectMatrix = objectMatrix.clone();
		return this.currentObjectMatrix;
	}

	public Matrix4f getObjectMatrix()
	{
		return this.currentObjectMatrix;
	}

	@Override
	public void setWorldLight(int sunLight, int blockLight)
	{
		// TODO Auto-generated method stub
		
	}

	static VertexBuffer fsQuadVertices = null;
	static AttributeSource fsQuadAttrib;

	public void drawFSQuad()
	{
		if (fsQuadVertices == null)
		{
			fsQuadVertices = new VertexBufferGL();
			FloatBuffer fsQuadBuffer = BufferUtils.createFloatBuffer(6 * 2);
			fsQuadBuffer.put(new float[] { 1f, 1f, -1f, -1f, 1f, -1f, 1f, 1f, -1f, 1f, -1f, -1f });
			fsQuadBuffer.flip();

			fsQuadVertices.uploadData(fsQuadBuffer);

			fsQuadAttrib = fsQuadVertices.asAttributeSource(VertexFormat.FLOAT, 2);
		}

		this.bindAttribute("vertexIn", fsQuadAttrib);

		this.draw(Primitive.TRIANGLE, 0, 6);
	}

	/* Pipeline config */

	@Override
	public PipelineConfiguration getPipelineConfiguration()
	{
		return pipelineConfiguration;
	}

	@Override
	public PipelineConfiguration setDepthTestMode(DepthTestMode depthTestMode)
	{
		pipelineConfiguration = pipelineConfiguration.setDepthTestMode(depthTestMode);
		return pipelineConfiguration;
	}

	@Override
	public PipelineConfiguration setBlendMode(BlendMode blendMode)
	{
		pipelineConfiguration = pipelineConfiguration.setBlendMode(blendMode);
		return pipelineConfiguration;
	}

	@Override
	public PipelineConfiguration setCullingMode(CullingMode cullingMode)
	{
		pipelineConfiguration = pipelineConfiguration.setCullingMode(cullingMode);
		return pipelineConfiguration;
	}

	@Override
	public PipelineConfiguration setPolygonFillMode(PolygonFillMode polygonFillMode)
	{
		pipelineConfiguration = pipelineConfiguration.setPolygonFillMode(polygonFillMode);
		return pipelineConfiguration;
	}

	@Override
	public AttributesConfiguration getAttributesConfiguration()
	{
		return attributesConfiguration;
	}

	@Override
	public AttributesConfiguration bindAttribute(String attributeName, AttributeSource attributeSource) throws AttributeNotPresentException
	{
		//TODO check in shader if attribute exists
		attributesConfiguration = attributesConfiguration.bindAttribute(attributeName, attributeSource);

		return this.attributesConfiguration;
	}

	@Override
	public AttributesConfiguration unbindAttributes()
	{
		attributesConfiguration = new AttributesConfigurationImplementation();
		return this.attributesConfiguration;
	}

	@Override
	public RenderingCommand draw(Primitive p, int startAt, int count)
	{
		RenderingCommandImplementation command = new RenderingCommandSingleInstance(p, currentlyBoundShader, texturingConfiguration, attributesConfiguration, currentlyBoundShader.getUniformsConfiguration(), pipelineConfiguration, currentObjectMatrix,
				startAt, count);

		queue(command);

		return command;
	}
	
	@Override
	public RenderingCommand drawMany(Primitive p, int... startAndCountPairs)
	{
		if(startAndCountPairs.length == 0)
			return null;
		if(startAndCountPairs.length % 2 == 1)
			throw new IllegalArgumentException("Non-pair amount of integers provided");
		
		int nb_arguments = startAndCountPairs.length / 2;
		IntBuffer starts = BufferUtils.createIntBuffer(nb_arguments);
		IntBuffer counts = BufferUtils.createIntBuffer(nb_arguments);
		
		for(int i = 0; i < nb_arguments; i++)
		{
			starts.put(startAndCountPairs[i*2]);
			counts.put(startAndCountPairs[i*2 + 1]);
		}
		
		starts.flip();
		counts.flip();
		
		RenderingCommandMultiDraw command = new RenderingCommandMultiDraw(p, currentlyBoundShader, texturingConfiguration, attributesConfiguration, currentlyBoundShader.getUniformsConfiguration(), pipelineConfiguration, currentObjectMatrix, starts, counts);

		queue(command);

		return command;
	}

	private void queue(RenderingCommandImplementation command)
	{
		//Limit to how many commands it may stack
		if (queuedCommandsIndex >= 1024)
			flush();

		queuedCommands[queuedCommandsIndex] = command;
		queuedCommandsIndex++;
	}

	@Override
	public void flush()
	{
		try
		{
			int kek = 0;
			while (kek < queuedCommandsIndex)
			{
				queuedCommands[kek].render(this);
				queuedCommands[kek] = null;
				kek++;
			}
		}
		catch (RenderingException e)
		{
			e.printStackTrace();
		}

		queuedCommandsIndex = 0;
	}

	@Override
	public long getVertexDataVramUsage()
	{
		return this.gameWindow.vramUsageVerticesObjects;
	}

	@Override
	public long getTextureDataVramUsage()
	{
		return TextureGL.getTotalVramUsage();
	}

	@Override
	public RenderTargetManager getRenderTargetManager()
	{
		return renderTargetManager;
	}

	@Override
	public LightsRenderer getLightsRenderer()
	{
		return lightsRenderers;
	}

	@Override
	public WorldRenderer getWorldRenderer()
	{
		WorldClient world = Client.getInstance().getWorld();
		if(world != null)
			return world.getWorldRenderer();
		return null;
	}

	@Override
	public Texture2D newTexture2D(TextureFormat type, int width, int height) {
		return new Texture2DRenderTargetGL(type, width, height);
	}
	

	@Override
	public VertexBuffer newVertexBuffer() {
		return new VertexBufferGL();
	}
	

	@Override
	public TrueTypeFontRenderer getFontRenderer() {
		return trueTypeFontRenderer;
	}

	@Override
	public final ClientInterface getClient() {
		return Client.getInstance();
	}

	@Override
	public final ClientRenderingConfig renderingConfig() {
		return Client.getInstance().renderingConfig();
	}

	@Override
	public final TexturesLibrary textures() {
		return getClient().getContent().textures();
	}

	@Override
	public final ClientMeshLibrary meshes() {
		return getClient().getContent().meshes();
	}
}