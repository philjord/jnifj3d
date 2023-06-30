package nif.appearance;

import org.jogamp.java3d.Appearance;
import org.jogamp.java3d.Shape3D;

import nif.j3d.J3dNiAVObject;
import nif.j3d.NiToJ3dData;
import nif.niobject.NiGeometry;
import nif.shader.NiGeometryAppearanceShader;
import tools3d.utils.AppearanceFactory;
import utils.source.TextureSource;

public class NiGeometryAppearanceFactoryShader implements NiGeometryAppearance
{
	public static void setAsDefault()
	{
		AppearanceFactory.currentAppearanceFactory = new NiGeometryAppearanceFactoryShader();
	}

	@Override
	public Appearance configureAppearance(NiGeometry niGeometry, NiToJ3dData niToJ3dData, TextureSource textureSource, Shape3D shape,
			J3dNiAVObject target)
	{
		NiGeometryAppearanceShader niGeometryAppearanceShader = new NiGeometryAppearanceShader(niGeometry, niToJ3dData, textureSource,
				shape, target);
		String progName = niGeometryAppearanceShader.setupShaderProgram();
		if (progName != null)
		{
			return niGeometryAppearanceShader.getAppearance();
		}
		else
		{
			if (NiGeometryAppearanceShader.OUTPUT_BINDINGS)
				System.out.println("using FFP");
			return new NiGeometryAppearanceFixed().configureAppearance(niGeometry, niToJ3dData, textureSource, shape, target);
		}
	}

}
