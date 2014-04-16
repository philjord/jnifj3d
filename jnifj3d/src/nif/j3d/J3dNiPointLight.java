package nif.j3d;

import javax.media.j3d.BoundingSphere;
import javax.media.j3d.PointLight;
import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Point3f;

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
