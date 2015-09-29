package nif.character;

import java.util.ArrayList;

import javax.media.j3d.BranchGroup;

import nif.NifJ3dVisRoot;
import nif.j3d.J3dNiDefaultAVObjectPalette;
import nif.j3d.NiToJ3dData;
import nif.j3d.animation.J3dNiControllerSequence;
import nif.j3d.animation.tes3.J3dNiSequenceStreamHelper;
import nif.niobject.NiControllerSequence;
import nif.niobject.NiSequenceStreamHelper;

public class KfJ3dRoot extends BranchGroup
{
	private J3dNiControllerSequence j3dNiControllerSequence;

	private J3dNiSequenceStreamHelper j3dNiSequenceStreamHelper;

	public KfJ3dRoot(NiControllerSequence kfNiControllerSequence, NiToJ3dData niToJ3dData)
	{
		this.setCapability(BranchGroup.ALLOW_DETACH);
		j3dNiControllerSequence = new J3dNiControllerSequence(kfNiControllerSequence, niToJ3dData);
		addChild(j3dNiControllerSequence);
	}

	//TES3 version
	public KfJ3dRoot(NiSequenceStreamHelper kfNiSequenceStreamHelper, NiToJ3dData niToJ3dData)
	{
		this.setCapability(BranchGroup.ALLOW_DETACH);
		j3dNiSequenceStreamHelper = new J3dNiSequenceStreamHelper(kfNiSequenceStreamHelper, niToJ3dData);
		addChild(j3dNiSequenceStreamHelper);
	}

	public void setAnimatedSkeleton(J3dNiDefaultAVObjectPalette allBonesInSkeleton, ArrayList<NifJ3dVisRoot> allOtherModels)
	{
		if (j3dNiControllerSequence != null)
			j3dNiControllerSequence.setAnimatedNodes(allBonesInSkeleton, allOtherModels);
		else
			j3dNiSequenceStreamHelper.setAnimatedNodes(allBonesInSkeleton, allOtherModels);
	}

	public J3dNiControllerSequence getJ3dNiControllerSequence()
	{
		return j3dNiControllerSequence;
	}

	public J3dNiSequenceStreamHelper getJ3dNiSequenceStreamHelper()
	{
		return j3dNiSequenceStreamHelper;
	}

}
