package nif.j3d.particles.tes3;

import nif.j3d.NiToJ3dData;
import nif.niobject.particle.NiAutoNormalParticles;
import nif.niobject.particle.NiAutoNormalParticlesData;
import nif.niobject.particle.NiParticles;
import utils.source.TextureSource;

public class J3dNiAutoNormalParticles extends J3dNiParticles
{
	public J3dNiAutoNormalParticles(NiAutoNormalParticles niAutoNormalParticles, NiToJ3dData niToJ3dData, TextureSource textureSource)
	{
		super(niAutoNormalParticles, niToJ3dData, textureSource);
	}

	@Override
	protected void init(NiParticles niParticles, NiToJ3dData niToJ3dData, TextureSource textureSource)
	{
		NiAutoNormalParticlesData niAutoNormalParticlesData = (NiAutoNormalParticlesData) niToJ3dData.get(niParticles.data);

		if (niAutoNormalParticlesData != null)
		{
			j3dNiParticlesData = new J3dNiAutoNormalParticlesData(niAutoNormalParticlesData);
		}

		super.init(niParticles, niToJ3dData, textureSource);
	}

}
