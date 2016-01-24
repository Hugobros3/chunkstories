varying vec4 texcoord;
varying vec2 lightMapCoords;

//Lighthing
uniform float sunIntensity;

// The normal we're going to pass to the fragment shader.
varying vec3 varyingNormal;
// The vertex we're going to pass to the fragment shader.
varying vec4 varyingVertex;

varying float fogI;

uniform float time;
varying vec3 eye;
uniform vec3 camPos;
uniform vec3 borderShift;

uniform float vegetation;
varying float chunkFade;
uniform float viewDistance;

varying vec4 modelview;

attribute vec4 billboardCoord;
attribute vec2 planeCoord;
attribute vec2 textureCoord;

uniform mat4 projectionMatrix;
uniform mat4 projectionMatrixInv;

uniform mat4 modelViewMatrix;
uniform mat4 modelViewMatrixInv;

uniform mat3 normalMatrix;
uniform mat3 normalMatrixInv;

uniform float billboardSize;

varying float back;

void main(){
	//Usual variable passing
	texcoord = vec4(planeCoord*0.5+0.5, 0, 0);//vec4(textureCoord, 0, 0);//gl_MultiTexCoord0;
	vec4 v = billboardCoord;//vec4(gl_Vertex);
	
	//TODO : Clean this shit up	
	v+=vec4(borderShift,0);
	
	//v += modelViewMatrixInv * vec4(planeCoord,0,0);
	
	varyingVertex = v;
	varyingNormal = gl_Normal;
	
	//Compute lightmap coords
	lightMapCoords = vec2(0,1*sunIntensity);
	
	//Translate vertex
	vec4 cameraUp = modelViewMatrixInv * vec4(0, 1, 0, 1);
	vec4 cameraRight = modelViewMatrixInv * vec4(1, 0, 1, 1);
	
	modelview = modelViewMatrix * v;
	
	modelview += vec4(planeCoord*billboardSize,0,0);
	
	vec4 clochard = (projectionMatrix * modelview );
	
	gl_Position = clochard;
	
	
	
	//Eye transform
	eye = v.xyz-camPos;
	
	/* Exp fog - don't want that
	vec3 sum = -gl_Position.xyz;
	float dist = length(sum);
	const float LOG2 = 1.442695;
	float fogFactor = exp2( -gl_Fog.density *  gl_Fog.density * dist * dist * LOG2 );
	fogI = clamp(fogFactor, 0.0, 1.0);*/
}