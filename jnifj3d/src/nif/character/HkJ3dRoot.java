package nif.character;

import java.util.ArrayList;

import org.jogamp.java3d.BranchGroup;

import nif.NifJ3dVisRoot;
import nif.j3d.J3dNiDefaultAVObjectPalette;
import nif.j3d.animation.hkx.J3dhkaAnimationContainer;
import nif.niobject.hkx.hkBaseObject;
import nif.niobject.hkx.animation.hkRootLevelContainer;
import nif.niobject.hkx.animation.hkRootLevelContainer.hkRootLevelContainerNamedVariant;
import nif.niobject.hkx.animation.hkaAnimationContainer;
import nif.niobject.hkx.reader.HKXContents;

public class HkJ3dRoot extends BranchGroup {
	private J3dhkaAnimationContainer j3dhkaAnimationContainer;

	private HKXContents hkxSkeletonContents;
	
	public HkJ3dRoot(HKXContents hkxContents, HKXContents hkxSkeletonContents) {
		this.setCapability(BranchGroup.ALLOW_DETACH);
		this.hkxSkeletonContents = hkxSkeletonContents;

		hkRootLevelContainer hkRootLevelContainer = (hkRootLevelContainer)hkxContents.get(0);
		// grab the first variant option for fun
		hkRootLevelContainerNamedVariant var0 = hkRootLevelContainer.NamedVariants[0];
		hkBaseObject variant = hkxContents.get(var0.variant);
		if (variant instanceof hkaAnimationContainer) {
			j3dhkaAnimationContainer = new J3dhkaAnimationContainer((hkaAnimationContainer)variant, hkxContents);
			addChild(j3dhkaAnimationContainer);
		} else {
			System.out.println("odd variant[0] not hkaAnimationContainer but " + variant);
		}
	}

	public void setAnimatedSkeleton(J3dNiDefaultAVObjectPalette allBonesInSkeleton,
									ArrayList<NifJ3dVisRoot> allOtherModels) {
		j3dhkaAnimationContainer.setAnimatedNodes(allBonesInSkeleton, allOtherModels, hkxSkeletonContents);
	}

	public J3dhkaAnimationContainer getJ3dhkaAnimationContainer() {
		return j3dhkaAnimationContainer;
	}
}
