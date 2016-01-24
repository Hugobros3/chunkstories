uniform sampler2D inputTexture;
 
varying vec2 texCoord;
varying vec2 texCoordBlur[14];
 
void main()
{
    vec4 blurredColor = texture2D(inputTexture, texCoord)*0.159576912161;
    blurredColor += texture2D(inputTexture, texCoordBlur[0])*0.0044299121055113265;
    blurredColor += texture2D(inputTexture, texCoordBlur[1])*0.00895781211794;
    blurredColor += texture2D(inputTexture, texCoordBlur[2])*0.0215963866053;
    blurredColor += texture2D(inputTexture, texCoordBlur[3])*0.0443683338718;
    blurredColor += texture2D(inputTexture, texCoordBlur[4])*0.0776744219933;
    blurredColor += texture2D(inputTexture, texCoordBlur[5])*0.115876621105;
    blurredColor += texture2D(inputTexture, texCoordBlur[6])*0.147308056121;
    blurredColor += texture2D(inputTexture, texCoordBlur[7])*0.147308056121;
    blurredColor += texture2D(inputTexture, texCoordBlur[8])*0.115876621105;
    blurredColor += texture2D(inputTexture, texCoordBlur[9])*0.0776744219933;
    blurredColor += texture2D(inputTexture, texCoordBlur[10])*0.0443683338718;
    blurredColor += texture2D(inputTexture, texCoordBlur[11])*0.0215963866053;
    blurredColor += texture2D(inputTexture, texCoordBlur[12])*0.00895781211794;
    blurredColor += texture2D(inputTexture, texCoordBlur[13])*0.0044299121055113265;
	gl_FragColor = blurredColor;
}