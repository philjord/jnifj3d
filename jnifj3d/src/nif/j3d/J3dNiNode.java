package nif.j3d;

import java.util.ArrayList;

import javax.media.j3d.Transform3D;
import javax.vecmath.Color3f;

import nif.NifToJ3d;
import nif.j3d.particles.J3dNiParticleSystem;
import nif.niobject.NiAVObject;
import nif.niobject.NiAmbientLight;
import nif.niobject.NiBSParticleNode;
import nif.niobject.NiBillboardNode;
import nif.niobject.NiBone;
import nif.niobject.NiCamera;
import nif.niobject.NiDirectionalLight;
import nif.niobject.NiLODNode;
import nif.niobject.NiNode;
import nif.niobject.NiPointLight;
import nif.niobject.NiRoom;
import nif.niobject.NiRoomGroup;
import nif.niobject.NiSwitchNode;
import nif.niobject.NiTextureEffect;
import nif.niobject.NiTriBasedGeom;
import nif.niobject.NiTriShape;
import nif.niobject.NiTriStrips;
import nif.niobject.RootCollisionNode;
import nif.niobject.bs.BSFadeNode;
import nif.niobject.bs.BSLODTriShape;
import nif.niobject.bs.BSLeafAnimNode;
import nif.niobject.bs.BSMasterParticleSystem;
import nif.niobject.bs.BSMultiBoundNode;
import nif.niobject.bs.BSOrderedNode;
import nif.niobject.particle.NiParticleSystem;
import tools3d.utils.scenegraph.Fadable;
import utils.source.TextureSource;

public class J3dNiNode extends J3dNiAVObject implements Fadable
{
	public static boolean warnPresenceBSMasterParticleSystem = true;

	public static boolean warnPresenceNiBSParticleNode = true;

	private ArrayList<Fadable> j3dNiNodes = new ArrayList<Fadable>();

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
				if (child instanceof NiNode)
				{
					if (child instanceof RootCollisionNode)
					{
						continue;//skip these as they are picked up by havok pass later like bhk nodes
					}

					//SPECIAL Accum Node handling!!!!
					// see http://gamebryo32docchs.googlecode.com/svn/trunk/gamebryo3_2_doc_chs/HTML/Convert/Previous/NiAnimation_Conversion.htm
					if (child.name.endsWith(" NonAccum"))
					{
						if (child.name.equals(niNode.name + " NonAccum"))
						{
							// we are an accum node! we take on movements etc of the model
							this.getTransformGroup().setTransform(new Transform3D());
						}
						else
						{
							System.out.println("accum not parent of child!! " + child.name + " " + niNode.nVer.fileName);
						}

					}

					J3dNiNode j3dNiNode = createNiNode((NiNode) child, niToJ3dData, textureSource, onlyNiNodes);
					j3dNiNodes.add(j3dNiNode);
					addChild(j3dNiNode);
				}
				else if (!onlyNiNodes)
				{
					if (child instanceof NiTriBasedGeom)
					{
						NiTriBasedGeom niTriBasedGeom = (NiTriBasedGeom) child;
						J3dNiTriBasedGeom ntbg = null;

						if (niTriBasedGeom instanceof NiTriShape)
						{
							//For now we skip meat caps and morrowind shadows
							if (!(NifToJ3d.HIDE_EDITORS && isEditorMarker(niTriBasedGeom.name))
									&& niTriBasedGeom.name.toLowerCase().indexOf("meat") == -1
									&& niTriBasedGeom.name.toLowerCase().indexOf("cap") == -1
									&& niTriBasedGeom.name.toLowerCase().indexOf("tri shadow") == -1)
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
							if (!(NifToJ3d.HIDE_EDITORS && isEditorMarker(niTriBasedGeom.name)))
							{
								NiTriStrips niTriStrips = (NiTriStrips) niTriBasedGeom;
								ntbg = new J3dNiTriStrips(niTriStrips, niToJ3dData, textureSource);
							}
						}
						j3dNiNodes.add(ntbg);
						addChild(ntbg);
					}
					else if (child instanceof NiParticleSystem)
					{
						J3dNiParticleSystem j3dNiParticleSystem = new J3dNiParticleSystem((NiParticleSystem) child, niToJ3dData,
								textureSource);
						addChild(j3dNiParticleSystem);
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
						J3dNiDirectionalLight j3dNiDirectionalLight = new J3dNiDirectionalLight((NiDirectionalLight) child, niToJ3dData);
						addChild(j3dNiDirectionalLight);
					}
					else if (child instanceof NiTextureEffect)
					{
						//TODO: NiTextureEffect
					}
					else
					{
						System.out.println("J3dNiNode - unhandled child NiAVObject " + child);
					}
				}
			}
		}
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
		{
			//TODO: BSMasterParticleSystem

			if (warnPresenceBSMasterParticleSystem)
			{
				System.out.println("****************************BSMasterParticleSystem");
				warnPresenceBSMasterParticleSystem = false;
			}
			return null;
		}
		else if (niNode instanceof NiBSParticleNode)
		{
			//TODO: NiBSParticleNode  
			if (warnPresenceNiBSParticleNode)
			{
				System.out.println("********************** NiBSParticleNode");
				warnPresenceNiBSParticleNode = false;
			}
			return null;
		}
		else if (niNode instanceof BSLeafAnimNode)
		{
			//nothng new and interesting, drop through
			// this guy should and BSLODLeaf below
		}
		else if (niNode instanceof BSMultiBoundNode)
		{
			//nothng new and interesting, drop through
			// this guy should have switch nodes below for skyrim trees
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

	@Override
	public void fade(float percent)
	{
		for (Fadable f : j3dNiNodes)
		{
			if (f != null)
				f.fade(percent);
		}
	}

	@Override
	public void setOutline(Color3f c)
	{
		for (Fadable f : j3dNiNodes)
		{
			if (f != null)
				f.setOutline(c);
		}
	}

	//NOTE do not use, for bones only, sorry 
	private Transform3D boneCurrentAccumedTrans;

	/** 
	 * do not use, for bones only, sorry
	 * @return
	 */
	public Transform3D getBoneCurrentAccumedTrans()
	{
		if (boneCurrentAccumedTrans == null)
		{
			boneCurrentAccumedTrans = new Transform3D();
			boneCurrentAccumedTrans.setIdentity();
		}
		return boneCurrentAccumedTrans;
	}

}
