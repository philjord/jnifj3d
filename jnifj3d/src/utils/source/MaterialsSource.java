package utils.source;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import nif.niobject.bgsm.BSMaterial;
import nif.niobject.bgsm.BSMaterialDataBGEM;
import nif.niobject.bgsm.BSMaterialDataBGSM;
import tools.WeakValueHashMap;

/**
 * 
 *
 */
public abstract class MaterialsSource {

	// cachign is done here unlike other sources
	protected static WeakValueHashMap<String, BSMaterial>	materialFiles	= new WeakValueHashMap<String, BSMaterial>();

	// a fat global for anyone to get at
	public static MaterialsSource								bgsmSource		= null;

	//Yes it is bullshit sorry... I have to hand these guys deep, deep into the geometry code
	public static void setBgsmSource(MaterialsSource staticbgsmSource) {
		bgsmSource = staticbgsmSource;
	}

	public abstract BSMaterialDataBGEM getEffectMaterial(String fileName);

	public abstract BSMaterialDataBGSM getShaderMaterial(String fileName);

	public static BSMaterial readMaterialFile(String fileName, ByteBuffer in) throws IOException {
		if (in != null) {
			in.order(ByteOrder.LITTLE_ENDIAN);
			byte[] buf = new byte[4];
			in.get(buf);
			String headerString = new String(buf);
			if (headerString.equals("BGEM")) {
				BSMaterial m = new BSMaterialDataBGEM();
				m.readFile(in);
				return m;
			} else if (headerString.equals("BGSM")) {
				BSMaterial m = new BSMaterialDataBGSM();
				m.readFile(in);
				return m;
			} else {
				throw new IOException("BAD Material file header: " + headerString);
			}

		} else {
			System.err.println("File Not Found in Mesh Source: " + fileName);
			return null;
		}
	}

}
