package nif.j3d.particles.tes3;

import nif.j3d.NiToJ3dData;
import nif.niobject.particle.NiParticleRotation;

public class J3dNiParticleRotation extends J3dNiParticleModifier
{
	private NiParticleRotation niParticleRotation;

	private float initialRotSpeed;

	public J3dNiParticleRotation(NiParticleRotation niParticleRotation, J3dNiParticlesData j3dNiParticlesData, NiToJ3dData niToJ3dData)
	{
		super(niParticleRotation, j3dNiParticlesData, niToJ3dData);
		this.niParticleRotation = niParticleRotation;
		this.initialRotSpeed = niParticleRotation.rotationSpeed;

	}

	public void updateInitialRotSpeed(float value)
	{
		initialRotSpeed = value;
	}

	@Override
	public void updateParticles(long elapsedMillisec)
	{
		float fractionOfSec = elapsedMillisec / 1000;
		// simply grab the rotation speed for an active particle and add it on to the current rotation
		// velocitys are in meters per second

		float[] rss = j3dNiParticlesData.particleRotationSpeed;
		float[] ras = j3dNiParticlesData.particleRotationAngle;
		for (int i = 0; i < j3dNiParticlesData.activeParticleCount; i++)
		{
			ras[i] += rss[i] * fractionOfSec;
		}
	}

	@Override
	public void particleCreated(int id)
	{
		//initialAxis TODO: how can it have an axis? possibly to reverse rotation?

		float rotSpeed = initialRotSpeed;
		rotSpeed += var(niParticleRotation.rotationSpeed * 2);

		// just random
		float rotAngle = 0;
		rotAngle += var((float) (Math.PI * 2));

		j3dNiParticlesData.particleRotationSpeed[id] = rotSpeed;
		j3dNiParticlesData.particleRotationAngle[id] = rotAngle;

	}
}
