package nif.j3d.animation;

import java.util.ArrayList;

import javax.vecmath.Point3f;

import org.jogamp.java3d.PointLight;

import nif.j3d.J3dNiAVObject;
import nif.niobject.controller.NiLightDimmerController;

public class J3dNiLightDimmerController extends J3dNiTimeController
{
	private ArrayList<PointLight> targetPointLights = new ArrayList<PointLight>();

	public J3dNiLightDimmerController(NiLightDimmerController controller, J3dNiAVObject nodeTarget)
	{
		super(controller, nodeTarget);
		for (int i = 0; i < nodeTarget.numChildren(); i++)
		{
			if (nodeTarget.getChild(i) instanceof PointLight)
			{
				PointLight pointLight = (PointLight) nodeTarget.getChild(i);
				pointLight.setCapability(PointLight.ALLOW_ATTENUATION_WRITE);
				targetPointLights.add(pointLight);
			}
		}
	}

	@Override
	public void update(float value)
	{
		for (PointLight pointLight : targetPointLights)
		{
			pointLight.setAttenuation(value, value, value);
		}

	}

	@Override
	public void update(Point3f value)
	{
		for (PointLight pointLight : targetPointLights)
		{
			pointLight.setAttenuation(value);
		}
	}

}
