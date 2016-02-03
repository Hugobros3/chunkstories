//#version 120

uniform sampler2D shadedBuffer;

varying vec2 screenCoord;

uniform float apertureModifier;

const float gamma = 2.2;
const float gammaInv = 1/2.2;

vec4 texture2DGammaIn(sampler2D sampler, vec2 coords)
{
	return pow(texture2D(sampler, coords), vec4(gamma));
}

vec4 gammaOutput(vec4 inputValue)
{
	return pow(inputValue, vec4(gammaInv));
}

float luminance(vec3 color)
{
	return color.r * 0.2125 + color.g * 0.7154 + color.b * 0.0721;
}

void main()
{	
	vec3 finalLight = texture2DLod(shadedBuffer, screenCoord, 0.0).rgb;
	float lum = luminance(finalLight) * apertureModifier;
	
	finalLight = pow(finalLight, vec3(gammaInv));
	finalLight *= clamp(lum-1.0, 0.0, 1000.0);
	
	gl_FragColor = vec4(finalLight, 1.0);
}
