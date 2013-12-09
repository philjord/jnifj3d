package nif.j3d.particles;

import nif.niobject.particle.NiPSysGravityStrengthCtlr;

public class J3dNiPSysGravityStrengthCtlr extends J3dNiPSysModifierFloatCtlr
{
	private J3dNiPSysGravityModifier j3dNiPSysGravityModifier;

	public J3dNiPSysGravityStrengthCtlr(NiPSysGravityStrengthCtlr niPSysGravityStrengthCtlr,
			J3dNiPSysGravityModifier j3dNiPSysGravityModifier)
	{
		super(niPSysGravityStrengthCtlr, j3dNiPSysGravityModifier);
		this.j3dNiPSysGravityModifier = j3dNiPSysGravityModifier;
	}

	@Override
	public void update(float value)
	{
		j3dNiPSysGravityModifier.updateStrength(value);
	}

}
