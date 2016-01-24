uniform sampler2D glowSampler;
uniform sampler2D colorSampler;
uniform sampler2D ntm;
uniform samplerCube skybox;

uniform vec3 sunPos;
uniform vec3 camPos;

varying vec2 vertex;
varying vec4 texcoords;

uniform float time;

varying vec3 eyeDirection;

<include noise2D.glsl>

void main()
{
	if(eyeDirection.y <= 0)
		discard;
	vec2 coords = 0.001 * ( -camPos.xz + ( eyeDirection.xz * ((1024.0 + camPos.y) / eyeDirection.y) ) );
	vec4 clouds = vec4(gl_Fog.color.rgb+vec3(0.2), 1.0);
	clouds.a = clamp(snoise(coords), 0.0, 1.0);
	clouds.a *= clamp(snoise(coords*0.2+vec2(77, 33))+1.0,0.0, 1.5);
	clouds.a += 1.0+snoise(coords*0.5+vec2(154, 1687));
	clouds.a *= 1.0+snoise(coords*0.05+vec2(-0.15, 1687));
	
	float cancer = (length(coords * 10) - 384.0);
	if(cancer < 0)
	{
		clouds.a += 0.1*snoise(coords*8+vec2(99, 3));
		clouds.a += 0.2*snoise(coords*4+vec2(11, 5));
		clouds.a += 0.4*snoise(coords*2+vec2(-78148, 3));
	}
	
	//clouds.a *= (1+snoise(coords*3+vec2(12, 44)));
	//clouds.rgb -= vec3(0.5) * (clamp( ( clouds.a-1)*snoise(coords*3+vec2(12, 44)) , 0.0, 1.0));
	
	if(clouds.a <= 0)
		discard;
		
	
    gl_FragData[0] = vec4(clouds.rgb, 0.0);
	gl_FragData[1] = vec4(0);
	gl_FragData[2] = vec4(vec3(1.0), clamp(clouds.a, 0.0, 1.0));
	gl_FragData[3] = vec4(0);	
	
}