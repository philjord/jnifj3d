package nif.j3d.particles;

import org.jogamp.vecmath.Point3f;
import org.jogamp.vecmath.Vector3f;

import nif.j3d.NiToJ3dData;
import nif.niobject.NiObject;
import nif.niobject.particle.NiPSysCollider;
import nif.niobject.particle.NiPSysColliderManager;
import nif.niobject.particle.NiPSysSpawnModifier;

public class J3dNiPSysColliderManager extends J3dNiPSysModifier
{
	private NiPSysCollider niPSysCollider;

	private J3dNiPSysSpawnModifier j3dNiPSysSpawnModifier;

	private J3dNiPSysCollider j3dNiPSysCollider;

	public J3dNiPSysColliderManager(NiPSysColliderManager niPSysColliderManager, NiToJ3dData niToJ3dData)
	{
		super(niPSysColliderManager, niToJ3dData);
		niPSysCollider = (NiPSysCollider) niToJ3dData.get(niPSysColliderManager.collider);
		j3dNiPSysCollider = J3dNiPSysCollider.createJ3dNiPSysCollider(niPSysCollider, niToJ3dData);

		//System.out.println("J3dNiPSysColliderManager " + niPSysCollider);

		NiObject niObject = niToJ3dData.get(niPSysCollider.spawnModifier);
		if (niObject instanceof NiPSysSpawnModifier)
		{
			j3dNiPSysSpawnModifier = (J3dNiPSysSpawnModifier) j3dNiParticleSystem.getJ3dNiPSysModifier((NiPSysSpawnModifier) niObject,
					niToJ3dData);
		}

	}

	//deburners
	private Point3f loc = new Point3f();

	private Vector3f vel = new Vector3f();

	private Vector3f newVel = new Vector3f();

	@Override
	public void updatePSys(long elapsedMillisec)
	{
		J3dPSysData j3dPSysData = j3dNiParticleSystem.j3dPSysData;
		float[] vs = j3dPSysData.particleVelocity;
		float[] ts = j3dPSysData.particleTranslation;

		for (int i = 0; i < j3dPSysData.activeParticleCount; i++)
		{
			boolean collision = false;
			loc.set(ts[i * 3 + 0], ts[i * 3 + 1], ts[i * 3 + 2]);
			vel.set(vs[i * 3 + 0], vs[i * 3 + 1], vs[i * 3 + 2]);
			newVel.set(0, 0, 0);

			collision = j3dNiPSysCollider.checkCollision(loc, vel, newVel);

			if (collision)
			{

				if (niPSysCollider.spawnonCollide)
				{
					j3dNiPSysSpawnModifier.particleDeath(i);
				}

				if (niPSysCollider.dieonCollide)
				{
					// note the spawn above needs the data alive so delete after telling it about it
					j3dPSysData.inactivateParticle(i);
				}
				else
				{
					//bounce off
					vs[i * 3 + 0] = newVel.x * niPSysCollider.bounce;
					vs[i * 3 + 1] = newVel.y * niPSysCollider.bounce;
					vs[i * 3 + 2] = newVel.z * niPSysCollider.bounce;
				}
			}
		}
	}
}
