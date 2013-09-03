package nif.j3d;

import javax.media.j3d.Link;
import javax.media.j3d.Node;
import javax.media.j3d.SharedGroup;

import nif.NifToJ3d;
import nif.basic.NifRef;
import nif.j3d.particles.J3dNiParticleSystem;
import nif.niobject.NiAVObject;
import nif.niobject.NiAmbientLight;
import nif.niobject.NiBillboardNode;
import nif.niobject.NiBone;
import nif.niobject.NiCamera;
import nif.niobject.NiDirectionalLight;
import nif.niobject.NiLODNode;
import nif.niobject.NiNode;
import nif.niobject.NiObject;
import nif.niobject.NiPointLight;
import nif.niobject.NiProperty;
import nif.niobject.NiRoom;
import nif.niobject.NiRoomGroup;
import nif.niobject.NiSwitchNode;
import nif.niobject.NiTriBasedGeom;
import nif.niobject.NiTriShape;
import nif.niobject.NiTriStrips;
import nif.niobject.bs.BSFadeNode;
import nif.niobject.bs.BSLODTriShape;
import nif.niobject.bs.BSLeafAnimNode;
import nif.niobject.bs.BSLightingShaderProperty;
import nif.niobject.bs.BSMasterParticleSystem;
import nif.niobject.bs.BSMultiBoundNode;
import nif.niobject.bs.BSOrderedNode;
import nif.niobject.bs.BSStripParticleSystem;
import nif.niobject.particle.NiMeshParticleSystem;
import nif.niobject.particle.NiParticleSystem;
import tools.WeakValueHashMap;
import utils.source.TextureSource;

public class J3dNiNode extends J3dNiAVObject
{

	protected J3dNiNode(NiNode niNode, NiToJ3dData niToJ3dData, TextureSource textureSource, boolean onlyNiNodes)
	{
		super(niNode, niToJ3dData);

		if (NifToJ3d.HIDE_EDITORS && isEditorMarker(niNode.name))
		{
			return;
		}

		//Black Prophecy physics markers and  
		if (niNode.name.startsWith("PHYS_"))
		{
			return;
		}

		for (int i = 0; i < niNode.numChildren; i++)
		{
			NiAVObject child = (NiAVObject) niToJ3dData.get(niNode.children[i]);
			if (child != null)
			{
				if (NifToJ3d.HIDE_EDITORS && isEditorMarker(child.name))
				{
					return;
				}

				if (child instanceof NiNode)
				{
					J3dNiNode j3dNiNode = createNiNode((NiNode) child, niToJ3dData, textureSource, onlyNiNodes);
					addChild(j3dNiNode);
				}
				else if (!onlyNiNodes)
				{
					if (child instanceof NiTriBasedGeom)
					{
						addChild(loadSharedNiTriBasedGeomVis((NiTriBasedGeom) child, niToJ3dData, textureSource));
					}
					else if (child instanceof NiParticleSystem)
					{
						if (child instanceof BSStripParticleSystem)
						{
							//TODO: this
						}
						else if (child instanceof NiMeshParticleSystem)
						{
							//TODO: this
						}
						else
						{
							J3dNiParticleSystem j3dNiParticleSystem = new J3dNiParticleSystem((NiParticleSystem) child, niToJ3dData,
									textureSource);
							addChild(j3dNiParticleSystem);
						}

					}
					else if (child instanceof NiCamera)
					{
						J3dNiCamera j3dNiCamera = new J3dNiCamera((NiCamera) child, niToJ3dData);
						addChild(j3dNiCamera);
					}
					else if (child instanceof NiAmbientLight)
					{
						J3dNiAmbientLight j3dNiAmbientLight = new J3dNiAmbientLight((NiAmbientLight) child, niToJ3dData);
						addChild(j3dNiAmbientLight);
					}
					else if (child instanceof NiPointLight)
					{
						J3dNiPointLight j3dNiPointLight = new J3dNiPointLight((NiPointLight) child, niToJ3dData);
						addChild(j3dNiPointLight);
					}
					else if (child instanceof NiDirectionalLight)
					{
						//TODO: NiDirectionalLight
						//J3dNiDirectionalLight j3dNiDirectionalLight = new J3dNiDirectionalLight((NiPointLight) child, niToJ3dData);
						//addChild(j3dNiDirectionalLight);
					}

					else
					{
						System.out.println("J3dNiNode - unhandled child NiAVObject " + child);
					}
				}
			}
		}
	}

	public static boolean SHARE_TRIGEOM = true;

	private static WeakValueHashMap<NiTriBasedGeom, SharedGroup> sharedNiTriBasedGeom = new WeakValueHashMap<NiTriBasedGeom, SharedGroup>();

	//private static HashMap<SharedGroup, Integer> counts = new HashMap<SharedGroup, Integer>();

