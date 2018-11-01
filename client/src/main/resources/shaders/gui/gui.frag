#version 450
//uniform sampler2D diffuseTexture;
layout(set=0, location=0) uniform sampler2D virtualTextures[32];
 
in vec2 texCoord;
in vec4 color;
in flat int textureId;

out vec4 fragColor;

//in vec3 color;

void main()
{
	fragColor = texture(virtualTextures[textureId], texCoord);
	//fragColor = vec4(texCoord, textureId * 0.25, 1.0) * color;
}