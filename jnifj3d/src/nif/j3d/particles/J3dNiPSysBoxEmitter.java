package nif.j3d.particles;

import javax.vecmath.Point3f;

import nif.j3d.NiToJ3dData;
import nif.niobject.particle.NiPSysBoxEmitter;
import utils.convert.ConvertFromNif;

public class J3dNiPSysBoxEmitter extends J3dNiPSysEmitter
{
	private NiPSysBoxEmitter niPSysBoxEmitter;

	public J3dNiPSysBoxEmitter(NiPSysBoxEmitter niPSysBoxEmitter, NiToJ3dData niToJ3dData)
	{
		super(niPSysBoxEmitter, niToJ3dData);
		this.niPSysBoxEmitter = niPSysBoxEmitter;
	}

	@Override
	protected void getCreationPoint(Point3f pos)
	{
		float x = var(niPSysBoxEmitter.width);
		x = ConvertFromNif.toJ3d(x);
		float y = var(niPSysBoxEmitter.height);
		y = ConvertFromNif.toJ3d(y);
		float z = var(niPSysBoxEmitter.depth);
		z = ConvertFromNif.toJ3d(z);

		pos.set(x, y, z);
	}
}
