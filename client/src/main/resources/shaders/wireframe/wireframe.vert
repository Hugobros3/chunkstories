#version 450

in vec3 vertexIn;
out vec4 color;

#include struct <io.xol.chunkstories.api.graphics.structs.Camera>
uniform Camera camera;

void main()
{
	color = vec4(1.0, 0.0, 0.0, 1.0);
	gl_Position = camera.projectionMatrix * camera.viewMatrix * vec4(vertexIn.xyz, 1.0);
}