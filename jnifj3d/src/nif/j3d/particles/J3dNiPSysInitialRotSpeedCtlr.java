package nif.j3d.particles;

import nif.niobject.particle.NiPSysInitialRotSpeedCtlr;

public class J3dNiPSysInitialRotSpeedCtlr extends J3dNiPSysModifierCtlr
{
	private J3dNiPSysRotationModifier j3dNiPSysRotationModifier;

	public J3dNiPSysInitialRotSpeedCtlr(NiPSysInitialRotSpeedCtlr niPSysInitialRotSpeedCtlr, J3dNiPSysRotationModifier j3dNiPSysRotationModifier)
	{
		super(niPSysInitialRotSpeedCtlr, j3dNiPSysRotationModifier);
		this.j3dNiPSysRotationModifier = j3dNiPSysRotationModifier;
	}

	@Override
	public void update(float value)
	{
		j3dNiPSysRotationModifier.updateInitialRotSpeed(value);
	}
}
