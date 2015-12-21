uniform sampler2D inputTexture;
 
varying vec2 texCoord;
 
void main()
{
	gl_FragColor = texture2D(inputTexture, texCoord);
}