#version 450
uniform sampler2D diffuseTexture;
 
in vec2 texCoord;
in vec4 color;

out vec4 fragColor;

//in vec3 color;

void main()
{
	fragColor = texture(diffuseTexture, texCoord) * color;
}