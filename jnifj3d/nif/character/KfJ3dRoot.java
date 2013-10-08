package nif.character;

import javax.media.j3d.BranchGroup;

import nif.j3d.J3dNiDefaultAVObjectPalette;
import nif.j3d.NiToJ3dData;
import nif.j3d.animation.J3dNiControllerSequence;
import nif.niobject.NiControllerSequence;

public class KfJ3dRoot extends BranchGroup
{
	private J3dNiControllerSequence j3dNiControllerSequence;

	public KfJ3dRoot(NiControllerSequence kfNiControllerSequence, NiToJ3dData niToJ3dData)
	{
		this.setCapability(BranchGroup.ALLOW_DETACH);
		j3dNiControllerSequence = new J3dNiControllerSequence(kfNiControllerSequence, niToJ3dData);
		addChild(j3dNiControllerSequence);
	}

	public void setAnimatedSkeleton(J3dNiDefaultAVObjectPalette allBonesInSkeleton)
	{
		j3dNiControllerSequence.setAnimatedNodes(allBonesInSkeleton);
	}

	public J3dNiControllerSequence getJ3dNiControllerSequence()
	{
		return j3dNiControllerSequence;
	}

}