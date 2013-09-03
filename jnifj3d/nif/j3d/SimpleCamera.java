package nif.j3d;

import javax.media.j3d.BranchGroup;
import javax.media.j3d.Group;
import javax.media.j3d.Node;

public class SimpleCamera extends BranchGroup
{
	public SimpleCamera()
	{
		this.setCapability(Group.ALLOW_CHILDREN_EXTEND);
		this.setCapability(Node.ALLOW_LOCAL_TO_VWORLD_READ);
		this.setCapability(Group.ALLOW_CHILDREN_WRITE);
	}

}
