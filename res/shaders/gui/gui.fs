#version 150 core
//(c) 2015 XolioWare Interactive

uniform sampler2D sampler;
uniform float useTexture;
in vec2 texCoordPassed;
in vec4 colorPassed;

out vec4 fragColor;

void main()
{
	if(useTexture > 0.5)
		fragColor = colorPassed * texture(sampler, texCoordPassed);// + vec4(1.0, 0.0, 1.0, 0.5);
	else
		fragColor = colorPassed;// + vec4(1.0, 0.0, 1.0, 0.5);
}