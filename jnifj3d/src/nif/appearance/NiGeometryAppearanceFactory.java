package nif.appearance;

import java.lang.reflect.Constructor;

public class NiGeometryAppearanceFactory
{
	private static NiGeometryAppearanceFixed niGeometryAppearanceFixed = null;
	private static boolean overrideChecked = false;
	private static NiGeometryAppearance niGeomteryAppearanceFactoryOverride = null;

	public static NiGeometryAppearance getNiGeometryAppearance()
	{
		if (!overrideChecked)
		{
			try
			{
				Class<?> newClass = Class.forName("nif.appearance.NiGeometryAppearanceFactoryOverride");
				Object[] noArgs = new Object[] {};
				Constructor<?> cons = newClass.getConstructors()[0];
				Object obj = cons.newInstance(noArgs);
				niGeomteryAppearanceFactoryOverride = (NiGeometryAppearance) obj;
			}
			catch (Exception e)
			{
				// no extensions exist
			}
			overrideChecked = true;
		}

		if (niGeomteryAppearanceFactoryOverride != null)
		{
			return niGeomteryAppearanceFactoryOverride;
		}
		else if (niGeometryAppearanceFixed == null)
		{
			niGeometryAppearanceFixed = new NiGeometryAppearanceFixed();
		}
		return niGeometryAppearanceFixed;
	}

}
