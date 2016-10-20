#version 150

uniform sampler2D shadedBuffer;

in vec2 screenCoord;

uniform float apertureModifier;
uniform vec2 screenSize;

const float gamma = 2.2;
const float gammaInv = 1/2.2;

uniform float max_mipmap;

out vec4 fragColor;

vec4 textureGammaIn(sampler2D sampler, vec2 coords)
{
	return pow(texture(sampler, coords), vec4(gamma));
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

/*	for(int i = 0; i <= 0 / 2.0; i++)
	{
		float powed = pow(2, i);
		
		vec2 screenCoordFloored = (floor(screenCoord * (screenSize / powed)) + vec2(0.5)) / screenSize * powed;
		
		//float normalizedError = clamp(1.0 - length(screenCoord * (screenSize / powed) - lp), 0.0, 1.0);
		
		finalLight += 1 * textureLod(shadedBuffer, screenCoord, i).rgb / powed;
		finalLight = pow(finalLight, vec3(gammaInv));
		
		//vec2 diff = vec2(0.0);
		
		//finalLight += 1.00 * clamp(1.0 - length((screenCoordFloored - screenCoord) * (screenSize / powed)) , 0.0, 1.0) * textureLod(shadedBuffer, screenCoordFloored + vec2(0.0, 0.0) / (screenSize / powed) , i).rgb / powed;
		//finalLight += 0.25 * textureLod(shadedBuffer, screenCoordFloored + vec2(0.0, 1.0) / (screenSize / powed) , i).rgb / powed;
		//finalLight += 0.25 * textureLod(shadedBuffer, screenCoordFloored + vec2(0.0, -1.0) / (screenSize / powed) , i).rgb / powed;
		//finalLight += 0.25 * textureLod(shadedBuffer, screenCoordFloored + vec2(1.0, 0.0) / (screenSize / powed) , i).rgb / powed;
		//finalLight += 0.25 * textureLod(shadedBuffer, screenCoordFloored + vec2(-1.0, 0.0) / (screenSize / powed) , i).rgb / powed;
	}
		
		*/
	//finalLight = 0.5 * textureLod(shadedBuffer, screenCoord, max_mipmap+1).rgb;
	
	vec3 originalPixelColor = texture(shadedBuffer, screenCoord).rgb;
	originalPixelColor = pow(originalPixelColor, vec3(gammaInv));
	
	float lum = luminance(originalPixelColor) * apertureModifier;
	
	finalLight += clamp(originalPixelColor * (lum - 0.6), vec3(0.0), vec3(1.0)) * 0.4;
	finalLight += clamp(originalPixelColor * lum, vec3(0.0), vec3(1.0)) * 0.10;
	
	fragColor = vec4(finalLight, 1.0);
}
