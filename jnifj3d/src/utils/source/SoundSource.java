package utils.source;

import java.io.InputStream;
import java.util.List;

import org.jogamp.java3d.MediaContainer;

public interface SoundSource
{
	public MediaContainer getMediaContainer(String mediaName);
	
	public List<String> getFilesInFolder(String folderName);

	public InputStream getInputStream(String string);
}
