#version 120
uniform sampler2D skyTextureSunny;
uniform sampler2D skyTextureRaining;
uniform sampler2D glowSampler;
uniform float overcastFactor;

uniform float isRaining;
uniform sampler2D comp_diffuse;
uniform samplerCube environmentCubemap;

uniform vec3 sunPos;

varying vec2 vertex;

uniform float time;

uniform vec3 camPos;

varying vec3 eyeDirection;

const float gamma = 2.2;
const float gammaInv = 0.45454545454;

vec4 texture2DGammaIn(sampler2D sampler, vec2 coords)
{
	return pow(texture2D(sampler, coords), vec4(gamma));
}

vec4 gammaOutput(vec4 inputValue)
{
	return pow(inputValue, vec4(gammaInv));
}

<include sky.glsl>

void main()
{
	gl_FragColor = vec4(getSkyColor(time, eyeDirection), 1.0);
}
