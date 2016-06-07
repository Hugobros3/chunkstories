#version 120
// Copyright 2015 XolioWare Interactive

varying float height;

void main()
{
	//Diffuse G-Buffer
	gl_FragDepth = height / 32.0;
	gl_FragColor = vec4(1.0);
}