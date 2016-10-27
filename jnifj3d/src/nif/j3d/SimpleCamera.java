package nif.j3d;

import org.jogamp.java3d.BranchGroup;
import org.jogamp.java3d.Group;
import org.jogamp.java3d.Node;

public class SimpleCamera extends BranchGroup
{
	public SimpleCamera()
	{
		this.setCapability(Group.ALLOW_CHILDREN_EXTEND);
		this.setCapability(Node.ALLOW_LOCAL_TO_VWORLD_READ);
		this.setCapability(Group.ALLOW_CHILDREN_WRITE);
	}

}
