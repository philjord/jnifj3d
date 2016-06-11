package nif.j3d.particles.tes3;

import nif.j3d.NiToJ3dData;
import nif.niobject.particle.NiParticleRotation;

public class J3dNiParticleRotation extends J3dNiParticleModifier
{

	private NiParticleRotation niParticleRotation;

	private float initialRotSpeed;

	public J3dNiParticleRotation(NiParticleRotation niParticleRotation, J3dNiAutoNormalParticlesData j3dNiAutoNormalParticlesData,
			NiToJ3dData niToJ3dData)
	{
		super(niParticleRotation, j3dNiAutoNormalParticlesData, niToJ3dData);
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
		// simply grab the rotation speed for an active particle and add it on to the current rotation
		// velocitys are in meters per second

		float fractionOfSec = elapsedMillisec / 1000f;

		float[] rss = j3dNiAutoNormalParticlesData.particleRotationSpeed;
		float[] ras = j3dNiAutoNormalParticlesData.particleRotationAngle;
		for (int i = 0; i < j3dNiAutoNormalParticlesData.activeParticleCount; i++)
		{
			ras[i] += rss[i] * fractionOfSec;
		}

		// note j3dPSysData.recalcAllGaCoords(); will be called once by the particle system after all modifiers have run

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

		j3dNiAutoNormalParticlesData.particleRotationSpeed[id] = rotSpeed;
		j3dNiAutoNormalParticlesData.particleRotationAngle[id] = rotAngle;

	}
}
