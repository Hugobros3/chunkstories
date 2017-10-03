#version 330
//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io


out vec2 screenCoord;

in vec2 vertexIn;
void main(void)
{
	gl_Position = vec4(vertexIn.xy, 0.0, 1.0);
	screenCoord = vertexIn.xy*0.5+0.5;
}