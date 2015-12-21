/*
 * Copyright (c) 2013, Oskar Veerhoek
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those
 * of the authors and should not be interpreted as representing official policies,
 * either expressed or implied, of the FreeBSD Project.
 */

package io.xol.engine.model;

import org.lwjgl.BufferUtils;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class BufferTools
{

	/**
	 * @param v
	 *            the vector that is to be turned into an array of floats
	 *
	 * @return a float array where [0] is v.x, [1] is v.y, and [2] is v.z
	 */
	public static float[] asFloats(Vector3f v)
	{
		return new float[] { v.x, v.y, v.z };
	}

	/**
	 * @param elements
	 *            the amount of elements to check
	 *
	 * @return true if the contents of the two buffers are the same, false if
	 *         not
	 */
	public static boolean bufferEquals(FloatBuffer bufferOne,
			FloatBuffer bufferTwo, int elements)
	{
		for (int i = 0; i < elements; i++)
		{
			if (bufferOne.get(i) != bufferTwo.get(i))
			{
				return false;
			}
		}
		return true;
	}

	/**
	 * @param values
	 *            the byte values that are to be turned into a readable
	 *            ByteBuffer
	 *
	 * @return a readable ByteBuffer
	 */
	public static ByteBuffer asByteBuffer(byte... values)
	{
		ByteBuffer buffer = BufferUtils.createByteBuffer(values.length);
		buffer.put(values);
		return buffer;
	}

	/**
	 * @param buffer
	 *            a readable buffer
	 * @param elements
	 *            the amount of elements in the buffer
	 *
	 * @return a string representation of the elements in the buffer
	 */
	public static String bufferToString(FloatBuffer buffer, int elements)
	{
		StringBuilder bufferString = new StringBuilder();
		for (int i = 0; i < elements; i++)
		{
			bufferString.append(" ").append(buffer.get(i));
		}
		return bufferString.toString();
	}

	/**
	 * @param matrix4f
	 *            the Matrix4f that is to be turned into a readable FloatBuffer
	 *
	 * @return a FloatBuffer representation of matrix4f
	 */
	public static FloatBuffer asFloatBuffer(Matrix4f matrix4f)
	{
		FloatBuffer buffer = BufferUtils.createFloatBuffer(16);
		matrix4f.store(buffer);
		return buffer;
	}

	/**
	 * @param matrix4f
	 *            the Matrix4f that is to be turned into a FloatBuffer that is
	 *            readable to OpenGL (but not to you)
	 *
	 * @return a FloatBuffer representation of matrix4f
	 */
	public static FloatBuffer asFlippedFloatBuffer(Matrix4f matrix4f)
	{
		FloatBuffer buffer = BufferUtils.createFloatBuffer(16);
		matrix4f.store(buffer);
		buffer.flip();
		return buffer;
	}

	/**
	 * @param values
	 *            the float values that are to be turned into a readable
	 *            FloatBuffer
	 *
	 * @return a readable FloatBuffer containing values
	 */
	public static FloatBuffer asFloatBuffer(float... values)
	{
		FloatBuffer buffer = BufferUtils.createFloatBuffer(values.length);
		buffer.put(values);
		return buffer;
	}

	/**
	 * @param amountOfElements
	 *            the amount of elements in the FloatBuffers
	 *
	 * @return an empty FloatBuffer with a set amount of elements
	 */
	public static FloatBuffer reserveData(int amountOfElements)
	{
		return BufferUtils.createFloatBuffer(amountOfElements);
	}

	/**
	 * @param values
	 *            the float values that are to be turned into a FloatBuffer
	 *
	 * @return a FloatBuffer readable to OpenGL (not to you!) containing values
	 */
	public static FloatBuffer asFlippedFloatBuffer(float... values)
	{
		FloatBuffer buffer = BufferUtils.createFloatBuffer(values.length);
		buffer.put(values);
		buffer.flip();
		return buffer;
	}
}
