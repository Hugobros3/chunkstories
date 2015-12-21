package io.xol.engine.math.lalgb;

import java.nio.FloatBuffer;
import java.util.Random;

import org.lwjgl.BufferUtils;

//(c) 2015-2016 XolioWare Interactive

public class LinearAlgebraTest
{
	public static void main(String args[])
	{
		org.lwjgl.util.vector.Matrix4f test = new org.lwjgl.util.vector.Matrix4f();
		//for(int a = 0; a < 3; a++)
		//	for(int b = 0; b < 3; b++)
		//		test.
		test.m01 = 0.1f;
		test.m10 = 0.5f;
		System.out.println(test);
		FloatBuffer buf = BufferUtils.createFloatBuffer(16);
		test.store(buf);
		buf.rewind();
		while(buf.hasRemaining())
			System.out.print(buf.get() + " ");
		
		Random rng = new Random();
		
		for(int i = 0; i < 150; i++)
		{
			// This test ensures that the two matrix codes perform in the same way
			
			org.lwjgl.util.vector.Matrix4f a = new org.lwjgl.util.vector.Matrix4f();
			Matrix4f b = new Matrix4f();
			assertEqual(a, b);
			buf.clear();
			for(int f = 0; f < 16; f++)
			{
				if(Math.random() > 0.5)
					buf.put(rng.nextFloat() * 4096f);
				else
					buf.put(rng.nextFloat() * 2f - 1.0f);
			}
			
			buf.rewind();
			
			a.load(buf);
			b.load(buf);
			
			//System.out.println(a+"\n"+b+"\n");
			
			assertEqual(a, b);
		}
		
		System.out.println("All checks passed ok !");
	}
	
	public static void assertEqual(org.lwjgl.util.vector.Matrix4f a, Matrix4f b)
	{
		assert a.m00 == b.m00;
		assert a.m01 == b.m01;
		assert a.m02 == b.m02;
		assert a.m03 == b.m03;
		
		assert a.m10 == b.m10;
		assert a.m11 == b.m11;
		assert a.m12 == b.m12;
		assert a.m13 == b.m13;
		
		assert a.m20 == b.m20;
		assert a.m21 == b.m21;
		assert a.m22 == b.m22;
		assert a.m23 == b.m23;
		
		assert a.m30 == b.m30;
		assert a.m31 == b.m31;
		assert a.m32 == b.m32;
		assert a.m33 == b.m33;
	}
}
