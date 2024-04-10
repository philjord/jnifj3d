package nif.j3d.particles;

import nif.j3d.NiToJ3dData;
import nif.niobject.NiColorData;
import nif.niobject.particle.NiPSysColorModifier;

public class J3dNiPSysColorModifier extends J3dNiPSysModifier
{
	private NiColorData niColorData;

	public J3dNiPSysColorModifier(NiPSysColorModifier niPSysColorModifier, NiToJ3dData niToJ3dData)
	{
		super(niPSysColorModifier, niToJ3dData);
		niColorData = (NiColorData) niToJ3dData.get(niPSysColorModifier.data);
	}

	@Override
	public void updatePSys(long elapsedMillisec)
	{
		// this has a keygroup, which should be controlled by an interpolator
		// but in the case of not I'll just set the first one
		J3dPSysData j3dPSysData = j3dNiParticleSystem.j3dPSysData;

		float[] cs = j3dPSysData.particleColors;

		for (int i = 0; i < j3dPSysData.activeParticleCount; i++)
		{
			cs[i * 4 + 0] = (niColorData.data.value[0]).r;
			cs[i * 4 + 1] = (niColorData.data.value[0]).g;
			cs[i * 4 + 2] = (niColorData.data.value[0]).b;
			cs[i * 4 + 3] = (niColorData.data.value[0]).a;
		}
		j3dPSysData.recalcAllGaColors();
	}

}
