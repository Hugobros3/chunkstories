#version 120
//(c) 2015 XolioWare Interactive

uniform sampler2D sampler;
uniform vec4 color;
uniform float useTexture;
varying vec2 texCoordPassed;

void main()
{
	if(useTexture > 0.5)
		gl_FragColor = color * texture2D(sampler, texCoordPassed);// + vec4(1.0, 0.0, 1.0, 0.5);
	else
		gl_FragColor = color;// + vec4(1.0, 0.0, 1.0, 0.5);
}