uniform sampler2D diffuseTexture;
 
varying vec2 texCoord;
 
void main()
{
	gl_FragColor = texture2D(diffuseTexture, texCoord);
}