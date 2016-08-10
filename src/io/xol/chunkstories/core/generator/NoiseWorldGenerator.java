package io.xol.chunkstories.core.generator;

import java.util.Random;

import io.xol.chunkstories.api.voxel.VoxelFormat;
import io.xol.chunkstories.api.world.World;
import io.xol.chunkstories.api.world.WorldGenerator;
import io.xol.chunkstories.api.world.chunk.Chunk;
import io.xol.chunkstories.api.world.chunk.Region;
import io.xol.chunkstories.world.WorldImplementation;
import io.xol.chunkstories.world.chunk.CubicChunk;
import io.xol.engine.math.lalgb.Vector3f;
import io.xol.engine.math.random.SeededSimplexNoiseGenerator;

public class NoiseWorldGenerator extends WorldGenerator
{
	Random rnd = new Random();
	SeededSimplexNoiseGenerator ssng;

	int ws;

	@Override
	public void initialize(World w)
	{
		super.initialize(w);
		ssng = new SeededSimplexNoiseGenerator(((WorldImplementation) w).getWorldInfo().getSeed());
		ws = world.getSizeInChunks() * 32;
	}

	@Override
	public Chunk generateChunk(Region region, int cx, int cy, int cz)
	{
		//rnd.setSeed(cx * 32 + cz + 48716148);
		
		CubicChunk chunk = new CubicChunk(region, cx, cy, cz);
		Vector3f position = new Vector3f();

		int wx = 8, wy = 4, wz = 8;
		
		//Real width of array, we need a x+1 array so we can properly interpolate at the far end
		int wwx = wx+1;
		int wwy = wy+1;
		int wwz = wz+1;
		float[] generated = new float[(wwx) * (wwy) * (wwz)];
		for (int a = 0; a < wwx; a++)
			for (int b = 0; b < wwy; b++)
				for (int c = 0; c < wwz; c++)
				{
					int x = a * (32 / wx);
					int y = b * (32 / wy);
					int z = c * (32 / wz);

					position.x = cx * 32 + x;
					position.y = cy * 32 + y;
					position.z = cz * 32 + z;

					position.scale(0.05f);
					generated[wwx * (wwy * c + b) + a] = ssng.noise(position.x, position.y, position.z);
				}

		for (int x = 0; x < 32; x++)
			for (int z = 0; z < 32; z++)
			{
				float finalHeight = 128.0f;
				finalHeight += ridgedNoise((cx * 32 + x), (cz * 32 + z), 5, 1.0f, 0.5f) * 128; // * (64 + 128 * mountainFactor));
				for (int y = 0; y < 32; y++)
				{
					//Assertion : a, b and c are not the last element in their dimension in the array
					//Put simply : you can access a+1, b+1 and c+1 even all of them at once, without issues.
					int a = (int) Math.floor(x / (32 / wx));
					int b = (int) Math.floor(y / (32 / wy));
					int c = (int) Math.floor(z / (32 / wz));
					
					//Unlerped value, for debug purposes
					float value = generated[wwx * (wwy * c + b) + a];

					//Lerped on X axis, 4 values
					float lerpedX_y0z0 = lerp(x, generated[wwx * (wwy * c       + b    ) + a], generated[wwx * (wwy * c       + b    ) + a + 1], a, a + 1, 32 / wx);
					float lerpedX_y1z0 = lerp(x, generated[wwx * (wwy * c       + b + 1) + a], generated[wwx * (wwy * c       + b + 1) + a + 1], a, a + 1, 32 / wx);

					float lerpedX_y0z1 = lerp(x, generated[wwx * (wwy * (c + 1) + b    ) + a], generated[wwx * (wwy * (c + 1) + b) +     a + 1], a, a + 1, 32 / wx);
					float lerpedX_y1z1 = lerp(x, generated[wwx * (wwy * (c + 1) + b + 1) + a], generated[wwx * (wwy * (c + 1) + b + 1) + a + 1], a, a + 1, 32 / wx);
					//Lerp that about the Y axis
					float lerpedXY_z0 = lerp(y, lerpedX_y0z0, lerpedX_y1z0, b, b + 1, 32 / wy);
					float lerpedXY_z1 = lerp(y, lerpedX_y0z1, lerpedX_y1z1, b, b + 1, 32 / wy);
					//Lerp moar
					float lerpedXYZ = lerp(z, lerpedXY_z0, lerpedXY_z1, c, c + 1, 32 / wz);
					
					value = lerpedXYZ;

					//Apply gradient so values decrease near ground and thus create air
					float gradient = clamp((finalHeight - 32 - (cy * 32 + y )) / 64f, 0.0f, 1.0f);
					gradient += clamp((finalHeight - (cy * 32 + y )) / 128f, 0.0f, 0.35f) + 0.00f;
					
					float noiseMult = clamp(((finalHeight + 64) - (cy * 32 + y )) / 32f, 0.0f, 1.0f);
					
					//gradient = gradient;
					value = gradient - clamp(value * noiseMult, -0.15f, 0.5f) * 0.15f;

					//Blocks writing
					if (value > 0.0f)
						chunk.setVoxelDataWithoutUpdates(x, y, z, VoxelFormat.format(99, 15-(int)(value * 15f), 0, 0));
					//Water
					else if (cy * 32 + y < 256)
						chunk.setVoxelDataWithoutUpdates(x, y, z, 128);
				}
			}
		return chunk;
	}

	float lerp(int x, float val0, float val1, int i0, int i1, int granularity)
	{
		if (x % granularity == 0)
			return val0;
		
		return (val1 * (x - i0 * granularity) + val0 * (i1 * granularity - x)) / granularity;
	}

	float clamp(float value, float lower, float upper)
	{
		if (value < lower)
			return lower;
		if (value > upper)
			return upper;
		return value;
	}

	float fractalNoise(int x, int z, int octaves, float freq, float persistence)
	{
		float total = 0.0f;
		float maxAmplitude = 0.0f;
		float amplitude = 1.0f;
		freq *= ws / (64 * 32);
		for(int i = 0; i < octaves; i++)
		{
			total += ssng.looped_noise(x * freq, z * freq, ws) * amplitude;
			freq*=2.0f;
			maxAmplitude += amplitude;
			amplitude *= persistence;
		}
		return total / maxAmplitude;
	}
	
	float ridgedNoise(int x, int z, int octaves, float freq, float persistence)
	{
		float total = 0.0f;
		float maxAmplitude = 0.0f;
		float amplitude = 1.0f;
		freq *= ws / (64 * 32);
		for(int i = 0; i < octaves; i++)
		{
			total += (1.0f - Math.abs(ssng.looped_noise(x * freq, z * freq, ws))) * amplitude;
			freq*=2.0f;
			maxAmplitude += amplitude;
			amplitude *= persistence;
		}
		return total / maxAmplitude;
	}
	@Override
	public int getTopDataAt(int x, int y)
	{
		//Stones
		return 1;
	}

	@Override
	public int getHeightAt(int x, int z)
	{
		//Don't care about far terrain for now
		return 0;
	}

}
