
//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

<include ../lib/noise2D.glsl>

//Internal method to obtain pre-mixed sun gradient color
vec4 getSkyTexture(vec2 coordinates)
{
	float greyFactor = clamp((overcastFactor - 0.2) / 0.5, 0.0, 1.0);
	float darkFactor = clamp((overcastFactor - 0.5), 0.0, 1.0);
	return mix(texture2D(skyTextureSunny, coordinates), mix(texture2D(skyTextureRaining, coordinates), vec4(0.0), darkFactor), greyFactor);
}

//Returns the sky color without sun, depending on direction and time
//Used in fog
//Requires sunPos, skyTextureSunny, skyTextureRaining, sunSetRiseTexture, overcastFactor
vec3 getSkyColorWOSun(float time, vec3 eyeDirection)
{	
	float sunEyeDot = dot(normalize(eyeDirection), normalize(sunPos));
	
	vec4 skyGlow = texture2D(sunSetRiseTexture, vec2(time, clamp(0.5 - sunEyeDot * 0.5, 0.0, 1.0)));
	vec3 skyColor = getSkyTexture(vec2(time, clamp(1.0-normalize(eyeDirection).y, 0.0, 1.0))).rgb;
    
	//Overcast renders sunrise/set aura less visible
	skyColor = mix(skyColor, skyColor * 0.6 + skyGlow.rgb * 0.8, skyGlow.a * 0.5 * clamp(1.0-overcastFactor * 2.0, 0.0, 1.0));
	
	return pow(skyColor, vec3(gamma));
}

//Returns the sky color depending on direction and time
//Requires sunPos, skyTextureSunny, skyTextureRaining, sunSetRiseTexture, overcastFactor
vec3 getSkyColor(float time, vec3 eyeDirection)
{
	float sunEyeDot = dot(normalize(eyeDirection), normalize(sunPos));

	vec4 skyGlow = texture2D(sunSetRiseTexture, vec2(time, clamp(0.5 - pow(sunEyeDot, 2.0) * 0.5, 0.0, 1.0)));
	vec3 skyColor = vec3(0.0);
	
	//We compute the gradient ourselves to avoid color banding
	vec3 skyColorTop = getSkyTexture(vec2(time, 0.0)).rgb;
	vec3 skyColorBot = getSkyTexture(vec2(time, 1.0)).rgb;
	float gradient = clamp(normalize(eyeDirection).y, 0.0, 1.0);
	skyColor = mix(skyColorBot, skyColorTop, gradient);
	
	//Overcast renders sunrise/set aura less visible
	skyColor = mix(skyColor, skyColor * 0.6 + skyGlow.rgb * 0.8, skyGlow.a * 0.5 * clamp(1.0-overcastFactor * 2.0, 0.0, 1.0));
	
	//We add in the sun
	skyColor += clamp(1.0-overcastFactor * 2.0, 0.0, 1.0)*max(vec3(5.0)*pow(clamp(sunEyeDot, 0.0, 1.0), 750.0), 0.0);
	
	return pow(skyColor, vec3(gamma));
}

vec3 getSkyColorDiffuse(float time, vec3 eyeDirection)
{
	float sunEyeDot = 1.0 + dot(normalize(eyeDirection), normalize(sunPos));

	vec4 skyGlow = texture2D(sunSetRiseTexture, vec2(time, clamp(0.5 - sunEyeDot * 0.5, 0.0, 1.0)));
	vec3 skyColor = vec3(0.0);
	
	//We compute the gradient ourselves to avoid color banding
	vec3 skyColorTop = getSkyTexture(vec2(time, 0.0)).rgb;
	vec3 skyColorBot = getSkyTexture(vec2(time, 1.0)).rgb;
	float gradient = clamp(normalize(eyeDirection).y, 0.0, 1.0);
	skyColor = mix(skyColorBot, skyColorTop, gradient);
	
	//Overcast renders sunrise/set aura less visible
	skyColor = mix(skyColor, skyColor * 0.6 + skyGlow.rgb * 0.8, skyGlow.a * 0.5 * clamp(1.0-overcastFactor * 2.0, 0.0, 1.0));
	
	//We add in the sun
	skyColor += clamp(1.0-overcastFactor * 2.0, 0.0, 1.0)*max(vec3(5.0)*pow(clamp(sunEyeDot, 0.0, 1.0), 750.0), 0.0);
	
	return pow(skyColor, vec3(gamma));
}