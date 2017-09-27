#version 330
uniform samplerCube skybox;

uniform vec3 sunPos;

in vec2 vertex;
in vec3 eyeDirection;

uniform float time;

out vec4 fragColor;

void main()
{
	//For some reason, GL requires me to do this in order to have the skybox not flipped along the y axis
    fragColor = mix(vec4(vec3(0.5), 1.0), texture(skybox, normalize(vec3(-eyeDirection.x, eyeDirection.yz))), 0.8);
	
	//Note : the above is very pretty !
	//gl_FragColor = vec4(vec3(0.5) + 0.5 * eyeDirection, 1.0);
    //gl_FragColor = textureCube(skybox, normalize(eyeDirection));
}
