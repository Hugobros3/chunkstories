#version 130
//(c) 2015 XolioWare Interactive

uniform sampler2D sampler;
uniform float useTexture;
in vec2 texCoordPassed;
in vec4 colorPassed;

void main()
{
	if(useTexture > 0.5)
		gl_FragColor = colorPassed * texture2D(sampler, texCoordPassed);// + vec4(1.0, 0.0, 1.0, 0.5);
	else
		gl_FragColor = colorPassed;// + vec4(1.0, 0.0, 1.0, 0.5);
}