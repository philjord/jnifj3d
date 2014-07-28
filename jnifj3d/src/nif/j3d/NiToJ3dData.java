package nif.j3d;

import java.util.Collection;
import java.util.LinkedHashMap;

import nif.NiObjectList;
import nif.NifVer;
import nif.basic.NifPtr;
import nif.basic.NifRef;
import nif.j3d.animation.J3dNiTimeController;
import nif.j3d.animation.interp.J3dNiInterpolator;
import nif.niobject.NiAVObject;
import nif.niobject.NiObject;
import nif.niobject.controller.NiTimeController;
import nif.niobject.interpolator.NiInterpolator;

public class NiToJ3dData
{
	public NifVer nifVer;

	private NiObjectList niObjects;

	private LinkedHashMap<NiAVObject, J3dNiAVObject> dataJ3dNiAVObject = new LinkedHashMap<NiAVObject, J3dNiAVObject>();

	private LinkedHashMap<NiTimeController, J3dNiTimeController> dataJ3dNiTimeController = new LinkedHashMap<NiTimeController, J3dNiTimeController>();

	private LinkedHashMap<NiInterpolator, J3dNiInterpolator> dataJ3dNiInterpolator = new LinkedHashMap<NiInterpolator, J3dNiInterpolator>();

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
		return dataJ3dNiAVObject.get(key);
	}

	public void put(NiAVObject key, J3dNiAVObject value)
	{
		dataJ3dNiAVObject.put(key, value);
	}

	public Collection<J3dNiAVObject> j3dNiAVObjectValues()
	{
		return dataJ3dNiAVObject.values();
	}

	public J3dNiTimeController get(NiTimeController key)
	{
		return dataJ3dNiTimeController.get(key);
	}

	public void put(NiTimeController key, J3dNiTimeController value)
	{
		dataJ3dNiTimeController.put(key, value);
	}

	public J3dNiInterpolator get(NiInterpolator key)
	{
		return dataJ3dNiInterpolator.get(key);
	}

	public void put(NiInterpolator key, J3dNiInterpolator value)
	{
		dataJ3dNiInterpolator.put(key, value);
	}

	public NiObject root()
	{
		return niObjects.root();
	}

	public J3dNiAVObject getJ3dRoot()
	{
		return dataJ3dNiAVObject.get(root());
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
