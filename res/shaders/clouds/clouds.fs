#version 130
//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

//Passed variables
in float alphaPassed;
in vec3 eyeDirection;

//Framebuffer outputs
out vec4 shadedFramebufferOut;

//Sky data
uniform sampler2D sunSetRiseTexture;
uniform sampler2D skyTextureSunny;
uniform sampler2D skyTextureRaining;
uniform vec3 sunPos;
uniform float overcastFactor;

//World
uniform float time;

//Gamma constants
<include ../lib/gamma.glsl>

//Sky functions
<include ../sky/sky.glsl>

void main()
{
	vec4 color = vec4(getSkyColor(time, eyeDirection), 1.0);
	
	vec3 cloudsColor = getSkyTexture(vec2(time, 1.0)).rgb;
	
	color.rgb += 0.125 * cloudsColor * clamp(alphaPassed, 0.0, 1.0);
	
	shadedFramebufferOut = color;
}