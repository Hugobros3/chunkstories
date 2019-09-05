#version 330
uniform sampler2D inputTexture;
 
in vec2 texCoord;
in vec2 texCoordBlur[14];

out vec4 fragColor;

void main()
{
    vec4 blurredColor = texture(inputTexture, texCoord)*0.159576912161;
    blurredColor += texture(inputTexture, texCoordBlur[0])*0.0044299121055113265;
    blurredColor += texture(inputTexture, texCoordBlur[1])*0.00895781211794;
    blurredColor += texture(inputTexture, texCoordBlur[2])*0.0215963866053;
    blurredColor += texture(inputTexture, texCoordBlur[3])*0.0443683338718;
    blurredColor += texture(inputTexture, texCoordBlur[4])*0.0776744219933;
    blurredColor += texture(inputTexture, texCoordBlur[5])*0.115876621105;
    blurredColor += texture(inputTexture, texCoordBlur[6])*0.147308056121;
    blurredColor += texture(inputTexture, texCoordBlur[7])*0.147308056121;
    blurredColor += texture(inputTexture, texCoordBlur[8])*0.115876621105;
    blurredColor += texture(inputTexture, texCoordBlur[9])*0.0776744219933;
    blurredColor += texture(inputTexture, texCoordBlur[10])*0.0443683338718;
    blurredColor += texture(inputTexture, texCoordBlur[11])*0.0215963866053;
    blurredColor += texture(inputTexture, texCoordBlur[12])*0.00895781211794;
    blurredColor += texture(inputTexture, texCoordBlur[13])*0.0044299121055113265;
	fragColor = blurredColor;
}