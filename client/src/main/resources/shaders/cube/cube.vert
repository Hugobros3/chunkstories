#version 450

//in int gl_VertexID;
in vec3 vertexIn;
in vec2 texCoordIn;

out vec2 texCoord;
out vec4 color;

#include struct <io.xol.chunkstories.api.graphics.Camera>
uniform Camera camera;

void main()
{
	texCoord = texCoordIn;
	color = vec4(gl_VertexIndex * 0.5, gl_VertexIndex * 0.125, gl_VertexIndex * 0.125 * 0.5, 1.0);
	gl_Position = camera.projectionMatrix * camera.viewMatrix * vec4(vertexIn.xyz, 1.0);
}