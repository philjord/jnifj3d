package nif.j3d.particles;

import org.jogamp.vecmath.Color4f;
import org.jogamp.vecmath.Point3f;
import org.jogamp.vecmath.Vector3f;

import nif.j3d.NiToJ3dData;
import nif.niobject.particle.NiPSysSpawnModifier;

public class J3dNiPSysSpawnModifier extends J3dNiPSysModifier
{
	private NiPSysSpawnModifier niPSysSpawnModifier;

	public J3dNiPSysSpawnModifier(NiPSysSpawnModifier niPSysSpawnModifier, NiToJ3dData niToJ3dData)
	{
		super(niPSysSpawnModifier, niToJ3dData);
		this.niPSysSpawnModifier = niPSysSpawnModifier;
	}

	@Override
	public void updatePSys(long elapsedMillisec)
	{
		// possibly I should only create the spawned particles now instead of dduring the particleDeath call?
	}

	private Vector3f vel = new Vector3f();

	private Point3f pos = new Point3f();

	private Color4f col = new Color4f();

	/**
	 * Called by agedeath if spawning is on
	 * @param id
	 */
	public void particleDeath(int id)
	{
		// get the details out of the template particle now as it is being deleted

		J3dPSysData j3dPSysData = j3dNiParticleSystem.j3dPSysData;

		//  check generation num
		int particleGeneration = j3dPSysData.particleGeneration[id] + 1;
		if (particleGeneration < niPSysSpawnModifier.numSpawnGenerations)
		{
			float[] particleColors = j3dPSysData.particleColors;
			col.set(particleColors[id * 4 + 0], particleColors[id * 4 + 1], particleColors[id * 4 + 2], particleColors[id * 4 + 3]);

			float particleRadius = j3dPSysData.particleRadius[id];
			float particleSize = j3dPSysData.particleSize[id];

			float[] particleTranslation = j3dPSysData.particleTranslation;
			pos.set(particleTranslation[id * 3 + 0], particleTranslation[id * 3 + 1], particleTranslation[id * 3 + 2]);

			float[] particleVelocity = j3dPSysData.particleVelocity;
			vel.set(particleVelocity[id * 3 + 0], particleVelocity[id * 3 + 1], particleVelocity[id * 3 + 2]);

			int numToSpawn = niPSysSpawnModifier.minNumtoSpawn;
			int numToSpawnRnd = niPSysSpawnModifier.maxNumtoSpawn - niPSysSpawnModifier.minNumtoSpawn;
			numToSpawnRnd = numToSpawnRnd < 0 ? 0 : numToSpawnRnd;
			numToSpawnRnd = (int) (Math.random() * numToSpawnRnd);
			numToSpawn += numToSpawnRnd;

			for (int i = 0; i < numToSpawn; i++)
			{
				float lifeSpan = niPSysSpawnModifier.lifeSpan;
				lifeSpan += var(niPSysSpawnModifier.lifeSpanVariation);
				lifeSpan *= 1000;// it's in seconds, convert to ms

				//TODO: do speed and direction chaos now

				int newParticleId = j3dNiParticleSystem.j3dPSysData.addActive(particleRadius, particleSize,  (long) lifeSpan, particleGeneration, pos.x,
						pos.y, pos.z, col.x, col.y, col.z, col.w, vel.x, vel.y, vel.z);

				j3dNiParticleSystem.particleCreated(newParticleId);
			}
		}

	}
}
