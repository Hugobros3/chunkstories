#version 130

uniform sampler2D diffuseTexture;
uniform sampler2D backgroundTexture;
 
in vec2 texCoord;
in vec2 ditherCoord;
in vec2 normalizedVertexIn;
 
void main()
{
	vec4 colour = texture2D(backgroundTexture, texCoord);
	
	colour = mix(colour, vec4(1.0), 0.5);
	
	vec4 dither = mix(texture2D(diffuseTexture, ditherCoord), vec4(0.0, 0.0, 0.0, 1.0), length(normalizedVertexIn) * 0.125);
	
	colour.rgb *= mix(vec3(1.0), dither.rgb, 0.75);

	gl_FragColor = colour;
	//gl_FragColor = vec4(1.0, 0.0, 1.0, 1.0);
}