package utils.source;

import java.io.InputStream;
import java.nio.ByteBuffer;

import nif.NifFile;

public abstract class MeshSource {

	// a fat global for anyone to get at
	public static MeshSource meshSource = null;

	//Yes it is bullshit sorry... I have to hand these guys deep, deep into the ninode code
	public static void setMeshSource(MeshSource staticmeshSource) {
		meshSource = staticmeshSource;
	}

	public abstract boolean nifFileExists(String nifName);

	public abstract NifFile getNifFile(String nifName);

	/**
	 * to support other crazy file formats you MUST close this yourself!
	 * 
	 * @param fileName
	 * @return
	 */
	public abstract InputStream getInputStreamForFile(String fileName);

	public abstract ByteBuffer getByteBuffer(String fileName);

}
