#version 130

uniform sampler2D shadedBuffer;

varying vec2 screenCoord;

uniform float apertureModifier;
uniform vec2 screenSize;

const float gamma = 2.2;
const float gammaInv = 1/2.2;

uniform float max_mipmap;

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
	vec3 finalLight = vec3(0.0);

	for(int i = 0; i <= 0 / 2.0; i++)
	{
		float powed = pow(2, i);
		
		vec2 screenCoordFloored = (floor(screenCoord * (screenSize / powed)) + vec2(0.5)) / screenSize * powed;
		
		//float normalizedError = clamp(1.0 - length(screenCoord * (screenSize / powed) - lp), 0.0, 1.0);
		
		finalLight += 1 * texture2DLod(shadedBuffer, screenCoord, i).rgb / powed;
		
		//vec2 diff = vec2(0.0);
		
		//finalLight += 1.00 * clamp(1.0 - length((screenCoordFloored - screenCoord) * (screenSize / powed)) , 0.0, 1.0) * texture2DLod(shadedBuffer, screenCoordFloored + vec2(0.0, 0.0) / (screenSize / powed) , i).rgb / powed;
		//finalLight += 0.25 * texture2DLod(shadedBuffer, screenCoordFloored + vec2(0.0, 1.0) / (screenSize / powed) , i).rgb / powed;
		//finalLight += 0.25 * texture2DLod(shadedBuffer, screenCoordFloored + vec2(0.0, -1.0) / (screenSize / powed) , i).rgb / powed;
		//finalLight += 0.25 * texture2DLod(shadedBuffer, screenCoordFloored + vec2(1.0, 0.0) / (screenSize / powed) , i).rgb / powed;
		//finalLight += 0.25 * texture2DLod(shadedBuffer, screenCoordFloored + vec2(-1.0, 0.0) / (screenSize / powed) , i).rgb / powed;
	}
		
		
	//finalLight = 0.5 * texture2DLod(shadedBuffer, screenCoord, max_mipmap+1).rgb;
	float lum = luminance(finalLight) * apertureModifier;
	
	finalLight = pow(finalLight, vec3(gammaInv));
	finalLight *= clamp(lum-0.4, 0.0, 1000.0);
	
	gl_FragColor = vec4(finalLight * 0.4, 1.0);
}
