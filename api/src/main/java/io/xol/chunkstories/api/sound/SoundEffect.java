package io.xol.chunkstories.api.sound;

import java.util.HashMap;

//(c) 2015-2017 XolioWare Interactive
//http://chunkstories.xyz
//http://xol.io

public class SoundEffect
{
	/**
	 * This class is basically a fancy wrapper for the OpenAL EFX Extension. Supported effects ( and by supported it means supported by the game engine, nothing more nothing less )
	 * are listed in {@link SoundEffectType}
	 * @param soundEffectType
	 */
	public SoundEffect(SoundEffectType soundEffectType)
	{
		this.soundEffectType = soundEffectType;
	}
	
	SoundEffectType soundEffectType;
	
	HashMap<Integer, Integer> intParameters = new HashMap<Integer, Integer>();
	HashMap<Integer, Float> floatParameters = new HashMap<Integer, Float>();
	HashMap<Integer, float[]> floatvParameters = new HashMap<Integer, float[]>();
	
	/**
	 * Aka alEffecti() - But you have to use this because we don't thrust you and you are not in a gl context. Sorry for the dirtyness, it's not worse than most libraries I found really.
	 * These 3 methods allow you to tell the game engine to use certain parameters for certain values, check out the OpenAL EFX reference for the explanations
	 * ( There's one copy hosted over http://zhang.su/seal/EffectsExtensionGuide.pdf )
	 * @param parameter See http://legacy.lwjgl.org/javadoc/constant-values.html for a list and the aformentioned reference for explanations
	 * @param value 
	 * @return
	 */
	public SoundEffect addSoundEffectParameteri(int parameter, int value)
	{
		if(intParameters.containsKey(parameter))
			intParameters.remove(parameter);
		intParameters.put(parameter, value);
		return this;
	}
	
	/**
	 * Aka alEffect() - But you have to use this because we don't thrust you and you are not in a gl context. Sorry for the dirtyness, it's not worse than most libraries I found really.
	 * These 3 methods allow you to tell the game engine to use certain parameters for certain values, check out the OpenAL EFX reference for the explanations
	 * ( There's one copy hosted over http://zhang.su/seal/EffectsExtensionGuide.pdf )
	 * @param parameter See http://legacy.lwjgl.org/javadoc/constant-values.html for a list and the aformentioned reference for explanations
	 * @param value 
	 * @return
	 */
	public SoundEffect addSoundEffectParameterf(int parameter, float value)
	{
		if(floatParameters.containsKey(parameter))
			floatParameters.remove(parameter);
		floatParameters.put(parameter, value);
		return this;
	}
	
	/**
	 * Aka alEffectfv() - But you have to use this because we don't thrust you and you are not in a gl context. Sorry for the dirtyness, it's not worse than most libraries I found really.
	 * These 3 methods allow you to tell the game engine to use certain parameters for certain values, check out the OpenAL EFX reference for the explanations
	 * ( There's one copy hosted over http://zhang.su/seal/EffectsExtensionGuide.pdf )
	 * @param parameter See http://legacy.lwjgl.org/javadoc/constant-values.html for a list and the aformentioned reference for explanations
	 * @param value A float[] of 3 COMPONENTS. Anything else will fail silently.
	 * @return
	 */
	public SoundEffect addSoundEffectParameter3f(int parameter, float values[])
	{
		if(values.length != 3)
			return this; // Fails silently because i secretly hate you
		if(floatvParameters.containsKey(parameter))
			floatvParameters.remove(parameter);
		floatvParameters.put(parameter, values);
		return this;
	}
}
