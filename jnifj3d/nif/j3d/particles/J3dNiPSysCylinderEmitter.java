package nif.j3d.particles;

import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import nif.j3d.NiToJ3dData;
import nif.niobject.particle.NiPSysCylinderEmitter;
import utils.convert.ConvertFromNif;

public class J3dNiPSysCylinderEmitter extends J3dNiPSysEmitter
{

	private NiPSysCylinderEmitter niPSysCylinderEmitter;

	public J3dNiPSysCylinderEmitter(NiPSysCylinderEmitter niPSysCylinderEmitter, NiToJ3dData niToJ3dData)
	{
		super(niPSysCylinderEmitter, niToJ3dData);
		this.niPSysCylinderEmitter = niPSysCylinderEmitter;
	}

	//deburner
	private Vector3f v = new Vector3f();

	@Override
	protected void getCreationPoint(Point3f pos)
	{

		float x = 0;
		float y = var(niPSysCylinderEmitter.height);
		float z = 0;
		boolean isInRadius = false;

		while (!isInRadius)
		{
			x = var(niPSysCylinderEmitter.radius);
			z = var(niPSysCylinderEmitter.radius);
			v.set(x, 0, z);
			isInRadius = v.length() <= niPSysCylinderEmitter.radius;
		}
		x = ConvertFromNif.toJ3d(x);
		y = ConvertFromNif.toJ3d(y);
		z = ConvertFromNif.toJ3d(z);
		pos.set(x, y, z);
	}
}
