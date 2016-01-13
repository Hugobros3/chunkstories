//#version 120

uniform sampler2D shadedBuffer;

varying vec2 screenCoord;

float luminance(vec3 color)
{
	return color.r * 0.2125 + color.g * 0.7154 + color.b * 0.0721;
}

void main()
{	
	vec3 finalLight = texture2D(shadedBuffer, screenCoord).rgb;
	float lum = luminance(finalLight);
	
	finalLight *= clamp(lum-0.8, 0.0, 10.0);
	
	gl_FragColor = vec4(finalLight, 1.0);
}
