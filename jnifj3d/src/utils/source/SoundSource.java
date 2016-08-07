package utils.source;

import java.io.InputStream;
import java.util.List;

import javax.media.j3d.MediaContainer;

public interface SoundSource
{
	public MediaContainer getMediaContainer(String mediaName);
	
	public List<String> getFilesInFolder(String folderName);

	public InputStream getInputStream(String string);
}
