#version 450

layout(location=0) in vec3 vertexIn;
in mat4 lol;

out vec2 texCoord;

void main()
{
	gl_Position = vec4(vertexIn.xy, 0.0, 1.0);
	texCoord = vertexIn.xy*0.5;
}