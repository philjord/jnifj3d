package utils.source;

import java.util.List;

import nif.NifFile;

public interface MeshSource
{
	public boolean nifFileExists(String nifName);

	public NifFile getNifFile(String nifName);

	public List<String> getFilesInFolder(String folderName);
}
