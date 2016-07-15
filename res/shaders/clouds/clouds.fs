uniform sampler2D skyTextureSunny;
uniform sampler2D skyTextureRaining;
uniform sampler2D glowSampler;
uniform float overcastFactor;

uniform vec3 sunPos;
uniform vec3 camPos;

varying vec4 vertex;
varying vec4 normal;

uniform float time;

varying vec3 eyeDirection;

const float gamma = 2.2;
const float gammaInv = 0.45454545454;
varying float alpha;

<include ../sky/sky.glsl>

void main()
{
	//vec4 color = mix(vec4(getSkyColor(time, eyeDirection), 1.0), vec4(pow(vec3(1.0), vec3(gamma)), 1.0), 0.05);
	
	vec4 color = vec4(getSkyColor(time, eyeDirection), 1.0);
	
	vec3 skyColorBot = getSkyTexture(vec2(time, 1.0)).rgb;
	
	color.rgb += 0.125 * skyColorBot * clamp(alpha, 0.0, 1.0); 
	
	//color += vec4(pow(vec3(1.0 * 0.25 * clamp(alpha, 0.0, 1.0)), vec3(gamma)), 0.0);
	
    gl_FragData[0] = color;
}