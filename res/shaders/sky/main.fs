uniform sampler2D glowSampler;
uniform sampler2D colorSampler;
uniform sampler2D comp_diffuse;
uniform samplerCube skybox;

uniform vec3 sunPos;

varying vec2 vertex;

uniform float time;

varying vec3 eyeDirection;

<include sky.glsl>

void main()
{
	//if(texture2D(comp_diffuse, vertex*0.5 + vec2(0.5)).a > 0.2)
	//	discard;
    gl_FragData[0] = vec4(getSkyColor(time, eyeDirection), 1.0);
	gl_FragData[1] = vec4(0);
	gl_FragData[2] = vec4(getSkyLight(time, eyeDirection)+vec3(0.0), 1.0);
	gl_FragData[3] = vec4(0);	
	
	//gl_FragData[0] = vec4(vec3(1,1,1)*dot(V, L),1);
	//gl_FragData[0] = texture(skybox,V);
	//gl_FragData[0] = textureCube(skybox, normalize(eyeDirection));
	//gl_FragData[0] = texture2D(comp_diffuse, vertex);
}