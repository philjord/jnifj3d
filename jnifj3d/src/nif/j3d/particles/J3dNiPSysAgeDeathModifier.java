package nif.j3d.particles;

import nif.j3d.NiToJ3dData;
import nif.niobject.NiObject;
import nif.niobject.particle.NiPSysAgeDeathModifier;
import nif.niobject.particle.NiPSysSpawnModifier;

public class J3dNiPSysAgeDeathModifier extends J3dNiPSysModifier {
	private boolean					spawnOnDeath	= false;

	private J3dNiPSysSpawnModifier	j3dNiPSysSpawnModifier;

	public J3dNiPSysAgeDeathModifier(NiPSysAgeDeathModifier niPSysAgeDeathModifier, NiToJ3dData niToJ3dData) {
		super(niPSysAgeDeathModifier, niToJ3dData);
		spawnOnDeath = niPSysAgeDeathModifier.spawnOnDeath;

		NiObject niObject = niToJ3dData.get(niPSysAgeDeathModifier.spawnModifier);
		if (niObject instanceof NiPSysSpawnModifier) {
			j3dNiPSysSpawnModifier = (J3dNiPSysSpawnModifier)j3dNiParticleSystem
					.getJ3dNiPSysModifier((NiPSysSpawnModifier)niObject, niToJ3dData);
		}
		

		if (J3dNiParticleSystem.DEBUG_DATA && J3dNiParticleSystem.MODIFIER_DEBUG_DATA) {
			System.out.print("J3dNiPSysAgeDeathModifier");
			System.out.println(" spawnOnDeath " + spawnOnDeath);			
		}
		
	}

	@Override
	public void updatePSys(long elapsedMillisec) {
		//System.out.println("J3dNiPSysAgeDeathModifier updatePSys");
		J3dPSysData j3dPSysData = j3dNiParticleSystem.j3dPSysData;

		//NOTE aging is done in ParticleSystem, cos I can't be sure an age death modifier exists but grow fade color mod needs age
		long[] as = j3dPSysData.particleAge; // in milliseconds
		long[] lss = j3dPSysData.particleLifeSpan; // in ms

		for (int i = 0; i < j3dPSysData.activeParticleCount; i++) {
			// is the particle past it's lifespan?
			if (lss[i] < as[i]) {
				if (spawnOnDeath && j3dNiPSysSpawnModifier != null) {
					j3dNiPSysSpawnModifier.particleDeath(i);
				}

				// check to make sure the grow fade has got it back to 0 in size 
				// though it would if it hit a psyscollider 
				// also very common for something with a very short lifespan (250ms)
				//if (J3dNiParticleSystem.DEBUG_DATA && j3dPSysData.particleSize[i] > 0.2) {
				//	System.out.println("***********Inactive particle " + i + " but not back to 0 " + j3dPSysData.particleSize[i]);
				//	System.out.println("***********Inactive particle " + j3dPSysData.particleAge[i] + " ls " +  j3dPSysData.particleLifeSpan[i]);
				//}

				// note the spawn above need the data alive so delete after telling it about it
				j3dPSysData.inactivateParticle(i);
			}
		}

	}

}
