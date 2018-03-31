//
// This file is a part of the Chunk Stories Implementation codebase
// Check out README.md for more information
// Website: http://chunkstories.xyz
//

package io.xol.chunkstories.mesh;

import java.io.BufferedReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.xol.chunkstories.api.content.Asset;
import io.xol.chunkstories.api.exceptions.content.MeshLoadException;
import io.xol.chunkstories.api.mesh.Mesh;
import io.xol.chunkstories.api.mesh.MeshLoader;

public class WavefrontLoader implements MeshLoader
{
	private static final Logger logger = LoggerFactory.getLogger("content.meshes.obj");
	public Logger logger() {
		return logger;
	}
	
	public Mesh loadMeshFromAsset(Asset asset) throws MeshLoadException
	{
		List<float[]> vertices = new ArrayList<float[]>();
		List<float[]> texcoords = new ArrayList<float[]>();
		List<float[]> normals = new ArrayList<float[]>();
		
		/*
		// Reset values
		vertices.clear();
		texcoords.clear();
		normals.clear();*/

		Map<String, Integer> groupsSizesMap = new HashMap<String, Integer>();
		Map<String, List<float[]>> tempGroups = new HashMap<String, List<float[]>>();

		boolean hasGroups = false;
		
		int groupSize = 0;
		String group = "root";

		int totalVertices = 0;

		int line = 0;
		try
		{
			// Read the actual file
			BufferedReader reader = new BufferedReader(asset.reader());
			String[] splitted;
			String[] e;
			String l;
			float[] v, t, n;
			while ((l = reader.readLine()) != null)
			{
				line++;
				if (!l.startsWith("#"))
				{
					splitted = l.split(" ");
					//Parse the various vertices attributes
					if (l.startsWith("vt"))
					{
						//Note that we invert Y texture coordinates from blender to ogl
						texcoords.add(new float[] { Float.parseFloat(splitted[1]), (1 - Float.parseFloat(splitted[2])) });
					}
					else if (l.startsWith("vn"))
					{
						normals.add(new float[] { Float.parseFloat(splitted[1]), Float.parseFloat(splitted[2]), Float.parseFloat(splitted[3]) });
					}
					else if (l.startsWith("v"))
					{
						vertices.add(new float[] { Float.parseFloat(splitted[1]), Float.parseFloat(splitted[2]), Float.parseFloat(splitted[3]) });
					}
					//Vertices group manager
					else if (l.startsWith("g"))
					{
						hasGroups = true;
						
						//If the current group contains vertices, note the size and put it in the hashmap
						if (groupSize > 0)
						{
							//All is simple if this is the first time we are done with this group ...
							if (!groupsSizesMap.containsKey(group))
								groupsSizesMap.put(group, groupSize);
							//But if that group already exists it means that it's vertices are splitted arround the file and we need to add up
							//the sizes and then write them in the correct order in the final buffer !
							else
							{
								int i = groupsSizesMap.get(group) + groupSize;
								groupsSizesMap.remove(group);
								groupsSizesMap.put(group, i);
							}
						}
						//Resets group size and change the current group name
						groupSize = 0;
						group = splitted[1];
					}
					else if (l.startsWith("f"))
					{
						groupSize++;
						// No support for quads, only triangles.
						if (splitted.length == 4)
						{
							for (int i = 1; i <= 3; i++) // For each vertex of
															// the triangle.
							{
								e = splitted[i].split("/");
								//Gets the various properties of the Obj file
								v = vertices.get(Integer.parseInt(e[0]) - 1);
								t = texcoords.get(Integer.parseInt(e[1]) - 1);
								n = normals.get(Integer.parseInt(e[2]) - 1);

								//Add the face to the current vertex group
								if (!tempGroups.containsKey(group))
									tempGroups.put(group, new ArrayList<float[]>());
								tempGroups.get(group).add(new float[] { v[0], v[1], v[2], t[0], t[1], n[0], n[1], n[2] });
							}

							totalVertices += 3;
						}
					}
				}
			}
			//Same logic as above, terminates the last group
			if (groupSize > 0)
			{
				if (!groupsSizesMap.containsKey(group))
					groupsSizesMap.put(group, groupSize);
				else
				{
					int i = groupsSizesMap.get(group) + groupSize;
					groupsSizesMap.remove(group);
					groupsSizesMap.put(group, i);
				}
			}
			reader.close();

				
			FloatBuffer verticesBuffer = ByteBuffer.allocateDirect(3 * totalVertices * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();//BufferUtils.createFloatBuffer(3 * totalVertices);
			FloatBuffer textureCoordinatesBuffer =  ByteBuffer.allocateDirect(2 * totalVertices * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();//BufferUtils.createFloatBuffer(2 * totalVertices);
			FloatBuffer normalsBuffer =  ByteBuffer.allocateDirect(3 * totalVertices * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();//BufferUtils.createFloatBuffer(3 * totalVertices);

			// Iterates over each group name in order of apparition
			for (String gName : tempGroups.keySet())
			{
				for (float[] gData : tempGroups.get(gName))
				{
					verticesBuffer.put(gData[0]);
					verticesBuffer.put(gData[1]);
					verticesBuffer.put(gData[2]);
					textureCoordinatesBuffer.put(gData[3]);
					textureCoordinatesBuffer.put(gData[4]);
					normalsBuffer.put(gData[5]);
					normalsBuffer.put(gData[6]);
					normalsBuffer.put(gData[7]);
					
					//System.out.println(gData[0]);
				}
			}

			verticesBuffer.flip();
			textureCoordinatesBuffer.flip();
			normalsBuffer.flip();

			if(hasGroups)
				return null;//new MultiPartMesh(verticesBuffer, textureCoordinatesBuffer, normalsBuffer, groupsSizesMap);
			else
				return new Mesh(verticesBuffer, textureCoordinatesBuffer, normalsBuffer);
			//return new ObjMeshRenderable(totalVertices, verticesBuffer, textureCoordinatesBuffer, normalsBuffer, groupsSizesMap);
		
			/*}*/
		}
		catch (Exception e)
		{
			//God damnit
			logger().error("Error loading model at line " + line);
			e.printStackTrace();
		}

		return null;
	}

	@Override
	public String getExtension() {
		return "obj";
	}
}
