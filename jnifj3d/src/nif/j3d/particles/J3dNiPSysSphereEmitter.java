package nif.j3d.particles;

import org.jogamp.vecmath.Point3f;
import org.jogamp.vecmath.Vector3f;

import nif.j3d.NiToJ3dData;
import nif.niobject.particle.NiPSysSphereEmitter;
import utils.convert.ConvertFromNif;

public class J3dNiPSysSphereEmitter extends J3dNiPSysEmitter
{
	private NiPSysSphereEmitter niPSysSphereEmitter;

	public J3dNiPSysSphereEmitter(NiPSysSphereEmitter niPSysSphereEmitter, NiToJ3dData niToJ3dData)
	{
		super(niPSysSphereEmitter, niToJ3dData);
		this.niPSysSphereEmitter = niPSysSphereEmitter;
	}

	//deburner
	private Vector3f v = new Vector3f();

	@Override
	protected void getCreationPoint(Point3f pos)
	{
		//The reject method below is fine it's equals 1.9 time s teh effort of this better algorithm
		// for unit shpere
		// choose z as random [-1,1]
		// choose t as random [0, 2PI]
		// r = root(1-z^2)
		// x = r*cos(t)
		// y = r*sin(t)

		float x = 0;
		float y = 0;
		float z = 0;
		boolean isInRadius = false;

		while (!isInRadius)
		{
			x = var(niPSysSphereEmitter.radius);
			y = var(niPSysSphereEmitter.radius);
			z = var(niPSysSphereEmitter.radius);
			v.set(x, y, z);
			isInRadius = v.length() <= niPSysSphereEmitter.radius;
		}
		x = ConvertFromNif.toJ3d(x);
		y = ConvertFromNif.toJ3d(y);
		z = ConvertFromNif.toJ3d(z);
		pos.set(x, y, z);
	}
}
