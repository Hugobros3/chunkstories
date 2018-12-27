#version 450

in vec2 vertexPos;
out vec4 fragColor;

uniform sampler2D colorBuffer;
uniform sampler2D normalBuffer;

void main()
{
	fragColor = vec4(texture(colorBuffer, vec2(vertexPos.x * 0.5 + 0.5, 0.5 - vertexPos.y * 0.5)).rgb, 1.0);
}