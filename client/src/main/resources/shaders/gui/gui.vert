#version 450

in vec2 vertexIn;
in vec2 texCoordIn;
in vec4 colorIn;

out vec2 texCoord;
out vec4 color;

void main()
{
	gl_Position = vec4(vertexIn.xy, 0.0, 1.0);

	texCoord = texCoordIn;
    color = colorIn;
}