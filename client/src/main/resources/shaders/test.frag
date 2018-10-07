#version 450 core

#include struct <xyz.chunkstories.client.FullscreenFillColor>
uniform FullscreenFillColor fillColor;

#define texureHandle int
#define vtexture(textureId, y) texture(virtualTextures[textureId], y)
uniform sampler2D virtualTextures[64];
uniform sampler2D textureFuckYou;
 
layout(location = 0) in vec2 texCoord;
layout(location = 0) out vec4 fragColor;
 /*
layout(std140) uniform WorldInformation {
	vec4 grassColor;
	vec4 cloudsColor;
	texureHandle albedoTexture;
} worldInformation;

layout(std140) uniform LightingInformation {
	vec4 sunlightColor;
} lightingInformation;*/

void main()
{
	fragColor = texture(textureFuckYou, texCoord) * fillColor.color;
	//fragColor = worldInformation.cloudsColor + lightingInformation.sunlightColor * vtexture(worldInformation.albedoTexture, texCoord);

	//fragColor = worldInformation.cloudsColor + lightingInformation.sunlightColor * texture(virtualTextures[worldInformation.albedoTexture], texCoord);
	//fragColor = vec4(1.0, 0.0, 1.0, 1.0);
}