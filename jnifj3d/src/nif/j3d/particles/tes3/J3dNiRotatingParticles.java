package nif.j3d.particles.tes3;

import nif.j3d.NiToJ3dData;
import nif.niobject.particle.NiParticles;
import nif.niobject.particle.NiRotatingParticles;
import nif.niobject.particle.NiRotatingParticlesData;
import utils.source.TextureSource;

public class J3dNiRotatingParticles extends J3dNiParticles
{
	public J3dNiRotatingParticles(NiRotatingParticles niRotatingParticles, NiToJ3dData niToJ3dData, TextureSource textureSource)
	{
		super(niRotatingParticles, niToJ3dData, textureSource);
	}

	@Override
	protected void init(NiParticles niParticles, NiToJ3dData niToJ3dData, TextureSource textureSource)
	{
		NiRotatingParticlesData niRotatingParticlesData = (NiRotatingParticlesData) niToJ3dData.get(niParticles.data);

		if (niRotatingParticlesData != null)
		{
			j3dNiParticlesData = new J3dNiRotatingParticlesData(niRotatingParticlesData);
		}

		super.init(niParticles, niToJ3dData, textureSource);
	}
}
