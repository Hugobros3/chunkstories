#version 450

in vec2 texCoord;
in vec4 color;

out vec4 fragColor;

void main()
{
	fragColor = color;
	//fragColor = vec4(0.5, 0.5, 0.0, 1.0);
	//fragColor = vec4(vertexPos, 1.0 - length(vec2(-1.0) - vertexPos) * 0.5, 1.0);
}