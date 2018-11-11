#version 450

in vec2 vertexIn;
in vec2 texCoordIn;
in vec4 colorIn;
in int textureIdIn;

out vec2 texCoord;
out vec4 color;
out flat int textureId;

void main()
{
	gl_Position = vec4(vertexIn.xy, 0.0, 1.0);

	textureId = textureIdIn;
	texCoord = texCoordIn;
    color = colorIn;
}