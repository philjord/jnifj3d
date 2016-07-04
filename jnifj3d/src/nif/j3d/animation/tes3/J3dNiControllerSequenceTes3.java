package nif.j3d.animation.tes3;

import java.util.ArrayList;

import javax.media.j3d.BranchGroup;
import javax.media.j3d.Group;

import nif.NifJ3dVisRoot;
import nif.j3d.J3dNiDefaultAVObjectPalette;
import nif.j3d.animation.J3dNiControllerSequence;
import nif.j3d.animation.tes3.J3dNiSequenceStreamHelper.TimeKeyValue;
import nif.niobject.NiControllerSequence;

public class J3dNiControllerSequenceTes3 extends J3dNiControllerSequence
{
	//TODO: where is babelfishes head? and it's a geom morph too (hopefully that's a freebie)

	private BranchGroup bg = new BranchGroup();

	private float offsetStartS = 0;

	private float offsetStopS = 0;

	/**
	 * This is a group but it needs to be attached/detached from scene many times, it owns it's own branch
	 * @param fireName
	 * @param tkvs
	 * @param keyValuesStartposition 
	 * @param j3dNiKeyframeControllers
	 */
	public J3dNiControllerSequenceTes3(String fireName, TimeKeyValue[] tkvs, int keyValuesStartposition,
			J3dNiKeyframeController[] j3dNiKeyframeControllers)
	{
		this.fireName = fireName;
		cycleType = NiControllerSequence.CYCLE_LOOP;// encourage looping

		j3dNiTextKeyExtraData = new J3dNiTextKeyExtraDataTes3(fireName, tkvs, keyValuesStartposition);

		//notice these are not the same definitions as J3dNiControllerSequence
		offsetStartS = j3dNiTextKeyExtraData.getStartTime();
		startTimeS = 0;
		offsetStopS = j3dNiTextKeyExtraData.getEndTime();
		stopTimeS = offsetStopS - offsetStartS;

		lengthS = stopTimeS - startTimeS;

		lengthMS = (long) (lengthS * 1000);

		controlledBlocks = new J3dNiControllerLinkTes3[j3dNiKeyframeControllers.length];

		for (int i = 0; i < j3dNiKeyframeControllers.length; i++)
		{
			J3dNiKeyframeController j3dNiKeyframeController = j3dNiKeyframeControllers[i];
			J3dNiControllerLinkTes3 j3dControllerLink = new J3dNiControllerLinkTes3(j3dNiKeyframeController, startTimeS, stopTimeS,
					offsetStartS);
			controlledBlocks[i] = j3dControllerLink;
			addChild(j3dControllerLink);
		}

		// set up our branchgroup
		bg.setCapability(BranchGroup.ALLOW_DETACH);
		bg.setCapability(Group.ALLOW_CHILDREN_WRITE);

		bg.addChild(this);
	}

	public BranchGroup getBranchGroup()
	{
		return bg;
	}

	/**
	 * handed in at contrcution not here
	 * @see nif.j3d.animation.J3dNiControllerSequence#setAnimatedNodes(nif.j3d.J3dNiDefaultAVObjectPalette)
	 */
	@Override
	public void setAnimatedNodes(J3dNiDefaultAVObjectPalette allBonesInSkeleton)
	{
		throw new UnsupportedOperationException();
	}

	/**
	 * handed in at contrcution not here
	 * @see nif.j3d.animation.J3dNiControllerSequence#setAnimatedNodes(nif.j3d.J3dNiDefaultAVObjectPalette)
	 */
	@Override
	public void setAnimatedNodes(J3dNiDefaultAVObjectPalette allBonesInSkeleton, ArrayList<NifJ3dVisRoot> allOtherModels)
	{
		throw new UnsupportedOperationException();
	}
}
