package nif.j3d.particles;

import nif.compound.NifColor4;
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
			cs[i * 4 + 0] = ((NifColor4) niColorData.data.keys[0].value).r;
			cs[i * 4 + 1] = ((NifColor4) niColorData.data.keys[0].value).g;
			cs[i * 4 + 2] = ((NifColor4) niColorData.data.keys[0].value).b;
			cs[i * 4 + 3] = ((NifColor4) niColorData.data.keys[0].value).a;
		}
		j3dPSysData.recalcAllGaColors();
	}

}
