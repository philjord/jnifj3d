package nif.j3d;

import java.util.LinkedHashMap;

import com.frostwire.util.SparseArray;

import nif.compound.NifAVObject;
import nif.niobject.NiAVObject;
import nif.niobject.NiDefaultAVObjectPalette;

public class J3dNiDefaultAVObjectPalette
{
	private LinkedHashMap<String, J3dNiAVObject> palette = new LinkedHashMap<String, J3dNiAVObject>();

	private SparseArray<J3dNiAVObject> paletteSA = new SparseArray<J3dNiAVObject>();

	public J3dNiDefaultAVObjectPalette()
	{
	}

	public J3dNiDefaultAVObjectPalette(NiDefaultAVObjectPalette niDefaultAVObjectPalette, NiToJ3dData niToJ3dData)
	{
		for (int i = 0; i < niDefaultAVObjectPalette.numObjs; i++)
		{
			NifAVObject nifAVObject = niDefaultAVObjectPalette.objs[i];
			String name = nifAVObject.name;
			NiAVObject niAVObject = (NiAVObject) niToJ3dData.get(nifAVObject.object);
			if (niAVObject != null)
			{
				palette.put(name, niToJ3dData.get(niAVObject));

				paletteSA.put(niAVObject.refId, niToJ3dData.get(niAVObject));
			}
		}
	}

	public void put(J3dNiAVObject v)
	{
		palette.put(v.niAVObject.name, v);
		paletteSA.put(v.niAVObject.refId, v);
	}

	public J3dNiAVObject get(int refId)
	{
		return paletteSA.get(refId);
	}

	public void putAll(J3dNiDefaultAVObjectPalette other)
	{
		paletteSA.putAll(other.paletteSA);
	}

	public int[] keySet()
	{
		return paletteSA.keySet();
	}

	/**
	 * !Expensive!
	 * @param refId
	 * @return
	 */
	public J3dNiAVObject getByName(String nodeName)
	{
		return palette.get(nodeName);
	}

}
