uniform samplerCube skybox;

uniform vec3 sunPos;

varying vec2 vertex;
varying vec4 texcoords;

uniform float time;

varying vec3 eyeDirection;

void main()
{
	//For some reason, GL requires me to do this in order to have the skybox not flipped along the y axis
    gl_FragColor = mix(vec4(vec3(0.5), 1.0), textureCube(skybox, normalize(vec3(-eyeDirection.x, eyeDirection.yz))), 0.8);
    //gl_FragColor = textureCube(skybox, normalize(eyeDirection));
}