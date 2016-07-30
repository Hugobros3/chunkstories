#version 130
//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

//Passed variables
in vec2 texCoordPassed;
in vec3 eyeDirection;

//Framebuffer outputs
// out vec4 shadedFramebufferOut;

//Sky data
uniform sampler2D diffuseTexture;
uniform sampler2D zBuffer;

uniform vec2 screenViewportSize;

//World
uniform float time;

//Gamma constants
<include ../lib/gamma.glsl>

void main()
{
	vec4 color = texture2D(diffuseTexture, texCoordPassed);
	
	float depth = texture2D(zBuffer, gl_FragCoord.xy / screenViewportSize ).x;
	
	/*if(abs(depth - gl_FragCoord.z) > 0.001)
		discard;*/
	
	//Debug : shows blue when out of bounds !
	/*if(texCoordPassed.x < 0.0 || texCoordPassed.x > 1.0)
		color = vec4(0.0, 0.0,1.0, 1.0);
		
	if(texCoordPassed.y < 0.0 || texCoordPassed.y > 1.0)
		color = vec4(0.0, 0.0,1.0, 1.0);*/
	
	//color = vec4(vec3(depth), 1.0);
	
	//if(color.a < 0.5)
	//	discard;
	
	//color.rgb = vec3(texCoordPassed, 0.0);
	
	gl_FragDepth = gl_FragCoord.z - 0.0001;
	
	//Diffuse G-Buffer
	gl_FragData[0] = vec4(color.rgb, color.a);
	//Normal G-Buffer + reflections
	gl_FragData[1] = vec4(0.5, 0.5, 1.0, 0.0);
	//Light color G-buffer
	gl_FragData[2] = vec4(vec2(0.0, 1.0), 0.0, 0.0);
}