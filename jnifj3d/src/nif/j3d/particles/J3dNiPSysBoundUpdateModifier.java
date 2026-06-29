package nif.j3d.particles;

import nif.j3d.NiToJ3dData;
import nif.niobject.particle.NiPSysBoundUpdateModifier;

/**
 * <niobject name="NiPSysBoundUpdateModifier" abstract="0" inherit="NiPSysModifier" ver1="10.1.0.0">
 * 
 * Unknown particle system modifier.
 * 
 * <add name="Update Skip" type="ushort">Unknown.</add> </niobject>
 */
public class J3dNiPSysBoundUpdateModifier extends J3dNiPSysModifier {

	public J3dNiPSysBoundUpdateModifier(NiPSysBoundUpdateModifier niPSysModifier, NiToJ3dData niToJ3dData) {
		super(niPSysModifier, niToJ3dData);

		if (J3dNiParticleSystem.DEBUG_DATA && J3dNiParticleSystem.MODIFIER_DEBUG_DATA) {
			System.out.print("J3dNiPSysBoundUpdateModifier");
			System.out.println(" Update Skip " + niPSysModifier.updateSkip);
		}
	}

	@Override
	public void updatePSys(long elapsedMillisec) {
		// FIXME: not done?
		//niPSysModifier.updateSkip of 0 seen, so presumably this guy could set the shape bounds

	}

}
