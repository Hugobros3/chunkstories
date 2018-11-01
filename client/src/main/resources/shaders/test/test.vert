#version 450

in vec2 vertexIn;


void main()
{
	gl_Position = vec4(vertexIn.xy, 0.0, 1.0);
}