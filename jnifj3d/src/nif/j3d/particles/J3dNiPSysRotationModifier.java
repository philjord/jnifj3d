package nif.j3d.particles;

import nif.j3d.NiToJ3dData;
import nif.niobject.particle.NiPSysRotationModifier;

/**
 * <niobject name="NiPSysRotationModifier" inherit="NiPSysModifier" module="NiParticle"> Particle modifier that adds
 * rotations to particles. <field name="Rotation Speed" type="float">Initial Rotation Speed in radians per
 * second.</field> <field name="Rotation Speed Variation" type="float" since="20.0.0.2">Distributes rotation speed over
 * the range [Speed - Variation, Speed + Variation].</field>
 * <field name="Unknown Vector" type="Vector4" vercond="#BS_GTE_F76#" />
 * <field name="Unknown Byte" type="byte" vercond="#BS_GTE_F76#" />
 * <field name="Rotation Angle" type="float" since="20.0.0.2">Initial Rotation Angle in radians.</field>
 * <field name="Rotation Angle Variation" type="float" since="20.0.0.2">Distributes rotation angle over the range [Angle
 * - Variation, Angle + Variation].</field> <field name="Random Rot Speed Sign" type="bool" since="20.0.0.2">Randomly
 * negate the initial rotation speed?</field> <field name="Random Axis" type="bool" default="true">Assign a random axis
 * to new particles?</field> <field name="Axis" type="Vector3" default="#X_AXIS#">Initial rotation axis.</field>
 * </niobject>
 */
public class J3dNiPSysRotationModifier extends J3dNiPSysModifier {
	private NiPSysRotationModifier	niPSysRotationModifier;

	private float					initialRotSpeed;
	private float					initialRotationSpeedVariation;

	public J3dNiPSysRotationModifier(NiPSysRotationModifier niPSysRotationModifier, NiToJ3dData niToJ3dData) {
		super(niPSysRotationModifier, niToJ3dData);
		this.niPSysRotationModifier = niPSysRotationModifier;
		this.initialRotSpeed = niPSysRotationModifier.initialRotationSpeed;
		this.initialRotationSpeedVariation = niPSysRotationModifier.initialRotationAngleVariation;

		if (J3dNiParticleSystem.DEBUG_DATA && J3dNiParticleSystem.MODIFIER_DEBUG_DATA) {
			System.out.print("J3dNiPSysRotationModifier");
			System.out.print(" initialRotSpeed " + initialRotSpeed);
			System.out.print(" initialRotationSpeedVariation " + initialRotationSpeedVariation);
			System.out.print(" randomRotSpeedSign " + niPSysRotationModifier.randomRotSpeedSign);
			System.out.print(" initialRotationAngle " + niPSysRotationModifier.initialRotationAngle);
			System.out
					.println(" initialRotationAngleVariation " + niPSysRotationModifier.initialRotationAngleVariation);
		}
	}

	public void updateInitialRotSpeed(float value) {
		initialRotSpeed = value;
	}

	public void updateInitialRotSpeedVar(float value) {
		initialRotationSpeedVariation = value;
	}

	@Override
	public void updatePSys(long elapsedMillisec) {
		// simply grab the rotation speed for an active particle and add it on to the current rotation
		J3dPSysData j3dPSysData = j3dNiParticleSystem.j3dPSysData;
		float fractionOfSec = elapsedMillisec / 1000f;

		float[] rss = j3dPSysData.particleRotationSpeed;
		float[] ras = j3dPSysData.particleRotationAngle;
		for (int i = 0; i < j3dPSysData.activeParticleCount; i++) {
			ras[i] += rss[i] * fractionOfSec;
		}

		// note j3dPSysData.recalcAllGaCoords(); will be called once by the particle system after all modifiers have run

	}

	@Override
	public void particleCreated(int id) {

		float rotSpeed = initialRotSpeed;
		rotSpeed += varFull(initialRotationSpeedVariation);
		
		if (niPSysRotationModifier.randomRotSpeedSign && Math.random() > 0.5) {
			rotSpeed = -rotSpeed;
		}

		float rotAngle = niPSysRotationModifier.initialRotationAngle;
		rotAngle += varFull(niPSysRotationModifier.initialRotationAngleVariation);

		//TODO:
		//niPSysRotationModifier.randomInitialAxis;
		//niPSysRotationModifier.initialAxis;

		J3dPSysData j3dPSysData = j3dNiParticleSystem.j3dPSysData;

		j3dPSysData.particleRotationSpeed[id] = rotSpeed;
		j3dPSysData.particleRotationAngle[id] = rotAngle;

	}

}
