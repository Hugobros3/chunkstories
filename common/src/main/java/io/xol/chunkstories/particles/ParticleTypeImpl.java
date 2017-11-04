package io.xol.chunkstories.particles;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import io.xol.chunkstories.api.content.Content.ParticlesTypes;
import io.xol.chunkstories.api.exceptions.content.IllegalParticleDeclarationException;
import io.xol.chunkstories.api.particles.ParticleType;
import io.xol.chunkstories.api.particles.ParticleTypeHandler;
import io.xol.chunkstories.materials.GenericNamedConfigurable;

public class ParticleTypeImpl extends GenericNamedConfigurable implements ParticleType {

	private final int id;
	private final ParticlesTypesStore store;

	private final float billBoardSize;
	private final RenderTime renderTime;
	
	private final ParticleTypeHandler handler;

	@Override
	public String getShaderName() {
		return this.resolveProperty("shaderName", "particles");
	}

	public ParticleTypeImpl(ParticlesTypesStore store, String particleName, int id, BufferedReader reader)
			throws IllegalParticleDeclarationException, IOException {
		super(particleName, reader);
		this.id = id;
		this.store = store;
		
		String rt = this.resolveProperty("renderTime", "forward");
		if(rt.equals("forward"))
			renderTime = RenderTime.FORWARD;
		else if(rt.equals("gbuffer"))
			renderTime = RenderTime.GBUFFER;
		else if(rt.endsWith("never"))
			renderTime = RenderTime.NEVER;
		else
			throw new IllegalParticleDeclarationException("renderTime has to be any of {forward, gbuffer, never}");
		
		try {
			this.billBoardSize = Float.parseFloat(this.resolveProperty("billboardSize", "1.0"));
			
			String handlerClassName = this.resolveProperty("handlerClass");
			if(handlerClassName == null)
				throw new IllegalParticleDeclarationException("handlerClass isn't set !");

			Class<?> rawClass = store.parent().modsManager().getClassByName(handlerClassName);
			if (rawClass == null)
			{
				throw new IllegalParticleDeclarationException("ParticleTypeHandler " + this.getName() + " does not exist in codebase.");
			}
			else if (!(ParticleTypeHandler.class.isAssignableFrom(rawClass)))
			{
				throw new IllegalParticleDeclarationException("Class " + this.getName() + " is not extending the ParticleTypeHandler class.");
			}
			else
			{
				@SuppressWarnings("unchecked")
				Class<? extends ParticleTypeHandler> handlerClass = (Class<? extends ParticleTypeHandler>) rawClass;
				Class<?>[] types = { ParticleType.class };
				Constructor<? extends ParticleTypeHandler> constructor = handlerClass.getConstructor(types);

				if (constructor == null)
				{
					throw new IllegalParticleDeclarationException("ParticleTypeHandler " + this.getName() + " does not provide a valid constructor.");
				}
				
				handler = constructor.newInstance(this);
			}
			
		} catch (NumberFormatException e) {
			throw new IllegalParticleDeclarationException("Billboard size must be a number.");
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
			throw new IllegalParticleDeclarationException("Error isntanciating the ParticleTypeHandler : "+e.getMessage());
		}
	}

	ParticleTypeHandler handler() {
		return handler;
	}
	
	@Override
	public int getID() {
		return id;
	}

	@Override
	public ParticlesTypes store() {
		return store;
	}

	@Override
	public RenderTime getRenderTime() {
		return renderTime;
	}

	@Override
	public String getAlbedoTexture() {
		return this.resolveProperty("albedoTexture");
	}

	@Override
	public String getNormalTexture() {
		return this.resolveProperty("normalTexture");
	}

	@Override
	public String getMaterialTexture() {
		return this.resolveProperty("materialTexture");
	}

	@Override
	public float getBillboardSize() {
		return billBoardSize;
	}

}
