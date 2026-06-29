package nif.j3d.particles;

import nif.j3d.NiToJ3dData;
import nif.niobject.bs.BSPSysLODModifier;

public class J3dBSPSysLODModifier extends J3dNiPSysModifier {
	/**
	 * 
	 * <niobject name="BSPSysLODModifier" inherit="NiPSysModifier">
	 * <add name="Uknown Float 1" type="float">Unknown</add> 
	 * <add name="Uknown Float 2" type="float">Unknown</add>
	 * <add name="Uknown Float 3" type="float">Unknown</add> 
	 * <add name="Uknown Float 4" type="float">Unknown</add>
	 * </niobject>
	 */
	public J3dBSPSysLODModifier(BSPSysLODModifier niPSysModifier, NiToJ3dData niToJ3dData) {
		super(niPSysModifier, niToJ3dData);
		if (J3dNiParticleSystem.DEBUG_DATA && J3dNiParticleSystem.MODIFIER_DEBUG_DATA) {
			System.out.print("J3dBSPSysLODModifier");
			System.out.print(" UnknownFloat1 " + niPSysModifier.UnknownFloat1);
			System.out.print(" UnknownFloat2 " + niPSysModifier.UnknownFloat2);
			System.out.print(" UnknownFloat3 " + niPSysModifier.UnknownFloat3);
			System.out.println(" UnknownFloat4 " + niPSysModifier.UnknownFloat4);

			//J3dBSPSysLODModifier UnknownFloat1 0.033333335 UnknownFloat2 0.23333333 UnknownFloat3 0.2 UnknownFloat4 1.0
			// looks a bit like perhaps update speeds getting slower as it's further away? No 2 is bigger than 3
			// I notice skyrim has about 4 lod levels in nifskop as render slider
		}
	}

	@Override
	public void updatePSys(long elapsedMillisec) {
		// TODO Auto-generated method stub

	}

}
