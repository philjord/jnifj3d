package nif.j3d;

import java.util.ArrayList;
import java.util.Collection;

import javax.media.j3d.TextureAttributes;

import nif.NiObjectList;
import nif.NifVer;
import nif.basic.NifPtr;
import nif.basic.NifRef;
import nif.j3d.animation.J3dNiTimeController;
import nif.j3d.animation.j3dinterp.J3dNiInterpolator;
import nif.niobject.NiAVObject;
import nif.niobject.NiObject;
import nif.niobject.controller.NiTimeController;
import nif.niobject.interpolator.NiInterpolator;
import utils.SparseArray;

public class NiToJ3dData
{
	public NifVer nifVer;

	private NiObjectList niObjects;

	private SparseArray<J3dNiAVObject> dataJ3dNiAVObject = new SparseArray<J3dNiAVObject>();

	private SparseArray<J3dNiTimeController> dataJ3dNiTimeController = new SparseArray<J3dNiTimeController>();

	private SparseArray<J3dNiInterpolator> dataJ3dNiInterpolator = new SparseArray<J3dNiInterpolator>();

	private SparseArray<TextureAttributes> textureAttributesLookup = new SparseArray<TextureAttributes>();

	/**
	 * Note by now the data in NiObjectList is totally static
	 * @param niObjectList
	 */
	public NiToJ3dData(NiObjectList niObjectList)
	{
		nifVer = niObjectList.nifVer;
		niObjects = niObjectList;
	}

	public J3dNiAVObject get(NiAVObject key)
	{
		if (key != null)
			return dataJ3dNiAVObject.get(key.refId);
		else
			return null;
	}

	public void put(NiAVObject key, J3dNiAVObject value)
	{
		if (key != null)
			dataJ3dNiAVObject.put(key.refId, value);
	}

	public Collection<J3dNiAVObject> j3dNiAVObjectValues()
	{
		ArrayList<J3dNiAVObject> ret = new ArrayList<J3dNiAVObject>();
		for (int i = 0; i < dataJ3dNiAVObject.size(); i++)
			ret.add(dataJ3dNiAVObject.get(dataJ3dNiAVObject.keyAt(i)));
		return ret;
	}

	public J3dNiTimeController get(NiTimeController key)
	{
		return dataJ3dNiTimeController.get(key.refId);
	}

	public void put(NiTimeController key, J3dNiTimeController value)
	{
		dataJ3dNiTimeController.put(key.refId, value);
	}

	public J3dNiInterpolator get(NiInterpolator key)
	{
		return dataJ3dNiInterpolator.get(key.refId);
	}

	public void put(NiInterpolator key, J3dNiInterpolator value)
	{
		dataJ3dNiInterpolator.put(key.refId, value);
	}

	public TextureAttributes getTextureAttributes(int refId)
	{
		return textureAttributesLookup.get(refId);
	}

	public void putTextureAttributes(int refId, TextureAttributes value)
	{
		textureAttributesLookup.put(refId, value);
	}

	public NiObject root()
	{
		return niObjects.root();
	}

	public J3dNiAVObject getJ3dRoot()
	{
		return dataJ3dNiAVObject.get(root().refId);
	}

	public NiObject get(NifRef nr)
	{
		return niObjects.get(nr);
	}

	public NiObject get(NifPtr np)
	{
		return niObjects.get(np);

	}

	public int length()
	{
		return niObjects.length();
	}

	public NiObjectList getNiObjects()
	{
		return niObjects;
	}

	public J3dNiAVObject get(String nodeName)
	{
		for (NiObject no : niObjects.getNiObjects())
		{
			if (no instanceof NiAVObject && ((NiAVObject) no).name.equals(nodeName))
				return get((NiAVObject) no);
		}

		return null;
	}

}
