#version 330
// Copyright 2015 XolioWare Interactive

in float height;
out vec4 fragColor;

void main()
{
	//Diffuse G-Buffer
	gl_FragDepth = height / 32.0;
	fragColor = vec4(1.0);
}