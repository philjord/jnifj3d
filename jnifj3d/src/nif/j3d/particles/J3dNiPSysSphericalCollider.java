package nif.j3d.particles;

import utils.convert.ConvertFromNif;

import org.jogamp.vecmath.Point3f;
import org.jogamp.vecmath.Vector3f;

import nif.j3d.NiToJ3dData;
import nif.niobject.particle.NiPSysSphericalCollider;

public class J3dNiPSysSphericalCollider extends J3dNiPSysCollider {
	private float radius;

	public J3dNiPSysSphericalCollider(	NiPSysSphericalCollider niPSysSphericalCollider, NiToJ3dData niToJ3dData,
										J3dNiParticleSystem j3dNiParticleSystem) {
		super(niPSysSphericalCollider, niToJ3dData, j3dNiParticleSystem);
		radius = ConvertFromNif.toJ3d(niPSysSphericalCollider.radius);

		if (J3dNiParticleSystem.DEBUG_DATA && J3dNiParticleSystem.MODIFIER_DEBUG_DATA) {
			System.out.print("J3dNiPSysSphericalCollider");
			System.out.println(" radius " + radius);
		}
	}

	@Override
	protected boolean checkCollision(Point3f loc, Vector3f vel, Vector3f newVel) {
		
		//FIXME: bounce direction not right and +vel not really good
		// example *********** NEW file Meshes\Furniture\Clutter\BlacksmithSharpeningWheelAnimating.nif
		//J3dNiPSysCollider abstract parent of the next one
		 //bounce 0.5 spawnonCollide false dieonCollide false spawnModifier [J3dNiPSysSpawnModifier]  parent null nextCollider null
		 //J3dNiPSysSphericalCollider radius 0.054447804
		
		collLoc.set(0, 0, 0);
		getCurrentNiNodeTransform().transform(collLoc);

		float distFromColl = collLoc.distance(loc);
		//System.out.println("testing part " + loc + " " + vel);
		//System.out.println("testing collLoc " + collLoc);
		//System.out.println("testing distFromColl " + distFromColl + " vs "+radius);
		if (distFromColl <= radius) {

			//System.out.println("J3dNiPSysSphericalCollider collidey ");
			//System.out.println("testing part " + loc + " " + vel);
			//System.out.println("testing collLoc " + collLoc);
			//System.out.println("testing distFromColl " + distFromColl + " vs "+radius);

			//if I've hit or about to hit, then set the bounce to the "straight away form radius" not the better incident angle		
			newVel.x = -vel.x * bounce;
			newVel.y = -vel.y * bounce;
			newVel.z = -vel.z * bounce;

			return true;
		}

		// add the velocity in to check that
		velApplied.add(loc, vel);
		distFromColl = collLoc.distance(velApplied);
		if (distFromColl <= radius) {
			newVel.x = -vel.x * bounce;
			newVel.y = -vel.y * bounce;
			newVel.z = -vel.z * bounce;
			//			System.out.println("J3dNiPSysSphericalCollider collidey ");
			return true;
		}

		if (nextCollider != null) {
			return nextCollider.checkCollision(loc, vel, newVel);
		} else {
			return false;
		}
	}
}
