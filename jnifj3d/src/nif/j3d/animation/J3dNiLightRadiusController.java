package nif.j3d.animation;

import java.util.ArrayList;

import javax.vecmath.Point3d;
import javax.vecmath.Point3f;

import org.jogamp.java3d.BoundingSphere;
import org.jogamp.java3d.PointLight;

import nif.j3d.J3dNiAVObject;
import nif.niobject.controller.NiLightRadiusController;

public class J3dNiLightRadiusController extends J3dNiTimeController
{
	private ArrayList<PointLight> targetPointLights = new ArrayList<PointLight>();

	public J3dNiLightRadiusController(NiLightRadiusController controller, J3dNiAVObject nodeTarget)
	{
		super(controller, nodeTarget);
		for (int i = 0; i < nodeTarget.numChildren(); i++)
		{
			if (nodeTarget.getChild(i) instanceof PointLight)
			{
				PointLight pointLight = (PointLight) nodeTarget.getChild(i);
				pointLight.setCapability(PointLight.ALLOW_INFLUENCING_BOUNDS_WRITE);
				targetPointLights.add(pointLight);
			}
		}
	}

	@Override
	public void update(float value)
	{
		for (PointLight pointLight : targetPointLights)
		{
			pointLight.setInfluencingBounds(new BoundingSphere(new Point3d(), value));
		}

	}

	@Override
	public void update(Point3f value)
	{
		for (PointLight pointLight : targetPointLights)
		{
			//TODO: why this call?
			pointLight.setInfluencingBounds(new BoundingSphere(new Point3d(), value.x));
		}
	}

}
