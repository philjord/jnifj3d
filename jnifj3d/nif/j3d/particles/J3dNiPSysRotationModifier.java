package nif.j3d.particles;

import nif.j3d.NiToJ3dData;
import nif.niobject.particle.NiPSysRotationModifier;

public class J3dNiPSysRotationModifier extends J3dNiPSysModifier
{
	private NiPSysRotationModifier niPSysRotationModifier;

	private float initialRotSpeed;

	public J3dNiPSysRotationModifier(NiPSysRotationModifier niPSysRotationModifier, NiToJ3dData niToJ3dData)
	{
		super(niPSysRotationModifier, niToJ3dData);
		this.niPSysRotationModifier = niPSysRotationModifier;
		this.initialRotSpeed = niPSysRotationModifier.initialRotationSpeed;
	}

	public void updateInitialRotSpeed(float value)
	{
		initialRotSpeed = value;
	}

	@Override
	public void updatePSys(long elapsedMillisec)
	{
		// simply grab the rotation speed for an active particle and add it on to the current rotation
		// velocitys are in meters per second
		J3dPSysData j3dPSysData = j3dNiParticleSystem.j3dPSysData;
		float fractionOfSec = elapsedMillisec / 1000f;

		float[] rss = j3dPSysData.particleRotationSpeed;
		float[] ras = j3dPSysData.particleRotationAngle;
		for (int i = 0; i < j3dPSysData.activeParticleCount; i++)
		{
			ras[i] += rss[i] * fractionOfSec;
		}

		// note j3dPSysData.recalcAllGaCoords(); will be called once by the particle system after all modifiers have run

	}

	@Override
	public void particleCreated(int id)
	{

		float rotSpeed = initialRotSpeed;
		rotSpeed += var(niPSysRotationModifier.initialRotationSpeedVariation * 2);
		if (niPSysRotationModifier.randomRotSpeedSign && Math.random() > 0.5)
		{
			rotSpeed = -rotSpeed;
		}

		float rotAngle = niPSysRotationModifier.initialRotationAngle;
		rotAngle += var(niPSysRotationModifier.initialRotationAngleVariation * 2);

		//TODO:
		//niPSysRotationModifier.randomInitialAxis;
		//niPSysRotationModifier.initialAxis;

		J3dPSysData j3dPSysData = j3dNiParticleSystem.j3dPSysData;

		j3dPSysData.particleRotationSpeed[id] = rotSpeed;
		j3dPSysData.particleRotationAngle[id] = rotAngle;

	}

}
