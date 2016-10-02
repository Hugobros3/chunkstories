#version 130

uniform sampler2D diffuseTexture;
 
in vec2 texCoord;
in vec2 normalizedVertexIn;
 
void main()
{
	gl_FragColor = mix(texture2D(diffuseTexture, texCoord), vec4(0.0, 0.0, 0.0, 1.0), length(normalizedVertexIn) * 0.125);
	//gl_FragColor = vec4(1.0, 0.0, 1.0, 1.0);
}