#version 150 core
//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

//Vertex inputs
in vec2 vertexIn;

//Passed variables
out vec2 screenCoord;

void main(void)
{
	gl_Position = vec4(vertexIn.xy, 0.0, 1.0);
	screenCoord = vertexIn.xy*0.5+0.5;
}