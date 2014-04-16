package nif.j3d;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Set;

import nif.compound.NifAVObject;
import nif.niobject.NiAVObject;
import nif.niobject.NiDefaultAVObjectPalette;

public class J3dNiDefaultAVObjectPalette
{
	private LinkedHashMap<String, J3dNiAVObject> palette = new LinkedHashMap<String, J3dNiAVObject>();

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
			}
		}
	}

	public void put(String key, J3dNiAVObject v)
	{
		palette.put(key, v);
	}

	public J3dNiAVObject get(String key)
	{
		return palette.get(key);
	}

	public J3dNiAVObject get(NiAVObject key)
	{
		return get(key.name);
	}

	public void put(NiAVObject niAVObject, J3dNiAVObject j3dNiAVObject)
	{
		put(niAVObject.name, j3dNiAVObject);
	}

	public void putAll(J3dNiDefaultAVObjectPalette other)
	{
		palette.putAll(other.palette);
	}

	public Collection<J3dNiAVObject> values()
	{
		return palette.values();
	}

	public Set<String> keySet()
	{
		return palette.keySet();
	}
}
