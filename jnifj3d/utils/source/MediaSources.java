package utils.source;

public class MediaSources
{
	private MeshSource meshSource;

	private TextureSource textureSource;

	private SoundSource soundSource;

	public MediaSources(MeshSource meshSource, TextureSource textureSource, SoundSource soundSource)
	{
		this.meshSource = meshSource;
		this.textureSource = textureSource;
		this.soundSource = soundSource;
	}

	public MeshSource getMeshSource()
	{
		return meshSource;
	}

	public TextureSource getTextureSource()
	{
		return textureSource;
	}

	public SoundSource getSoundSource()
	{
		return soundSource;
	}
}
