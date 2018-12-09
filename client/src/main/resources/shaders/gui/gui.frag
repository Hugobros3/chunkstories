#version 450

layout(set=0, location=0) uniform sampler2D virtualTextures[1024];
 
in vec2 texCoord;
in vec4 color;
in flat int textureId;

out vec4 fragColor;

void main()
{
	fragColor = texture(virtualTextures[textureId], texCoord) * color;
	//fragColor = vec4(texture(virtualTextures[textureId], texCoord).a, 0.0, 1.0, 0.5);
}