package nif.j3d.particles;

import nif.j3d.NiToJ3dData;
import nif.niobject.NiObject;
import nif.niobject.particle.NiPSysAgeDeathModifier;
import nif.niobject.particle.NiPSysSpawnModifier;

public class J3dNiPSysAgeDeathModifier extends J3dNiPSysModifier
{
	private boolean spawnOnDeath = false;

	private J3dNiPSysSpawnModifier j3dNiPSysSpawnModifier;

	public J3dNiPSysAgeDeathModifier(NiPSysAgeDeathModifier niPSysAgeDeathModifier, NiToJ3dData niToJ3dData)
	{
		super(niPSysAgeDeathModifier, niToJ3dData);
		spawnOnDeath = niPSysAgeDeathModifier.spawnOnDeath;

		NiObject niObject = niToJ3dData.get(niPSysAgeDeathModifier.spawnModifier);
		if (niObject instanceof NiPSysSpawnModifier)
		{
			j3dNiPSysSpawnModifier = (J3dNiPSysSpawnModifier) j3dNiParticleSystem.getJ3dNiPSysModifier((NiPSysSpawnModifier) niObject,
					niToJ3dData);
		}
	}

	@Override
	public void updatePSys(long elapsedMillisec)
	{
		J3dPSysData j3dPSysData = j3dNiParticleSystem.j3dPSysData;

		long[] as = j3dPSysData.particleAge; // in milliseconds
		long[] lss = j3dPSysData.particleLifeSpan; // in ms

		for (int i = 0; i < j3dPSysData.activeParticleCount; i++)
		{
			as[i] += elapsedMillisec;
			// is the particle past it's lifespan?
			if (lss[i] < as[i])
			{
				if (spawnOnDeath)
				{
					j3dNiPSysSpawnModifier.particleDeath(i);
				}
				// note teh spawn above need the data alive so delete after telling it about it
				j3dPSysData.inactivateParticle(i);
			}
		}

	}

}
