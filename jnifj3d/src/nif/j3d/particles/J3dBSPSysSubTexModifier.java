package nif.j3d.particles;

import nif.j3d.NiToJ3dData;
import nif.niobject.bs.BSPSysSubTexModifier;

public class J3dBSPSysSubTexModifier extends J3dNiPSysModifier {

	/**
	 <niobject name="BSPSysSubTexModifier"  abstract="0" inherit="NiPSysModifier">
	    Similar to a Flip Controller, this handles particle texture animation on a single texture atlas
	        <add name="Start Frame" type="uint">Starting frame/position on atlas</add>
	        <add name="Start Frame Fudge" type="float">Random chance to start on a different frame?</add>
	        <add name="End Frame" type="float">Ending frame/position on atlas</add>
	        <add name="Loop Start Frame" type="float">Frame to start looping</add>
	        <add name="Loop Start Frame Fudge" type="float"></add>
	        <add name="Frame Count" type="float">Unknown</add>
	        <add name="Frame Count Fudge" type="float">Unknown</add>
	    </niobject>
	 */
	
	public J3dBSPSysSubTexModifier(BSPSysSubTexModifier bSPSysSubTexModifier, NiToJ3dData niToJ3dData) {
		super(bSPSysSubTexModifier, niToJ3dData);

		if (J3dNiParticleSystem.DEBUG_DATA && J3dNiParticleSystem.MODIFIER_DEBUG_DATA) {
			System.out.print("J3dBSPSysSubTexModifier");
			System.out.print(" StartFrame " + bSPSysSubTexModifier.StartFrame);
			System.out.print(" StartFrameFudge " + bSPSysSubTexModifier.StartFrameFudge);
			System.out.print(" EndFrame " + bSPSysSubTexModifier.EndFrame);
			System.out.print(" LoopStartFrame " + bSPSysSubTexModifier.LoopStartFrame);
			System.out.print(" LoopStartFrameFudge " + bSPSysSubTexModifier.LoopStartFrameFudge);
			System.out.print(" FrameCount " + bSPSysSubTexModifier.FrameCount);
			System.out.println(" FrameCountFudge " + bSPSysSubTexModifier.FrameCountFudge);
		}
	}

	public void particleCreated(int pId) {
		//TODO: the bSPSysSubTexModifier has lots of data
		
	}

	//temp tester
	private long accumTime = 0;

	@Override
	public void updatePSys(long elapsedMillisec) {
		
		//temp tester
		// just swap sub tex every 2 seconds to see if anything at all is working at all
		accumTime += elapsedMillisec;
		if (accumTime > 2000) {
			accumTime -= 2000;
			//TODO: lots to be done
			J3dPSysData j3dPSysData = j3dNiParticleSystem.j3dPSysData;
			int[] iis = j3dPSysData.particleImageIds;
			for (int i = 0; i < j3dPSysData.activeParticleCount; i++) {
				iis[i] = iis[i] + 1;
				// wrap
				iis[i] = iis[i] >= j3dPSysData.atlasAnimatedTexture.getSubImageCount() ? 0 : iis[i];
			}

			// Note j3dPSysData.updateAllTexCoords();will be called once by the particle system after all modifiers have run
		}
	}
}
