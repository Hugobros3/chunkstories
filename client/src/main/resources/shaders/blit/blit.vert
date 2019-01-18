#version 450

in vec2 vertexIn;
out vec2 texCoord;

void main()
{
	gl_Position = vec4(vertexIn.xy, 0.0, 1.0);
	texCoord = vertexIn.xy*0.5+0.5;
}