package nif.j3d;

import javax.vecmath.Color3f;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3f;

import org.jogamp.java3d.BoundingSphere;
import org.jogamp.java3d.DirectionalLight;

import nif.niobject.NiDirectionalLight;

public class J3dNiDirectionalLight extends J3dNiLight
{
	public static BoundingSphere defaultBoundingShpere = new BoundingSphere(new Point3d(0.0, 0.0, 0.0), 100d);

	public J3dNiDirectionalLight(NiDirectionalLight niDirectionalLight, NiToJ3dData niToJ3dData)
	{
		super(niDirectionalLight, niToJ3dData);
		niToJ3dData.put(niDirectionalLight, this);

		DirectionalLight dLight = new DirectionalLight(true, new Color3f(niDirectionalLight.diffuseColor.r,
				niDirectionalLight.diffuseColor.g, niDirectionalLight.diffuseColor.b), new Vector3f(0, -1, 0));
		dLight.setInfluencingBounds(defaultBoundingShpere);
		addChild(dLight);

	}

}
