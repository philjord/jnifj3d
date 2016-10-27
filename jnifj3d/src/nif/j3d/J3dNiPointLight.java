package nif.j3d;

import org.jogamp.java3d.BoundingSphere;
import org.jogamp.java3d.PointLight;
import org.jogamp.vecmath.Color3f;
import org.jogamp.vecmath.Point3d;
import org.jogamp.vecmath.Point3f;

import nif.niobject.NiPointLight;

public class J3dNiPointLight extends J3dNiLight
{
	public static BoundingSphere defaultBoundingShpere = new BoundingSphere(new Point3d(0.0, 0.0, 0.0), 100d);

	public J3dNiPointLight(NiPointLight niPointLight, NiToJ3dData niToJ3dData)
	{
		super(niPointLight, niToJ3dData);
		niToJ3dData.put(niPointLight, this);

		PointLight pointLight = new PointLight(true, new Color3f(niPointLight.diffuseColor.r, niPointLight.diffuseColor.g,
				niPointLight.diffuseColor.b), new Point3f(0, 0, 0), new Point3f(niPointLight.constantAttenuation, niPointLight.linearAttenuation,
				niPointLight.quadraticAttenuation));
		pointLight.setInfluencingBounds(defaultBoundingShpere);
		addChild(pointLight);

		//TODO: possibly I should set the affected nodes list (scope)

	}

}
