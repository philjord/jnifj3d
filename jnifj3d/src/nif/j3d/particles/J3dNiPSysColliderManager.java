package nif.j3d.particles;

import org.jogamp.vecmath.Point3f;
import org.jogamp.vecmath.Vector3f;

import nif.j3d.NiToJ3dData;
import nif.niobject.particle.NiPSysCollider;
import nif.niobject.particle.NiPSysColliderManager;

public class J3dNiPSysColliderManager extends J3dNiPSysModifier {

	/**
	 <niobject name="NiPSysColliderManager" abstract="0" inherit="NiPSysModifier" ver1="10.2.0.0">
	
	 Particle modifier that adds a defined shape to act as a collision object for particles to interact with.
	 
	 <add name="Collider" type="Ref" template="NiPSysCollider">
	 Link to a NiPSysPlanarCollider or NiPSysSphericalCollider.
	 </add>
	 </niobject>
	 */

	private J3dNiPSysCollider j3dNiPSysCollider;

	public J3dNiPSysColliderManager(NiPSysColliderManager niPSysColliderManager, NiToJ3dData niToJ3dData) {
		super(niPSysColliderManager, niToJ3dData);
		NiPSysCollider niPSysCollider = (NiPSysCollider)niToJ3dData.get(niPSysColliderManager.collider);
		j3dNiPSysCollider = j3dNiParticleSystem.getJ3dNiPSysCollider(niPSysCollider, niToJ3dData);

		if (J3dNiParticleSystem.DEBUG_DATA && J3dNiParticleSystem.MODIFIER_DEBUG_DATA) {
			System.out.print("J3dNiPSysColliderManager");
			System.out.println(" First j3dNiPSysCollider " + j3dNiPSysCollider);

		}
	}

	//deburners
	private Point3f		loc		= new Point3f();

	private Vector3f	vel		= new Vector3f();

	private Vector3f	newVel	= new Vector3f();

	@Override
	public void updatePSys(long elapsedMillisec) {
		J3dPSysData j3dPSysData = j3dNiParticleSystem.j3dPSysData;
		float[] vs = j3dPSysData.particleVelocity;
		float[] ts = j3dPSysData.particleTranslation;

		for (int i = 0; i < j3dPSysData.activeParticleCount; i++) {
			boolean collision = false;
			loc.set(ts[i * 3 + 0], ts[i * 3 + 1], ts[i * 3 + 2]);
			vel.set(vs[i * 3 + 0], vs[i * 3 + 1], vs[i * 3 + 2]);
			newVel.set(0, 0, 0);

			collision = j3dNiPSysCollider.checkCollision(loc, vel, newVel);

			if (collision) {
				if (j3dNiPSysCollider.spawnonCollide) {
					j3dNiPSysCollider.particleDeath(i);
				}

				if (j3dNiPSysCollider.dieonCollide) {
					// note the spawn above needs the data alive so delete after telling it about it
					j3dPSysData.inactivateParticle(i);
				} else {
					//bounce off
					vs[i * 3 + 0] = newVel.x;
					vs[i * 3 + 1] = newVel.y;
					vs[i * 3 + 2] = newVel.z;
				}
			}
		}
	}
}
