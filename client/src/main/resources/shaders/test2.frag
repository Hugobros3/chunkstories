#version 400

layout(std140) uniform WorldInformation
{
    vec4 grassColor;
    vec4 cloudsColor;
    int albedoTexture;
} worldInformation;

layout(std140) uniform LightingInformation
{
    vec4 sunlightColor;
} lightingInformation;

uniform sampler2D virtualTextures[64];

layout(location = 0) out vec4 fragColor;
in vec2 texCoord;

void main()
{
    fragColor = worldInformation.cloudsColor + (lightingInformation.sunlightColor * texture(virtualTextures[worldInformation.albedoTexture], texCoord));
}