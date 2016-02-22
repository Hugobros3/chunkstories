varying vec2 vertex;

attribute vec2 vertexIn;

uniform vec3 sunPos;

varying vec3 eyeDirection;

uniform mat4 projectionMatrix;
uniform mat4 projectionMatrixInv;

uniform mat4 modelViewMatrix;
uniform mat4 modelViewMatrixInv;

uniform mat4 modelViewProjectionMatrixInv;

uniform mat3 normalMatrix;
uniform mat3 normalMatrixInv;

void main()
{
    vertex = vertexIn;
	
	vec4 transformedSS = vec4(vertexIn.x, vertexIn.y, -1.0, 1.0);
	
	/*vec4 tg = vec4(0.0);
	
	tg.x += length(modelViewProjectionMatrixInv[0]);
	tg.y += length(modelViewProjectionMatrixInv[1]);
	tg.z += length(modelViewProjectionMatrixInv[2]);
	tg.a += length(modelViewProjectionMatrixInv[3]);
	*/
	eyeDirection = (modelViewProjectionMatrixInv * transformedSS ).xyz;
	
	//eyeDirection = vec4(vec3(1.0) * length(tg), 1.0);
	
    gl_Position = vec4(vertexIn.xy, 0.0, 1.0);
}