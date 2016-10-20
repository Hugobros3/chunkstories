#version 150

uniform sampler2D diffuseTexture;
uniform sampler2D backgroundTexture;
 
in vec2 texCoord;
in vec2 ditherCoord;
in vec2 normalizedVertexIn;

out vec4 fragColor;
 
void main()
{
	vec4 colour = texture(backgroundTexture, texCoord);
	
	colour = mix(colour, vec4(1.0), 0.5);
	
	vec4 dither = mix(texture(diffuseTexture, ditherCoord), vec4(0.0, 0.0, 0.0, 1.0), length(normalizedVertexIn) * 0.125);
	
	colour.rgb *= mix(vec3(1.0), dither.rgb, 0.75);

	fragColor = colour;
	//gl_FragColor = vec4(1.0, 0.0, 1.0, 1.0);
}