package utils.source.file;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel.MapMode;

import nif.niobject.bgsm.BSMaterial;
import nif.niobject.bgsm.BSMaterialDataBGEM;
import nif.niobject.bgsm.BSMaterialDataBGSM;
import utils.source.MaterialsSource;

public class FileMaterialsSource extends MaterialsSource {

	// we use the file mesh file searching and opening
	private static FileMeshSource fileMeshSource = new FileMeshSource();

	public FileMaterialsSource() {
	}

	@Override
	public BSMaterialDataBGEM getEffectMaterial(String fileName) {
		// e.g. materials\Landscape\Rocks\BlastedForestRocksMoss.BGSM
		// e.g. here is a bad name C:\Projects\Fallout4\Build\PC\Data\materials\Vehicles\Frame01.BGSM

		if (fileName.toLowerCase().indexOf("materials") > 0) {
			fileName = fileName.toLowerCase().substring(fileName.toLowerCase().indexOf("materials"));
		}

		BSMaterial material = materialFiles.get(fileName);

		if (material == null) {
			try {
				material = readMaterialFile(fileName, fileMeshSource.getByteBuffer(fileName));
			} catch (IOException e) {
				System.out.println("FileBgsmSource:  " + fileName + " " + e + " " + e.getStackTrace()[0]);
			}
			materialFiles.put(fileName, material);
		}

		if (!(material instanceof BSMaterialDataBGEM)) {
			// it is possible for a desired EffectMaterial to have the header string BGSM and 
			// thus cause chaos about now example FO4: Materials\SetDressing\WaterCooler\WaterCooler_Dirty.BGEM
			return null;
		}

		return (BSMaterialDataBGEM)material;
	}

	@Override
	public BSMaterialDataBGSM getShaderMaterial(String fileName) {
		// e.g. materials\Landscape\Rocks\BlastedForestRocksMoss.BGSM
		// e.g. here is a bad name C:\Projects\Fallout4\Build\PC\Data\materials\Vehicles\Frame01.BGSM

		if (fileName.toLowerCase().indexOf("materials") > 0) {
			fileName = fileName.toLowerCase().substring(fileName.toLowerCase().indexOf("materials"));
		}

		BSMaterial material = materialFiles.get(fileName);

		if (material == null) {
			try {
				material = readMaterialFile(fileName, fileMeshSource.getByteBuffer(fileName));
			} catch (IOException e) {
				System.out.println("FileBgsmSource:  " + fileName + " " + e + " " + e.getStackTrace()[0]);
			}
			materialFiles.put(fileName, material);
		}

		return (BSMaterialDataBGSM)material;
	}

	public static BSMaterial readMaterialFile(File file) throws IOException {
		RandomAccessFile nifIn = new RandomAccessFile(file, "r");

		BSMaterial m = readMaterialFile(file.getCanonicalPath(),
				nifIn.getChannel().map(MapMode.READ_ONLY, 0, file.length()));
		nifIn.close();
		return m;
	}
}
