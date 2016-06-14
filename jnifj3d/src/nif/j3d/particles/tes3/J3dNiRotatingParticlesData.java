package nif.j3d.particles.tes3;

import nif.niobject.particle.NiRotatingParticlesData;

public class J3dNiRotatingParticlesData extends J3dNiParticlesData
{

	public J3dNiRotatingParticlesData(NiRotatingParticlesData niRotatingParticlesData)
	{
		super(niRotatingParticlesData);

	}

	@Override
	protected void setupInitialParticles()
	{
		super.setupInitialParticles();
		NiRotatingParticlesData niRotatingParticlesData = (NiRotatingParticlesData) niParticlesData;

		particleRotationAngle = new float[maxParticleCount * 1];
		particleRotationSpeed = new float[maxParticleCount * 1];

		if (niRotatingParticlesData.HasRotations2)
		{
			for (int indx = 0; indx < activeParticleCount; indx++)
			{
				//Quat4f r = ConvertFromNif.toJ3d(niRotatingParticlesData.rotations1[indx]);
				//	particleRotationAngle[indx] = r;
				///TODO: how the hell to get from a quat to a particle angle??
			}
		}
	}
}
