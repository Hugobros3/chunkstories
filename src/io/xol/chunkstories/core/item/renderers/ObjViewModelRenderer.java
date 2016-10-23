package io.xol.chunkstories.core.item.renderers;

import io.xol.engine.graphics.textures.TexturesHandler;
import io.xol.engine.math.lalgb.Matrix4f;
import io.xol.chunkstories.api.Location;
import io.xol.chunkstories.api.item.Item;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.item.ItemPile;
import io.xol.chunkstories.item.renderer.DefaultItemRenderer;
import io.xol.engine.model.ModelLibrary;

//(c) 2015-2016 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class ObjViewModelRenderer extends DefaultItemRenderer
{
	final String objName;
	final String albedoTextureName;
	final String normalTextureName;
	//TODO
	final String materialTextureName = null;

	public ObjViewModelRenderer(Item item, String objName, String albedoTextureName)
	{
		this(item, objName, albedoTextureName, "./textures/normalnormal.png");
	}
	
	public ObjViewModelRenderer(Item item, String objName, String albedoTextureName, String normalTextureName)
	{
		super(item);
		
		this.objName = objName;
		this.albedoTextureName = albedoTextureName;
		this.normalTextureName = normalTextureName;
	}

	@Override
	public void renderItemInInventory(RenderingInterface context, ItemPile pile, int screenPositionX, int screenPositionY, int scaling)
	{
		super.renderItemInInventory(context, pile, screenPositionX, screenPositionY, scaling);
	}

	@Override
	public void renderItemInWorld(RenderingInterface renderingContext, ItemPile pile, World world, Location location, Matrix4f handTransformation)
	{
		renderingContext.setObjectMatrix(handTransformation);
		
		renderingContext.bindAlbedoTexture(TexturesHandler.getTexture(albedoTextureName));
		renderingContext.bindNormalTexture(TexturesHandler.getTexture(normalTextureName));
		ModelLibrary.getRenderableMesh(objName).render(renderingContext);
	}

}
