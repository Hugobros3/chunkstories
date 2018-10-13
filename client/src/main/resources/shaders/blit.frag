#version 450
uniform sampler2D diffuseTexture;
 
in vec2 texCoord;
out vec4 fragColor;

in vec3 color;

void main()
{
	//fragColor = texture(diffuseTexture, texCoord);
	fragColor = vec4(color, 1.0);
}