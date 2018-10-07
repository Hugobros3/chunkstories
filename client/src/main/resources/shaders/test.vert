#version 450
//uniform vec2 screenSize;

/*layout(std140) uniform MatrixBlock {
	mat4 firstMatrix;
	mat4 secondMatrix;
} howdy;

layout(std140) uniform CameraInfoTest {
	mat4 viewMatrix;
	mat4 projectionMatrix;
} cameraData;*/

#include struct <xyz.chunkstories.client.CameraTest>
uniform CameraTest cameraData;

in vec3 vertexIn;
out vec2 texCoord;

void main()
{
	gl_Position = cameraData.viewMatrix * vec4(vertexIn.xy, 0.0, 1.0);
	texCoord = vertexIn.xy*0.5;
}