package io.xol.chunkstories.api.sound;

public enum SoundEffectType
{
	NullEffect(0x0000),
	ReverbEffect(0x0001),
	ChorusEffect(0x0002),
	DistorsionEffect(0x0003),
	EchoEffect(0x004),
	FlangerEffect(0x005),
	FrequentyShiterEffect(0x006),
	VocalMorpherEffect(0x007),
	PitchShifterEffect(0x008),
	RingModulatorEffect(0x009),
	AutowahEffect(0x00A),
	CompressorEffect(0x00B),
	EqualizerEffect(0x00C);
	//EaxReverbEffect(0x8000); // We'll skip that one
	
	public int id;

	SoundEffectType(int id)
	{
		this.id = id;
	}
}
