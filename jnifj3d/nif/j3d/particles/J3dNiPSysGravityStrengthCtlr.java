package nif.j3d.particles;

import nif.j3d.NiToJ3dData;
import nif.niobject.particle.NiPSysGravityStrengthCtlr;

public class J3dNiPSysGravityStrengthCtlr extends J3dNiPSysModifierFloatCtlr
{
	private J3dNiPSysGravityModifier j3dNiPSysGravityModifier;

	public J3dNiPSysGravityStrengthCtlr(NiPSysGravityStrengthCtlr niPSysGravityStrengthCtlr, NiToJ3dData niToJ3dData,
			J3dNiPSysGravityModifier j3dNiPSysGravityModifier)
	{
		super(niPSysGravityStrengthCtlr, niToJ3dData, j3dNiPSysGravityModifier);
		this.j3dNiPSysGravityModifier = j3dNiPSysGravityModifier;
	}

	@Override
	public void update(float value)
	{
		j3dNiPSysGravityModifier.updateStrength(value);
	}

}
