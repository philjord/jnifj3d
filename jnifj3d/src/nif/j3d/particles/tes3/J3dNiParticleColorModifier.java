package nif.j3d.particles.tes3;

import nif.compound.NifColor4;
import nif.j3d.NiToJ3dData;
import nif.niobject.NiColorData;
import nif.niobject.particle.NiParticleColorModifier;

public class J3dNiParticleColorModifier extends J3dNiParticleModifier
{
	private NiColorData niColorData;

	public J3dNiParticleColorModifier(NiParticleColorModifier niParticleColorModifier, J3dNiParticlesData j3dNiParticlesData,
			NiToJ3dData niToJ3dData)
	{
		super(niParticleColorModifier, j3dNiParticlesData, niToJ3dData);
		niColorData = (NiColorData) niToJ3dData.get(niParticleColorModifier.colorData);
	}

	@Override
	public void updateParticles(long elapsedMillisec)
	{
		// this has a keygroup, which should be controlled by an interpolator
		// but in the case of not I'll just set the first one

		float[] cs = j3dNiParticlesData.particleColors;

		for (int i = 0; i < j3dNiParticlesData.activeParticleCount; i++)
		{
			NifColor4 nc = (NifColor4) niColorData.data.keys[0].value;
			cs[i * 4 + 0] = nc.r;
			cs[i * 4 + 1] = nc.g;
			cs[i * 4 + 2] = nc.b;
			cs[i * 4 + 3] = nc.a;
		}
		//TODO: this system is crazy must use the incoming elapsed time to interpolate a a value
		// in fact currently I'm setting colors to 0,0,0,1 which should blank everythign out if it's running why not?
		j3dNiParticlesData.resetAllGaColors();

	}
}
