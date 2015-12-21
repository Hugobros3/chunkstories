
<include noise2D.glsl>

uniform vec3 camPos;

vec4 getClouds(vec3 eyeDirection);

vec3 getSkyLight(float time, vec3 eyeDirection)
{
	vec3 V = normalize(eyeDirection);
    vec3 L = normalize(sunPos);
	
    float vl = dot(V, L);
    // Look up the sky color and glow colors.
	//vl+= 1.155;
	//vl = abs(vl);
	vl *= 0.1;
	vl = clamp(vl, 0.0, 1.0);
    vec4 Kg = texture2D(glowSampler, vec2(time, 1-vl));
	vec3 rgbSky = texture2D(colorSampler, vec2(time, clamp(1-normalize(eyeDirection).y*0.75, -1.0, 1.0))).rgb;
    vec3 skyColor = vec3(vec3(0.7) + vec3(0.5) * rgbSky + 0 * Kg.rgb * Kg.a / 2.0);
	
	skyColor = vec3(1.0);
	
	vec4 cloudsColor = getClouds(eyeDirection);
	
	skyColor += max(vec3(15,15,15)*pow(clamp(dot(V, L), 0.0, 1.0), 1750.0), 0.0);
	
	vec3 combined = mix(skyColor, vec3(1.0), clamp(cloudsColor.a, 0.0, 1.0));
	
	return combined;
}

vec3 getSkyColor(float time, vec3 eyeDirection)
{
	vec3 V = normalize(eyeDirection);
    vec3 L = normalize(sunPos);
	
    float vl = dot(V, L);
    // Look up the sky color and glow colors.
	vl+= 1.0;
	//vl = abs(vl);
	vl *= 0.5;
	vl = clamp(vl, 0.0, 1.0);
    vec4 Kg = texture2D(glowSampler, vec2(time, 1.0-vl));
	vec3 rgbSky = texture2D(colorSampler, vec2(time, clamp(1-normalize(eyeDirection).y*0.75, -1.0, 1.0))).rgb;
    vec3 skyColor = vec3(rgbSky + Kg.rgb * Kg.a / 2.0);
	
	vec4 cloudsColor = getClouds(eyeDirection);
	
	skyColor += 0.0 * max(vec3(15,15,15)*pow(clamp(dot(V, L), 0.0, 1.0), 1750.0), 0.0);
	
	vec3 combined = mix(skyColor, cloudsColor.rgb, clamp(cloudsColor.a, 0.0, 1.0));
	
	return combined;
}

vec4 getClouds(vec3 eyeDirection)
{
	<ifdef doClouds>
	if(eyeDirection.y <= 0)
		return vec4(0);
		
	vec2 cloudsPosition = ( -camPos.xz + ( eyeDirection.xz * ((1024.0 + camPos.y) / eyeDirection.y) ) );
	vec2 coords = 0.001 * cloudsPosition + vec2(0, time*25);
	vec4 clouds = vec4(gl_Fog.color.rgb+vec3(0.10), 1.0);
	
	clouds.a = clamp(snoise(coords*0.2), -1.0, 1.0);
	clouds.a *= clamp(snoise(coords*0.4+vec2(77, 33)),0.0, 1.5);
	clouds.a += 1.0+snoise(coords*0.5+vec2(154, 1687));
	clouds.a *= 0.5+snoise(coords*0.5+vec2(-0.15, 1687));
	
	//clouds.a -= 0.2;
	
	float cancer = (length(coords * 10) - 384.0);
	if(cancer < 0)
	{
		clouds.a += 0.05*snoise(coords*16+vec2(-335, 11));
		clouds.a += 0.1*snoise(coords*8+vec2(99, 3));
		clouds.a += 0.2*snoise(coords*4+vec2(11, 5));
		clouds.a += 0.4*snoise(coords*2+vec2(-78148, 3));
	}
	
	float distantFade = clamp((length( eyeDirection.xz * ((1024.0 + camPos.y) / eyeDirection.y) ) - 35000.0), 0.0, 1.0) ;
	clouds.a -= distantFade;
	
	//clouds.a += 1.5;
	
	if(clouds.a <= 0)
		return vec4(0);
	
	if(clouds.a > 0.5)
		clouds.rgb = mix(clouds.rgb, 0.75*clouds.rgb, (clouds.a-0.5)*0.5);
	
	return clamp(clouds, 0.0, 1.0);
	<endif doClouds>
	return vec4(0);
}