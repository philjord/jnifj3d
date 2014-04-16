package nif.j3d.particles;

import nif.j3d.NiToJ3dData;
import nif.niobject.particle.NiPSysPositionModifier;

public class J3dNiPSysPositionModifier extends J3dNiPSysModifier
{
	public J3dNiPSysPositionModifier(NiPSysPositionModifier niPSysPositionModifier, NiToJ3dData niToJ3dData)
	{
		super(niPSysPositionModifier, niToJ3dData);
	}

	@Override
	public void updatePSys(long elapsedMillisec)
	{
		// simply grab the velocity for an active particle and add it on to the translation
		// velocitys are in meters per second
		J3dPSysData j3dPSysData = j3dNiParticleSystem.j3dPSysData;
		float fractionOfSec = elapsedMillisec / 1000f;

		float[] vs = j3dPSysData.particleVelocity;
		float[] ts = j3dPSysData.particleTranslation;
		for (int i = 0; i < j3dPSysData.activeParticleCount; i++)
		{
			ts[i * 3 + 0] += vs[i * 3 + 0] * fractionOfSec;
			ts[i * 3 + 1] += vs[i * 3 + 1] * fractionOfSec;
			ts[i * 3 + 2] += vs[i * 3 + 2] * fractionOfSec;
		}

		// note j3dPSysData.recalcAllGaCoords(); will be called once by the particle system after all modifiers have run
	}

	@Override
	public void particleCreated(int id)
	{

	}
}
