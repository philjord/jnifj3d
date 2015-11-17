package utils.source;

import java.io.InputStream;
import java.util.List;

import nif.NifFile;

public interface MeshSource
{
	public boolean nifFileExists(String nifName);

	public NifFile getNifFile(String nifName);

	/** to support other crazy file formats
	 you MUST close this yourself!
	 * 
	 * @param fileName
	 * @return
	 */
	public InputStream getInputStreamForFile(String fileName);

	/**
	 * Do NOT include the final \
	 * 
	 * @param folderName
	 * @return
	 */
	public List<String> getFilesInFolder(String folderName);
}
