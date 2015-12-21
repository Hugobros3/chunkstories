//#version 120

uniform sampler2D comp_final;

uniform sampler2D comp_diffuse;
uniform sampler2D comp_normal;
uniform sampler2D comp_depth;
uniform sampler2D comp_light;
uniform sampler2D comp_specular;
uniform sampler2D comp_sm;
uniform sampler2D bloom;
uniform sampler2D ssao;

uniform samplerCube skybox;

varying vec2 f_texcoord;

varying vec2 scaledPixel;

uniform float viewWidth;
uniform float viewHeight;

uniform mat4 projectionMatrix;
uniform mat4 projectionMatrixInv;

uniform mat4 modelViewMatrix;
uniform mat4 modelViewMatrixInv;

uniform mat3 normalMatrix;
uniform mat3 normalMatrixInv;

uniform vec3 sunPos;

uniform float time;
uniform float underwater;

const vec4 waterColor = vec4(51/255.0, 104/255.0, 110/255.0, 1.0);

vec4 getDebugShit();

vec3 convertCameraSpaceToScreenSpace(vec3 cameraSpace) {
    vec4 clipSpace = projectionMatrix * vec4(cameraSpace, 1.0);
    vec3 NDCSpace = clipSpace.xyz / clipSpace.w;
    vec3 screenSpace = 0.5 * NDCSpace + 0.5;
		 screenSpace.z = 0.1f;
    return screenSpace;
}

vec3 convertScreenSpaceToWorldSpace(vec2 co) {

    vec4 fragposition = projectionMatrixInv * vec4(vec3(co*2-1, texture2D(comp_depth, co, 0).x * 2.0 - 1.0), 1.0);
    fragposition /= fragposition.w;
    return fragposition.xyz;
}

float linearizeDepth(float z)
{
  float n = 0.1; // camera z near
  float f = 300.0; // camera z far
  return (2.0 * n) / (f + n - z * (f - n));	
}

vec3 addBloom(vec2 cc)
{
	vec3 baseColor = texture2D(bloom, cc).rgb;
	return baseColor / 1.0;
}

void main() {	
	//gl_FragColor = bloom(comp_final, f_texcoord);
	
	vec2 finalCoords = f_texcoord;
	
	finalCoords.x += underwater*sin(finalCoords.x * 50 + finalCoords.y * 60 + time * 1.0) / viewWidth * 5.0;
	finalCoords.y += underwater*cos(finalCoords.y * 60 + time * 1.0) / viewHeight * 2.0;
	
	vec4 compositeColor = texture2D(comp_final, finalCoords);
	
	compositeColor = mix(compositeColor, compositeColor * waterColor, underwater * 0.5);
	
	<ifdef doBloom>
	compositeColor.rgb += addBloom(finalCoords);
	<endif doBloom>
	
	<ifdef ssao>
	compositeColor.rgb *= texture2D(ssao, finalCoords).rgb;
	<endif ssao>
	
	gl_FragColor = compositeColor;
	<ifdef debugGBuffers>
	gl_FragColor = getDebugShit();
	<endif debugGBuffers>
}


vec4 getDebugShit()
{
	vec4 baseColor = vec4(0.0);
	if(f_texcoord.x > 0.666)
	{
		if(f_texcoord.y > 0.666)
		{
			vec2 cc = vec2((f_texcoord.x-0.666)*3,(f_texcoord.y-0.666)*3);
			baseColor = vec4(texture2D(bloom, cc).rgb,1);
		}
		else if(f_texcoord.y > 0.333)
		{
			vec2 cc = vec2((f_texcoord.x-0.666)*3,(f_texcoord.y-0.333)*3);
			baseColor = vec4(vec3(1.0, 0.5, 0.0) * linearizeDepth(texture2D(comp_depth, cc).x),1);
		}
		else
			baseColor = texture2D(comp_normal, vec2((f_texcoord.x-0.666)*3,(f_texcoord.y+0.333)*3-1));
	}
	else if(f_texcoord.x > 0.333)
	{
		if(f_texcoord.y > 0.666)
		{
			vec2 cc = vec2((f_texcoord.x-0.666)*3+1,(f_texcoord.y-0.666)*3);
			baseColor = texture2D(comp_light, cc);
		}
		else if(f_texcoord.y > 0.333)
		{
			vec2 cc = vec2((f_texcoord.x-0.666)*3+1,(f_texcoord.y-0.333)*3);
			baseColor = texture2D(ssao, cc);
		}
		else
		{
			vec2 cc = vec2((f_texcoord.x-0.666)*3+1,(f_texcoord.y+0.333)*3-1);
			baseColor = vec4((texture2D(comp_specular, cc).rgb),1);
		}
	}
	else
	{
		if(f_texcoord.y > 0.5)
		{
			vec2 cc = vec2((f_texcoord.x-0.333)*3+1,(f_texcoord.y-0.5)*2);
			baseColor = texture2D(comp_final, cc);
		}
		else
		{
			vec2 cc = vec2((f_texcoord.x-0.333)*3+1,(f_texcoord.y+0.5)*2-1);
			baseColor = vec4((texture2D(comp_diffuse, cc).rgb),1);
		}
	}
	return baseColor;
}
