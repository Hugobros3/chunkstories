
//(c) 2015-2016 XolioWare Interactive
// http://chunkstories.xyz
// http://xol.io

const float gamma = 2.2;
const float gammaInv = 0.45454545454;

vec4 texture2DGammaIn(sampler2D sampler, vec2 coords)
{
	return pow(texture2D(sampler, coords), vec4(gamma));
}

vec4 gammaOutput(vec4 inputValue)
{
	return pow(inputValue, vec4(gammaInv));
}