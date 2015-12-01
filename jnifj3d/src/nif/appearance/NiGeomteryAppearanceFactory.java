package nif.appearance;

public class NiGeomteryAppearanceFactory
{
	private static NiGeometryAppearanceFixed niGeometryAppearanceFixed = null;

	public static NiGeomteryAppearance getNiGeometryAppearance()
	{
		if (niGeometryAppearanceFixed == null)
		{
			niGeometryAppearanceFixed = new NiGeometryAppearanceFixed();
		}
		return niGeometryAppearanceFixed;
	}

}
