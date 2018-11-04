#version 450

in vec2 vertexIn;
out vec2 vertexPos;

void main()
{
	vertexPos = vertexIn;
	gl_Position = vec4(vertexIn.xy, 0.0, 1.0);
}