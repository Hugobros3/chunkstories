uniform sampler2D glowSampler;
uniform float isRaining;
uniform sampler2D colorSampler;
uniform sampler2D comp_diffuse;
uniform samplerCube environmentCubemap;

uniform vec3 sunPos;

varying vec2 vertex;

uniform float time;

uniform vec3 camPos;

varying vec3 eyeDirection;

const float gamma = 2.2;
const float gammaInv = 0.45454545454;

vec4 texture2DGammaIn(sampler2D sampler, vec2 coords)
{
	return pow(texture2D(sampler, coords), vec4(gamma));
}

vec4 gammaOutput(vec4 inputValue)
{
	return pow(inputValue, vec4(gammaInv));
}

<include sky.glsl>

void main()
{
	//if(texture2D(comp_diffuse, vertex*0.5 + vec2(0.5)).a > 0.2)
	//	discard;
    
	//vec4 envmap = textureCube(environmentCubemap, -vec3(-eyeDirection.x, eyeDirection.yz));
	//gl_FragColor = mix(vec4(getSkyColor(time, eyeDirection), 1.0), vec4(envmap.rgb, 1.0), envmap.a);
	
	gl_FragColor = vec4(getSkyColor(time, eyeDirection), 1.0);
	
	//gl_FragData[0] = vec4(getSkyColor(time, eyeDirection), 0.0);
	//gl_FragData[1] = vec4(0);
	//gl_FragData[2] = vec4(getSkyLight(time, eyeDirection)+vec3(0.0), 0.0);
	//gl_FragData[3] = vec4(0);	
	
	//gl_FragData[0] = vec4(vec3(1,1,1)*dot(V, L),1);
	//gl_FragData[0] = texture(skybox,V);
	//gl_FragData[0] = textureCube(skybox, normalize(eyeDirection));
	//gl_FragData[0] = texture2D(comp_diffuse, vertex);
}
