//#version 120

uniform sampler2D lightTexture;
uniform sampler2D normalTexture;
uniform sampler2D diffuseTexture;

varying vec2 screenCoord;

float luminance(vec3 color)
{
	return color.r * 0.2125 + color.g * 0.7154 + color.b * 0.0721;
}

void main() {
	vec3 finalLight = texture2D(lightTexture, screenCoord).rgb;
	
	//finalLight = vec3(1.0);
	
	//finalLight /= 5.0;
	
	/*if(finalLight.x > 1)
		finalLight.x = 1.0 + log(finalLight.x)/2.0;
	if(finalLight.y > 1)
		finalLight.y = 1.0 + log(finalLight.y)/2.0;
	if(finalLight.z > 1)
		finalLight.z = 1.0 + log(finalLight.z)/2.0;*/
	
	vec4 normal = texture2D(normalTexture, screenCoord);
	vec4 diffuse = texture2D(diffuseTexture, screenCoord);
	//if(length(normal.xyz) < 0.5)
	finalLight *= diffuse.rgb;
	float lum = luminance(finalLight);
	
	
	finalLight *= clamp(lum-0.6, 0.0, 10.0);
	
	//	finalLight += vec3(1.0);
		//finalLight += (diffuse.rgb) * 0.5 + vec3(0);
	
	//finalLight = clamp(finalLight, 0.0, 2.0);
	gl_FragColor = vec4(finalLight, 1.0);
}
