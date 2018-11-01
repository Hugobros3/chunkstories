#version 450

layout(location = 0) in vec2 vertexIn;
in vec2 texCoordIn;
in vec4 colorIn;
in int textureIdIn;

out vec2 texCoord;
out vec4 color;
out flat int textureId;

//in int gl_VertexID; 
//#include struct <io.xol.chunkstories.graphics.vulkan.shaders.UniformTestOffset>
//uniform UniformTestOffset uniformTest;


void main()
{
	//color = vec3(gl_VertexIndex / 4.0, gl_VertexIndex / 8.0, gl_VertexIndex / 16.0);
	//gl_Position = vec4(vertexIn.xy + uniformTest.offset, 0.0, 1.0);
	gl_Position = vec4(vertexIn.xy, 0.0, 1.0);

	textureId = textureIdIn;
	texCoord = texCoordIn;
    color = colorIn;
}