package org.jogamp.java3d;

// FIXME: gettign excpetions here on set live for teh whole grah so need some debugaroonies
/*
 * at java.base/jdk.internal.util.Preconditions.outOfBounds(Preconditions.java:64) at
 * java.base/jdk.internal.util.Preconditions.outOfBoundsCheckIndex(Preconditions.java:70) at
 * java.base/jdk.internal.util.Preconditions.checkIndex(Preconditions.java:248) at
 * java.base/java.util.Objects.checkIndex(Objects.java:372) at java.base/java.util.ArrayList.get(ArrayList.java:458) at
 * org.jogamp.java3d.OrderedGroupRetained.setAuxData(OrderedGroupRetained.java:326) at
 * org.jogamp.java3d.OrderedGroupRetained.setNodeData(OrderedGroupRetained.java:366)
 */

public class MyOrderedGroupRetained extends OrderedGroupRetained {
	@Override
	void setAuxData(SetLiveState s, int index, int hkIndex) {

		OrderedPath setLiveStateOrderedPath = s.orderedPaths.get(hkIndex);
		for (int i = 0; i < children.size(); i++) {
			NodeRetained child = children.get(i);
			if (refCount == s.refCount) {
				// only need to do it once if in shared group when the first
				// instances is to be added
				child.orderedId = getOrderedChildId();
			}

			OrderedPath newOrderedPath = setLiveStateOrderedPath.clonePath();
			newOrderedPath.addElementToPath(this, child.orderedId);

			if (i < childrenOrderedPaths.size()) {
				childrenOrderedPaths.get(i).add(hkIndex, newOrderedPath);
			} else {
				System.out.println("MyOrderedGroupRetained this is where my toruble started");
				//I've got 12 paths and 19 childs? what?
			}
		}
	}
	
	
	@Override
    void childDoSetLive(NodeRetained child, int childIndex, SetLiveState s) {
        if (refCount == s.refCount) {
            s.ogList.add(this);
            s.ogChildIdList.add(new Integer(childIndex));
            s.ogOrderedIdList.add(child.orderedId);
        }
        if (childIndex < childrenOrderedPaths.size()) {
        	s.orderedPaths = childrenOrderedPaths.get(childIndex);
		} else {
			System.out.println("MyOrderedGroupRetained this is where my toruble started2 ");
		}
        
        if(child!=null)
            child.setLive(s);
    }
}
