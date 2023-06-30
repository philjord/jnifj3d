package nif.shader;

import java.util.ArrayList;

import nif.basic.NifRef;
import nif.j3d.NiToJ3dData;
import nif.niobject.NiObject;
import nif.niobject.NiProperty;
import nif.niobject.bs.BSLightingShaderProperty;

public class PropertyList extends ArrayList<NiObject>
{

	public PropertyList(NifRef[] properties, NiToJ3dData niToJ3dData)
	{
		for (int i = 0; i < properties.length; i++)
		{
			NiObject prop = niToJ3dData.get(properties[i]);
			if (prop != null)
			{
				add(prop);
			}
		}
	}

	public NiProperty get(Class<? extends NiProperty> type)
	{
		for (NiObject p : this)
		{
			if (type.isInstance(p))
				return (NiProperty) p;
		}
		return null;
	}

	public BSLightingShaderProperty getBSLightingShaderProperty()
	{
		for (NiObject p : this)
		{
			if (p instanceof BSLightingShaderProperty)
				return (BSLightingShaderProperty) p;
		}
		return null;
	}

}
