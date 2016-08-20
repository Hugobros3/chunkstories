uniform sampler2D diffuseTexture;
 
varying vec2 texCoord;
 
void main()
{
	gl_FragColor = texture2D(diffuseTexture, texCoord);
	//gl_FragColor = vec4(1.0, 0.0, 1.0, 1.0);
}