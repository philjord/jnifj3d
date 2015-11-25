package nif.j3d.particles;

import nif.niobject.particle.NiPSysInitialRotSpeedVarCtlr;

public class J3dNiPSysInitialRotSpeedVarCtlr extends J3dNiPSysModifierCtlr
{
	private J3dNiPSysRotationModifier j3dNiPSysRotationModifier;

	public J3dNiPSysInitialRotSpeedVarCtlr(NiPSysInitialRotSpeedVarCtlr niPSysInitialRotSpeedVarCtlr,
			J3dNiPSysRotationModifier j3dNiPSysRotationModifier)
	{
		super(niPSysInitialRotSpeedVarCtlr, j3dNiPSysRotationModifier);
		this.j3dNiPSysRotationModifier = j3dNiPSysRotationModifier;
	}

	@Override
	public void update(float value)
	{
		
		//TODO: variatio needed here
		//j3dNiPSysRotationModifier.updateInitialRotSpeed(value);
	}
}
