package nif.j3d.particles;

import org.jogamp.vecmath.Point3f;
import org.jogamp.vecmath.Vector3f;

import nif.j3d.NiToJ3dData;
import nif.niobject.particle.NiPSysSphereEmitter;
import utils.convert.ConvertFromNif;

public class J3dNiPSysSphereEmitter extends J3dNiPSysVolumeEmittter {
	private float radius;

	public J3dNiPSysSphereEmitter(NiPSysSphereEmitter niPSysSphereEmitter, NiToJ3dData niToJ3dData) {
		super(niPSysSphereEmitter, niToJ3dData);
		this.radius = ConvertFromNif.toJ3d(niPSysSphereEmitter.radius);

		if (J3dNiParticleSystem.DEBUG_DATA && J3dNiParticleSystem.MODIFIER_DEBUG_DATA) {
			System.out.print("J3dNiPSysSphereEmitter");
			System.out.println(" radius " + niPSysSphereEmitter.radius);
		}
	}

	@Override
	protected void getCreationPoint(Point3f pos, Vector3f vel) {
		
		//https://stackoverflow.com/questions/5531827/random-point-on-a-given-sphere
		float r = (float)(radius * Math.sqrt(Math.random()));
		float theta = (float)(Math.random() * 2 * Math.PI);
		float phi = (float)Math.acos((2 * Math.random()) - 1);
		float x = (float)(r * Math.sin(phi) * Math.cos(theta));
		float y = (float)(r * Math.sin(phi) * Math.sin(theta));
		float z = (float)(r * Math.cos(phi));

		pos.set(x, y, z);
		
		getCurrentNiNodeTransform().transform(pos);
		getCurrentNiNodeTransform().transform(vel);
	}
}
