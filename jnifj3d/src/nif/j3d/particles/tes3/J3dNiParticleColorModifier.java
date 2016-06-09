package nif.j3d.particles.tes3;

import nif.compound.NifColor4;
import nif.j3d.NiToJ3dData;
import nif.niobject.NiColorData;
import nif.niobject.particle.NiParticleColorModifier;

public class J3dNiParticleColorModifier extends J3dNiParticleModifier
{
	private NiColorData niColorData;

	public J3dNiParticleColorModifier(NiParticleColorModifier niParticleColorModifier,
			J3dNiAutoNormalParticlesData j3dNiAutoNormalParticlesData, NiToJ3dData niToJ3dData)
	{
		super(niParticleColorModifier, j3dNiAutoNormalParticlesData, niToJ3dData);
		niColorData = (NiColorData) niToJ3dData.get(niParticleColorModifier.colorData);
	}

	@Override
	public void updatePSys(long elapsedMillisec)
	{
		// this has a keygroup, which should be controlled by an interpolator
		// but in the case of not I'll just set the first one
		 

		float[] cs = j3dNiAutoNormalParticlesData.particleColors;

		for (int i = 0; i < j3dNiAutoNormalParticlesData.activeParticleCount; i++)
		{
			cs[i * 4 + 0] = ((NifColor4) niColorData.data.keys[0].value).r;
			cs[i * 4 + 1] = ((NifColor4) niColorData.data.keys[0].value).g;
			cs[i * 4 + 2] = ((NifColor4) niColorData.data.keys[0].value).b;
			cs[i * 4 + 3] = ((NifColor4) niColorData.data.keys[0].value).a;
		}
		j3dNiAutoNormalParticlesData.resetAllGaColors();

	}
}
