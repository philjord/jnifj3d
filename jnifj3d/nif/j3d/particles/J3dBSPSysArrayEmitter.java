package nif.j3d.particles;

import javax.vecmath.Point3f;

import utils.convert.ConvertFromNif;
import nif.j3d.NiToJ3dData;
import nif.niobject.bs.BSPSysArrayEmitter;

public class J3dBSPSysArrayEmitter extends J3dNiPSysEmitter
{
	private BSPSysArrayEmitter bSPSysArrayEmitter;

	public J3dBSPSysArrayEmitter(BSPSysArrayEmitter bSPSysArrayEmitter, NiToJ3dData niToJ3dData)
	{
		super(bSPSysArrayEmitter, niToJ3dData);
		this.bSPSysArrayEmitter = bSPSysArrayEmitter;
	}

	@Override
	protected void getCreationPoint(Point3f pos)
	{
		//TODO:  lord knows what sort of emitter this is?
		// Particle emitter that uses a node, its children and subchildren to emit from. 
		// Emission will be evenly spread along points from nodes leading to their direct parents/children only.
		//bSPSysArrayEmitter.emitterObject
		//System.out.println("J3dBSPSysArrayEmitter emitted a particle");

		float x = var(1f);
		x = ConvertFromNif.toJ3d(x);
		float y = var(1f);
		y = ConvertFromNif.toJ3d(y);
		float z = var(1f);
		z = ConvertFromNif.toJ3d(z);

		pos.set(x, y, z);
	}

}
