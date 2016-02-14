package nif.appearance;

import tools3d.utils.AppearanceFactory;

public class NiGeometryAppearanceFactoryFixed
{
	public static void setAsDefault()
	{
		AppearanceFactory.currentAppearanceFactory = new NiGeometryAppearanceFixed();
	}

}
