#version 330

#include struct xyz.chunkstories.graphics.vulkan.systems.world.ViewportSize
uniform ViewportSize viewportSize;

in vec2 vertexIn;
out vec2 texCoord;
out vec2 texCoordBlur[14];

//uniform float lookupScale;
#define lookupScale 1.0

void main()
{
	vec2 pixelSize = vec2(1.0) / viewportSize.size;
	//vec2 pixelSize = vec2(1.0) / vec2(512.0);
	gl_Position = vec4(vertexIn.xy, 0.0, 1.0);
	
	texCoord = vertexIn.xy*0.5+0.5;
	texCoord *= lookupScale;
	texCoordBlur[0] = texCoord + vec2(0.0, -7.0) * pixelSize;
	texCoordBlur[1] = texCoord + vec2(0.0, -6.0) * pixelSize;
	texCoordBlur[2] = texCoord + vec2(0.0, -5.0) * pixelSize;
	texCoordBlur[3] = texCoord + vec2(0.0, -4.0) * pixelSize;
	texCoordBlur[4] = texCoord + vec2(0.0, -3.0) * pixelSize;
	texCoordBlur[5] = texCoord + vec2(0.0, -2.0) * pixelSize;
	texCoordBlur[6] = texCoord + vec2(0.0, -1.0) * pixelSize;
	texCoordBlur[7] = texCoord + vec2(0.0,  1.0) * pixelSize;
	texCoordBlur[8] = texCoord + vec2(0.0,  2.0) * pixelSize;
	texCoordBlur[9] = texCoord + vec2(0.0,  3.0) * pixelSize;
	texCoordBlur[10] = texCoord + vec2(0.0,  4.0) * pixelSize;
	texCoordBlur[11] = texCoord + vec2(0.0,  5.0) * pixelSize;
	texCoordBlur[12] = texCoord + vec2(0.0,  6.0) * pixelSize;
	texCoordBlur[13] = texCoord + vec2(0.0,  7.0) * pixelSize;
}