package nif.j3d.animation.tes3;

import nif.j3d.animation.J3dControllerLink;

/**
 * A controller link (interp to target) that also transfrom to the chunk alpha 
 * @author phil
 *
 */
public class J3dNiControllerLinkTes3 extends J3dControllerLink
{
	// Notice j3dNiKeyframeController is NOT added to scene graph for this case it is simply a holder of info
	private J3dNiKeyframeController j3dNiKeyframeController;

	private float offSetStartS;// offset into the interp alpha

	private float startTimeS; // 0 in all case(?)

	private float stopTimeS; // length of animation

	public J3dNiControllerLinkTes3(J3dNiKeyframeController j3dNiKeyframeController, float startTimeS, float stopTimeS, float offSetStartS)
	{
		this.j3dNiKeyframeController = j3dNiKeyframeController;
		this.nodeTarget = j3dNiKeyframeController.getNodeTarget();
		if (nodeTarget != null)
		{
			nodeTarget.setCapability(ALLOW_BOUNDS_READ);

			isAccumNodeTarget = nodeTarget.getName().equals("Bip01");
		}

		this.offSetStartS = offSetStartS;
		this.startTimeS = startTimeS;
		this.stopTimeS = stopTimeS;

		j3dNiInterpolator = j3dNiKeyframeController.getJ3dNiInterpolator();
	}

	@Override
	public void process(float alphaValue)
	{
		if (j3dNiInterpolator != null)
		{
			// translate from 0-1 over the whole interp to just  tiny bit for the start-stop portion 
			float chunkStart = offSetStartS / j3dNiKeyframeController.getTotalLengthS();
			float chunkLen = stopTimeS / j3dNiKeyframeController.getTotalLengthS();
			float chunkAlphaValue = chunkStart + (alphaValue * chunkLen);

			j3dNiInterpolator.process(chunkAlphaValue);
		}
	}

}
