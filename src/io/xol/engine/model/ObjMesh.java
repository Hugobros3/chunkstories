package io.xol.engine.model;

import io.xol.chunkstories.GameData;
import io.xol.engine.model.animation.BVHAnimation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.BufferUtils;
import org.lwjgl.util.vector.Matrix4f;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

/**
 * An appoling mess of an Obj loader
 * @author Hugo
 *
 */
public class ObjMesh
{
	int vboId = -1;
	public Map<String, Integer> groups = new HashMap<String, Integer>();

	public ObjMesh(String name) throws FileNotFoundException
	{
		loadMesh(GameData.getFileLocation(name));
	}

	public void loadMesh(File file) throws FileNotFoundException
	{
		// Check file
		if (!file.exists() || file.isDirectory())
		{
			System.out.println(".obj file " + file.getAbsolutePath() + " coudln't be loaded.");
			throw new FileNotFoundException(file.getAbsolutePath());
		}
		// Reset values
		vertices.clear();
		texcoords.clear();
		normals.clear();

		Map<String, List<float[]>> tempGroups = new HashMap<String, List<float[]>>();

		vboData.clear();

		// groups.clear();
		int groupSize = 0;
		String group = "root";

		int line = 0;
		//int faces = 0;
		try
		{
			// Read the actual file
			BufferedReader reader = new BufferedReader(new FileReader(file));
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
					if (l.startsWith("vt"))
					{
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
					else if (l.startsWith("g"))
					{
						// This loader defines sub-parts of the vbo
						//System.out.println("loaded " + faces + " faces." + groupSize + " new list" + splitted[1] + "old:" + group + " s:" + groups.size());
						if (groupSize > 0)
						{
							if (!groups.containsKey(group))
								groups.put(group, groupSize);
							else
							{
								int i = groups.get(group) + groupSize;
								groups.remove(group);
								groups.put(group, i);
							}
						}
						groupSize = 0;
						group = splitted[1];
					}
					else if (l.startsWith("f"))
					{
						groupSize++;
						//faces++;
						// No support for quads, only triangles.
						if (splitted.length == 4)
						{
							for (int i = 1; i < 4; i++) // For each vertex of
														// the triangle.
							{
								e = splitted[i].split("/");
								v = vertices.get(Integer.parseInt(e[0]) - 1);
								t = texcoords.get(Integer.parseInt(e[1]) - 1);
								n = normals.get(Integer.parseInt(e[2]) - 1);
								// XYZTSNLO - 8 components of float.
								// this is fucking disgusting.
								if (!tempGroups.containsKey(group))
									tempGroups.put(group, new ArrayList<float[]>());
								tempGroups.get(group).add(new float[] { v[0], v[1], v[2], t[0], t[1], n[0], n[1], n[2] });
							}
						}
						/*
						 * if(splitted.length == 5) { groupSize++; for(int i :
						 * new int[]{1,2,3, 1,3,4}) { e =
						 * splitted[i].split("/"); v =
						 * vertices.get(Integer.parseInt(e[0])-1); t =
						 * texcoords.get(Integer.parseInt(e[1])-1); n =
						 * normals.get(Integer.parseInt(e[2])-1); // XYZTSNLO -
						 * 8 components of float. vboData.add( new float[]{v[0],
						 * v[1], v[2], t[0], t[1], n[0], n[1], n[2]} ); } }
						 */
					}
				}
			}
			if (groupSize > 0)
			{
				if (!groups.containsKey(group))
					groups.put(group, groupSize);
				else
				{
					int i = groups.get(group) + groupSize;
					groups.remove(group);
					groups.put(group, i);
				}
				// I want to vomit
			}
			// It'll be over soon
			for (String gName : tempGroups.keySet())
			{
				for (float[] gData : tempGroups.get(gName))
				{
					vboData.add(gData);
				}
			}
			reader.close();
			// Send OpenGL the data.
			FloatBuffer fb = BufferUtils.createFloatBuffer(8 * vboData.size());
			for (float[] d : vboData)
				fb.put(d);
			fb.flip();
			// Make a buffer for vertex data
			if (vboId == -1)
				vboId = glGenBuffers();
			// Upload it
			glBindBuffer(GL_ARRAY_BUFFER, vboId);
			glBufferData(GL_ARRAY_BUFFER, fb, GL_STATIC_DRAW);

			//System.out.println("loaded " + faces + " faces. in vbo" + vboId);
		}
		catch (Exception e)
		{
			System.out.println("Error loading model at line "+line);
			e.printStackTrace();
		}
	}

	public void destroy()
	{
		glDeleteBuffers(vboId);
	}

	public void render(RenderingContext renderingContext)
	{
		glBindBuffer(GL_ARRAY_BUFFER, vboId);
		if (renderingContext.verticesAttribMode)
		{
			// glDisableVertexAttribArray(colorIn);
			// vboData.add( new float[]{v[0], v[1], v[2], t[0], t[1], n[0],
			// n[1], n[2]} );
			glVertexAttribPointer(renderingContext.vertexIn, 3, GL_FLOAT, false, 8 * 4, 0);
			glVertexAttribPointer(renderingContext.texCoordIn, 2, GL_FLOAT, false, 8 * 4, 3 * 4);
			glVertexAttribPointer(renderingContext.normalIn, 3, GL_FLOAT, true, 8 * 4, 5 * 4);
			int totalSize = 0;
			for (int i : groups.values())
			{
				//System.out.println("nsm : "+i+" "+groups.containsValue(i)+" s");
				glDrawArrays(GL_TRIANGLES, totalSize * 3, i * 3);
				totalSize += i;
			}
		}
	}

	public void renderUsingBVHTree(RenderingContext renderingContext, BVHAnimation animationData, int frame)
	{
		glBindBuffer(GL_ARRAY_BUFFER, vboId);
		
		//EntityTest debug = new EntityTest(null, 0, 0, 0);
		if (renderingContext.verticesAttribMode)
		{
			glVertexAttribPointer(renderingContext.vertexIn, 3, GL_FLOAT, false, 8 * 4, 0);
			glVertexAttribPointer(renderingContext.texCoordIn, 2, GL_FLOAT, false, 8 * 4, 3 * 4);
			glVertexAttribPointer(renderingContext.normalIn, 3, GL_FLOAT, true, 8 * 4, 5 * 4);
			int totalSize = 0;
			Matrix4f matrix;
			for (String currentVertexGroup : groups.keySet())
			{
				int i = groups.get(currentVertexGroup);
				matrix = animationData.getTransformationForBonePlusOffset(currentVertexGroup, frame);
				renderingContext.renderingShader.setUniformMatrix4f("localTransform", matrix);
				matrix.m30 = 0;
				matrix.m31 = 0;
				matrix.m32 = 0;
				renderingContext.renderingShader.setUniformMatrix4f("localTransformNormal", matrix);
				glDrawArrays(GL_TRIANGLES, totalSize * 3, i * 3);
				totalSize += i;
			}
		}
	}

	public static List<float[]> vertices = new ArrayList<float[]>();
	public static List<float[]> texcoords = new ArrayList<float[]>();
	public static List<float[]> normals = new ArrayList<float[]>();

	public static List<float[]> vboData = new ArrayList<float[]>();
}
