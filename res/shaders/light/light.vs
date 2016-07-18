#version 130
//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

in vec2 vertexIn;

out vec2 screenCoord;
 
void main(void)
{
	//Transform clip space to texcoord
	screenCoord = vertexIn.xy * 0.5 + vec2(0.5);
	gl_Position = vec4(vertexIn.xy, 0.0, 1.0);
}