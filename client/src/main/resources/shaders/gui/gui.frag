#version 450

in vec2 texCoord;
in vec4 color;
//in flat int textureId;

//uniform sampler2D currentTexture;

out vec4 fragColor;

void main()
{
	//fragColor = vtexture2D(textureId, texCoord) * color;
	
	//fragColor = texture(currentTexture, texCoord) * color;

	fragColor = vec4(1.0, 0.0, 1.0, 1.0);
}