package nif.gui.util;

import java.awt.GridLayout;
import java.util.HashMap;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import nif.NifFile;
import nif.niobject.NiAVObject;
import nif.niobject.NiNode;
import nif.niobject.NiObject;

public class NifFileDisplayTree extends JPanel
{
	private DefaultMutableTreeNode root = new DefaultMutableTreeNode();

	private HashMap<NiObject, DefaultMutableTreeNode> nodes = new HashMap<NiObject, DefaultMutableTreeNode>();

	private NifTreeModel treeModel = new NifTreeModel(root);

	private JTree tree = new JTree(treeModel);

	private NifFile nifFile;

	private NiObjectDisplayTable niObjectDisplayTable;

	public NifFileDisplayTree(NiObjectDisplayTable niObjectDisplayTable2)
	{
		this.niObjectDisplayTable = niObjectDisplayTable2;
		this.setLayout(new GridLayout(1, 1));
		this.add(new JScrollPane(tree));

		tree.getSelectionModel().addTreeSelectionListener(new TreeSelectionListener()
		{
			@Override
			public void valueChanged(TreeSelectionEvent e)
			{
				if (isEnabled())
				{
					DefaultMutableTreeNode dmtn = (DefaultMutableTreeNode) e.getPath().getLastPathComponent();
					if (dmtn != null && dmtn.getUserObject() != null)
					{
						NiObject niobject = (NiObject) dmtn.getUserObject();
						niObjectDisplayTable.displayNiObject(niobject);
					}
					else
					{
						niObjectDisplayTable.displayNiObject(null);
					}
				}
			}

		});
	}

	public void displayNifFile(NifFile nifFile2)
	{
		this.nifFile = nifFile2;
		root.removeAllChildren();

		for (int i = 0; i < nifFile.blocks.getNiObjects().length; i++)
		{
			NiObject niObject = nifFile.blocks.getNiObjects()[i];
			NifTreeNode node = new NifTreeNode(i, niObject);
			nodes.put(niObject, node);
			findParent(niObject).add(node);
		}
		treeModel.reload();
	}

	private DefaultMutableTreeNode findParent(NiObject niObject)
	{
		if (niObject instanceof NiAVObject)
		{
			NiAVObject niAVObject = (NiAVObject) niObject;
			NiNode parentNode = niAVObject.parent;
			DefaultMutableTreeNode parent = nodes.get(parentNode);
			if (parent != null)
				return parent;

		}
		return root;
	}

	private class NifTreeNode extends DefaultMutableTreeNode
	{
		public NifTreeNode(int idx, NiObject niobject)
		{
			super("" + idx + " " + niobject.getClass() + ":" + niobject.toString());
			this.setUserObject(niobject);
		}
	}

	private class NifTreeModel extends DefaultTreeModel
	{
		public NifTreeModel(DefaultMutableTreeNode root)
		{
			super(root);
		}
	}
}
