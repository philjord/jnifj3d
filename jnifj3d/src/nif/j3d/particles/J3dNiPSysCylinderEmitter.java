package nif.j3d.particles;

import org.jogamp.vecmath.Point3f;
import org.jogamp.vecmath.Vector3f;

import nif.j3d.NiToJ3dData;
import nif.niobject.particle.NiPSysCylinderEmitter;
import utils.convert.ConvertFromNif;

public class J3dNiPSysCylinderEmitter extends J3dNiPSysVolumeEmittter {

	float	radius;
	float	height;

	public J3dNiPSysCylinderEmitter(NiPSysCylinderEmitter niPSysCylinderEmitter, NiToJ3dData niToJ3dData) {
		super(niPSysCylinderEmitter, niToJ3dData);

		this.radius = ConvertFromNif.toJ3d(niPSysCylinderEmitter.radius);
		this.height = ConvertFromNif.toJ3d(niPSysCylinderEmitter.height);

		if (J3dNiParticleSystem.DEBUG_DATA && J3dNiParticleSystem.MODIFIER_DEBUG_DATA) {
			System.out.print("J3dNiPSysCylinderEmitter");
			System.out.print(" radius " + radius);
			System.out.println(" height " + height);
		}

	}

	@Override
	protected void getCreationPoint(Point3f pos, Vector3f vel) {
		//https://stackoverflow.com/questions/5837572/generate-a-random-point-within-a-circle-uniformly
		float r = (float)(radius * Math.sqrt(Math.random()));
		float theta = (float)(Math.random() * 2 * Math.PI);
		float x = (float)(r * Math.cos(theta));
		float y = varHalf(height);
		float z = (float)(r * Math.sin(theta));

		pos.set(x, y, z);
		getCurrentNiNodeTransform().transform(pos);
	}
}