	public static Node loadSharedNiTriBasedGeomVis(NiTriBasedGeom niTriBasedGeom, NiToJ3dData niToJ3dData, TextureSource textureSource)
	{
		SharedGroup sg = sharedNiTriBasedGeom.get(niTriBasedGeom);

		if (sg != null)
		{

			//	System.out.println("count " + niTriBasedGeom.name + " " + (counts.get(sg) + 1));
			//	counts.put(sg, counts.get(sg) + 1);
			Link l = new Link();
			l.setSharedGroup(sg);
			return l;
		}
		else
		{
			J3dNiTriBasedGeom ntbg = null;

			if (niTriBasedGeom instanceof NiTriShape)
			{
				//For now we skip meat caps
				if (niTriBasedGeom.name.toLowerCase().indexOf("meat") == -1 && niTriBasedGeom.name.toLowerCase().indexOf("cap") == -1)
				{
					NiTriShape niTriShape = (NiTriShape) niTriBasedGeom;
					ntbg = new J3dNiTriShape(niTriShape, niToJ3dData, textureSource);
				}
			}
			else if (niTriBasedGeom instanceof BSLODTriShape)
			{
				BSLODTriShape bSLODTriShape = (BSLODTriShape) niTriBasedGeom;
				ntbg = new J3dNiTriShape(bSLODTriShape, niToJ3dData, textureSource);
			}
			else if (niTriBasedGeom instanceof NiTriStrips)
			{
				NiTriStrips niTriStrips = (NiTriStrips) niTriBasedGeom;
				ntbg = new J3dNiTriStrips(niTriStrips, niToJ3dData, textureSource);
			}

			if (SHARE_TRIGEOM && canBeShared(niTriBasedGeom, niToJ3dData))
			{
				sg = new SharedGroup();
				sg.addChild(ntbg);
				sharedNiTriBasedGeom.put(niTriBasedGeom, sg);
				//	counts.put(sg, 1);
				Link l = new Link();
				l.setSharedGroup(sg);
				return l;
			}
			else
			{
				return ntbg;
			}
		}

	}

	private static boolean canBeShared(NiTriBasedGeom niTriBasedGeom, NiToJ3dData niToJ3dData)
	{
		// if your contorller is not -1  or any of your proerties
		if (niTriBasedGeom.controller.ref != -1)
			return false;

		NifRef[] properties = niTriBasedGeom.properties;

		for (int i = 0; i < properties.length; i++)
		{
			NiObject prop = niToJ3dData.get(properties[i]);
			if (prop != null)
			{
				if (prop instanceof NiProperty)
				{
					NiProperty niProperty = (NiProperty) prop;
					if (niProperty.controller.ref != -1)
						return false;
				}
				else if (prop instanceof BSLightingShaderProperty)
				{
					BSLightingShaderProperty bSLightingShaderProperty = (BSLightingShaderProperty) prop;
					if (bSLightingShaderProperty.controller.ref != -1)
						return false;
				}
			}
		}
		return true;
	}

	/**
	 * Note if ninode only then forPhysics is ignored
	 * forPhysics is only for the physics line drawing renderer stuff, havok use ninodeonly
	 * @param niNode
	 * @param niToJ3dData
	 * @param imageDir
	 * @param onlyNiNodes
	 * @param forPhysics
	 * @return
	 */
	public static J3dNiNode createNiNode(NiNode niNode, NiToJ3dData niToJ3dData, TextureSource textureSource, boolean onlyNiNodes)
	{
		if (niNode instanceof NiBillboardNode)
		{
			return new J3dNiBillboardNode((NiBillboardNode) niNode, niToJ3dData, textureSource, onlyNiNodes);
		}
		else if (niNode instanceof BSFadeNode)
		{
			return new J3dBSFadeNode((BSFadeNode) niNode, niToJ3dData, textureSource, onlyNiNodes);
		}
		else if (niNode instanceof BSOrderedNode)
		{
			return new J3dBSOrderedNode((BSOrderedNode) niNode, niToJ3dData, textureSource, onlyNiNodes);
		}
		else if (niNode instanceof NiLODNode)
		{
			return new J3dLODNode((NiLODNode) niNode, niToJ3dData, textureSource, onlyNiNodes);
		}
		else if (niNode instanceof NiSwitchNode)
		{
			return new J3dNiSwitchNode((NiSwitchNode) niNode, niToJ3dData, textureSource, onlyNiNodes);
		}
		else if (niNode instanceof NiBone)
		{
			// not seen in Character dir, possibly not used any more?
			//System.out.println("********************** NiBone");, drop through
		}
		else if (niNode instanceof NiRoom)
		{
			//System.out.println("********************** NiRoom");, drop through
		}
		else if (niNode instanceof NiRoomGroup)
		{
			//System.out.println("********************** NiRoomGroup");, drop through
		}

		else if (niNode instanceof BSMasterParticleSystem)
		{//TODO: BSMasterParticleSystem
			//System.out.println("********************** BSMasterParticleSystem");
		}
		else if (niNode instanceof BSLeafAnimNode)
		{
			//nothng new and interesting, drop through
		}
		else if (niNode instanceof BSMultiBoundNode)
		{
			//nothng new and interesting, drop through
		}

		// return ordinary ninode
		return new J3dNiNode(niNode, niToJ3dData, textureSource, onlyNiNodes);

	}

	private boolean isEditorMarker(String niNodeName)
	{
		// the string data of sgoKeep=1 is some sort of optomisation flag, not a editor marker flag		
		// is it's name is editor maker
		if (niNodeName.startsWith("EditorMarker"))
		{
			return true;
		}
		return false;
	}

}
