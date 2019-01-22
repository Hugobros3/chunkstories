#version 450

in vec2 texCoord;
in vec4 color;
in flat int textureId;

out vec4 fragColor;

void main()
{
	fragColor = vtexture2D(textureId, texCoord) * color;
}