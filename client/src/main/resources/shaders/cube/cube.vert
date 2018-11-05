#version 450

in vec3 vertexIn;
in vec2 texCoordIn;

out vec2 texCoord;

#include struct <io.xol.chunkstories.api.graphics.Camera>
uniform Camera camera;

void main()
{
	texCoord = texCoordIn;
	gl_Position = camera.projectionMatrix * camera.viewMatrix * vec4(vertexIn.xyz, 1.0);
}