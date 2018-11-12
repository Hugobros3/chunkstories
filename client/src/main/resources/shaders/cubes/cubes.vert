#version 450

//in int gl_VertexID;
in vec3 vertexIn;
//in vec2 texCoordIn;

//in vec3 cubePositionIn;
//in vec3 cubeColorIn;

out vec2 texCoord;
out vec4 color;

#include struct <io.xol.chunkstories.api.graphics.structs.Camera>
uniform Camera camera;

void main()
{
	//texCoord = texCoordIn;
	//color = vec4(cubeColorIn, 1.0);
	texCoord = vec2(0.0);
	color = vec4(0.5, 0.9, 0.1, 1.0);
	gl_Position = camera.projectionMatrix * camera.viewMatrix * vec4(vertexIn.xyz/* + cubePositionIn.xyz*/, 1.0);
}