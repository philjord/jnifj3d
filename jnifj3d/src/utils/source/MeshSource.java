package utils.source;

import java.util.List;

import nif.NifFile;

public interface MeshSource
{
	public boolean nifFileExists(String nifName);

	public NifFile getNifFile(String nifName);

	/**
	 * Do NOT include the final \
	 * @param folderName
	 * @return
	 */
	public List<String> getFilesInFolder(String folderName);
}
