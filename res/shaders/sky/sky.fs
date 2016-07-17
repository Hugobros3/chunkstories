#version 130

//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

//Passed variables
in vec3 eyeDirection;

//Framebuffer outputs
out vec4 shadedFramebufferOut;

//Sky data
uniform sampler2D skyTextureSunny;
uniform sampler2D skyTextureRaining;
uniform sampler2D sunSetRiseTexture;
uniform float overcastFactor;
uniform vec3 sunPos;

//World
uniform float time;

//Common camera matrices & uniforms
uniform vec3 camPos;

//Gamma constants
<include ../lib/gamma.glsl>

//Sky functions
<include ../sky/sky.glsl>

void main()
{
	//Straight output of library's method
	shadedFramebufferOut = vec4(getSkyColor(time, eyeDirection), 1.0);
}
