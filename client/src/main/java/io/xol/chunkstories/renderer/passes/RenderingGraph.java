//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.renderer.passes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.xol.chunkstories.api.events.rendering.RenderPassesInitEvent;
import io.xol.chunkstories.api.rendering.RenderingInterface;
import io.xol.chunkstories.api.rendering.pass.RenderPass;
import io.xol.chunkstories.api.rendering.pass.RenderPasses;
import io.xol.chunkstories.api.rendering.textures.Texture;
import io.xol.chunkstories.api.rendering.world.WorldRenderer;

public class RenderingGraph implements RenderPasses {

	private static final Logger logger = LoggerFactory.getLogger("rendering.graph");
	static Logger logger() {
		return logger;
	}
	
	Map<String, RenderPass> registeredRenderPasses = new HashMap<>();
	RenderingInterface renderer;

	private RenderPass currentPassBeingRendered = null;
	private RenderPass[] executionOrder = null;
	
	Map<String, Node> nodes = new HashMap<>();
	private boolean shouldRebuildGraph = true;
	
	public RenderingGraph(RenderingInterface renderer) {
		this.renderer = renderer;
	}
	
	@Override
	public RenderingInterface getRenderingInterface() {
		return renderer;
	}

	@Override
	public void registerRenderPass(RenderPass pass) {
		registeredRenderPasses.put(pass.name, pass);
	}

	@Override
	public RenderPass getRenderPass(String name) {
		return registeredRenderPasses.get(name);
	}
	
	@Override
	public RenderPass getCurrentPass() {
		return currentPassBeingRendered;
	}
	
	private void resolveGraphOrder() {
		RenderPass finalPass = getRenderPass("final");
		
		List<RenderPass> dependencies = new ArrayList<>();
		
		nodes.clear();
		try {
			recursivelyAddNodes(finalPass, null);
		
			while(nodes.size() > 0) {
				//Find a node with no children
				Node noChildrenNode = null;
				for(Node node : nodes.values()) {
					if(node.children.size() == 0) {
						noChildrenNode = node;
						break;
					}
				}
				
				if(noChildrenNode == null)
					throw new CycleException("could not find a node without children, we have a loop!");
				
				//Add it to the sequence of operations
				dependencies.add(noChildrenNode.pass);
				
				//remove that node from the list
				nodes.remove(noChildrenNode.pass.name);
				
				//remove any mention of it in the other nodes
				final Node n2 = noChildrenNode;
				nodes.forEach((s,n) -> n.children.remove(n2));
			}
			
			executionOrder = new RenderPass[dependencies.size()];
			for(int i = 0; i < executionOrder.length; i++) {
				executionOrder[i] = dependencies.get(i);
			}
		
		} catch (CycleException e) {
			e.printStackTrace();
		}
	}

	private Node recursivelyAddNodes(RenderPass pass, Node parent) throws CycleException {
		Node node = nodes.get(pass.name);
		
		if(node == null) {
			node = new Node();
			node.pass = pass;
			nodes.put(pass.name, node);
		}
		
		//Ensure we can't find this node by going up
		cycleCheck(node, parent);
		
		node.parents.add(parent);
		for(String requirement : pass.requires) {
			
			String passName = requirement.indexOf(".") == -1 ? "invalid" : requirement.split("\\.")[0];
			
			//System.out.println("this requires pass: "+passName + " (from "+requirement+")");
			RenderPass requiredPass = getRenderPass(passName);
			if(requiredPass != null) {
				Node childNode = recursivelyAddNodes(requiredPass, node);
				node.children.add(childNode);
			} else
				logger.error("pass" +passName+" missing");
		}
		
		return node;
	}
	
	void cycleCheck(Node lookFor, Node node) throws CycleException {
		if(node == null)
			return;
		
		if(node.equals(lookFor))
			throw new CycleException(lookFor.pass.name + " requires itself");
		
		for(Node parent : node.parents) {
			cycleCheck(lookFor, parent);
		}
	}
	
	class Node {
		RenderPass pass;
		
		Set<Node> parents = new HashSet<>();
		Set<Node> children = new HashSet<>();
	}
	
	/** Passes the output buffers of the early passes to the inputs of the later ones */
	private void resolveInputs() {
		if(executionOrder != null) {
			for(int i = 0; i < executionOrder.length; i++) {
				RenderPass pass = executionOrder[i];
				
				Map<String, Texture> inputs = new HashMap<>();
				
				//For each buffer we depend on
				for(String requirement : pass.requires) {
					//System.out.println("resolving requirement "+requirement+" for pass "+pass.name);
					
					//parse pass and buffer name
					String s[] = requirement.split("\\.");
					String requiredPassName = s[0];
					String requiredBufferName = s.length > 0 ? s[1] : "invalid";
					
					boolean forward = requiredBufferName.endsWith("!");
					requiredBufferName = requiredBufferName.replace("!", "");
					
					//System.out.println(requiredPassName+"."+requiredBufferName+" forward:"+forward);
					RenderPass requiredPass = this.getRenderPass(requiredPassName);
					//assumes all previous passes were resolved ok
					Texture requiredBuffer = requiredPass == null ? null : requiredPass.resolvedOutputs.get(requiredBufferName);
					//System.out.println("found buffer "+requiredBuffer);
					
					if(forward) {
						//System.out.println("auto-forwarding buffer:"+requiredBufferName);
						pass.resolvedOutputs.put(requiredBufferName, requiredBuffer);
					}
					
					//return it the buffer he asked under multiple names
					inputs.put(requiredPassName + "." + requiredBufferName, requiredBuffer);
					inputs.put(requiredBufferName, requiredBuffer);
					inputs.put(requirement, requiredBuffer); // may alias !
				}
				
				//notify the pass we found it's buffers
				pass.resolvedInputs.clear();
				pass.resolvedInputs.putAll(inputs);
				pass.onResolvedInputs();
			}
		}
	}
	
	public void render(RenderingInterface renderer) {
		if(shouldRebuildGraph) {
			RenderPassesInitEvent event = new RenderPassesInitEvent(this);
			renderer.getClient().getPluginManager().fireEvent(event);
			
			resolveGraphOrder();
			resolveInputs();
			
			shouldRebuildGraph = false;
			
			System.out.println("----------");
			if(executionOrder != null) {
				for(RenderPass pass : executionOrder) {
					System.out.println("pass:" +pass.name + pass.getClass().getName());
				}
			}
		}

		if(executionOrder != null) {
			for(RenderPass pass : executionOrder) {
				this.currentPassBeingRendered = pass;
				pass.render(renderer);
			}
		}
	}

	public void resize(int width, int height) {
		if(executionOrder != null) {
			for(RenderPass pass : executionOrder) {
				pass.onScreenResize(width, height);
			}
		}
	}

	@Override
	public WorldRenderer getWorldRenderer() {
		return this.renderer.getWorldRenderer();
	}

	@Override
	public void reloadPasses() {
		this.shouldRebuildGraph = true;
	}
}
