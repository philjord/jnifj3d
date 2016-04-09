package utils.optimize;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.media.j3d.Transform3D;
import javax.vecmath.Point3f;

import nif.NifFile;
import nif.NifVer;
import nif.basic.NifRef;
import nif.character.NifJ3dSkeletonRoot;
import nif.j3d.NiToJ3dData;
import nif.niobject.NiAVObject;
import nif.niobject.NiAlphaProperty;
import nif.niobject.NiMaterialProperty;
import nif.niobject.NiNode;
import nif.niobject.NiObject;
import nif.niobject.NiProperty;
import nif.niobject.NiTexturingProperty;
import nif.niobject.NiTriShape;
import nif.niobject.NiTriShapeData;
import nif.niobject.NiVertexColorProperty;
import nif.niobject.RootCollisionNode;
import utils.convert.ConvertFromNif;
import utils.optimize.OptimizeState.MergeTriShape;

// so it looks like most nif files are nicely optimized, I can't see any others with the morrowind 
// transparent tri sorting condition.

// but I'll run the CompState output and see what was getting the multi shape compiles and why in case.

//then this class will only look for alpha'ed trishapes and put them together with all the restrictions below

// I believe I possibly saw a physics that was split up into many pieces,is there any value in
// trying to merge them?

// we side note I see the banner have waving animations in them, why not running?

//okokok

//Fist off forget about sharing anything! forget it! transparency for fadin denies it totally
// I can re-use geometry data, which I do now (I hope) for non morphables and I can re-use
// animation data which I hope I do now too.

// I need to traverse NiNodes and NiTriShape/NiTriStrips/BSTriShape

// gather transforms and appearances

// notice that compacting transforms that are used once below a niNode is not in fact useful

// check for controllers or any other "stoppers" skin, particles, ...

// any chain that has no geom node is to be ignored

// Any NiNode that is a bone is to be ignored (bone defined in J3dNiNode, like Bip01 etc)

// let's just start with morrowind, then examine each game in turn

// Don't touch NiTrishape with a non null SkinInstance

// Don't touch NiTriShape with any non null controller( including NiGeomMorphController, NiKeyFrameController )

// the appearance is made up of properties of NiTriShape
// The various items of appearance can have controllers pointing at them, if so do not merge at all

// things below attachment node can still be nicely merged (eg a\towershield_glass.nif)
// and attachment nodes are the root??

// Notice NiTextureEffect appear to want to affect the following node, so can be considered part of 
// appearance equivalence (maybe?) but will in fact be the previous node so tricky to find

// NiVertexColorProperty is similar appearing before the TriShape see (c/c_ring_common05.nif)
// or because it appears in the Properties of the parent, maybe it applies to that scope? (not like NiTextureEffect)
// I see that it appears in the list of properties of shapes so just ignore if it happens to be a child else where
// possibly that an old lighting hint having it early for emissive or something

// Don't touch or change a root node (except possibly to extract the transform?)

// for human attachments possibly don't touch things with known part names e.g B_N_Redguard_F_Foot
// though would I in fact do anything with these? I'm never going to toucha root node

// For first pass don't touch physics node and below, e.g. RootCollisionNode
// For first pass don't touch particles node and below, e.g. NiBSParticleNode
// For first pass don't touch billboards node, e.g. NiBillBoardNode, but do compile children

// NiStringExtraData "sgoKeep = " should be looked for and respected just in case

// I notice an aweful lot of them are precompiled together, in fact I notice that mainly transparent
// shapes are separated out, so that ordering works! bugger! bugger! bugger!

// As I go down each ninode path, I should keep an upwards path of node pointers (linked list)
// and put all shapes that are static into a list
// afterward go through list and compare each by type and appearance equivalence (equality) for any 2 or more that can be merged do
// get each ones Xtransform and pre multiply the coords, then put all the coords and triangles and normals etc 
// into a big fat byte buffers and int [] and then 
// stick it into the first, then attach the first shape to the non compactable parent and snip off all other
// trishapes (but leave ninodes in case something else need them)

//Well well well... transparency sorting, we meet at last...

// in order to smash these things together I need to sort the triangles myself every frame
// no I need to re-traverse the sorted transparent geoms and put them back into the byte buffers
// in their new order for the current frame (possibly for each second frame or some bullshit like that)
// mark the byte buffer as changing, of course it would need to be the index array, which 
// would have to go across as an IntegerBuffer in order to be updated by the sub call
// it's not terrible, keep all the trishapes in the tree, and just before render time, after transparent sort
// make a call to take the indexes and resort them in the mega shape

// I can in fact merge them if I can make them pure alpha sorting I notice, so perhaps? Oblivion has them merged

///OKOKOKOKOK!!!  more info!!!!11!
// in all of morrowind only one! file has a alpha flags >= 512 (512 is bit 9 alpha testing enable)
// so I suspect alpha testing was not a thing back in 2000 alpha test is in opengl1.1 doc 1997
// alpha flags.flags = 4097 f:\game_media\morrowind\meshes\e\magic_cast_frost.nif = bit 0 and bit 12
// appears to be cock up

// so if I swap for bit 9 and 12 (alpha test normal) = 4608
// normal bland and alpha test = 4845 regular!

// flags seen
// 237 (the blendy bit above)
// lights seems to like 13 (blend on src =alpha dest = 1

// I wager sized based distance fading will help a lot with frame rate

// once I merge them must set the alpha value to 4608 and 128

// I notice the argonian f hair 02 is transparent and looks much better with just alpha testing turned on

// also my tree now does crazy shit if I set alpha test only! with threshold 0, like java3d compilation is
// suddenly active and smashing them together!

public class NifFileOptimizer
{
	private NifFile nifFile;

	public NifFileOptimizer(NifFile nifFile)
	{
		this.nifFile = nifFile;
		//System.out.println("Processing file " + nifFile.header.nifVer.fileName);

	}

	public void optimize()
	{
		NiObject root = nifFile.blocks.root();

		//Optimize Tes3 transparent shapes into a single shape
		if (root.nVer.LOAD_VER <= NifVer.VER_10_0_1_0 && root instanceof NiNode)
		{
			NiToJ3dData niToJ3dData = new NiToJ3dData(nifFile.blocks);
			OptimizeState optimizeState = new OptimizeState();
			searchTes3TransparentTriShape((NiNode) root, niToJ3dData, optimizeState);
			mergeTes3TransparentTriShape(optimizeState, niToJ3dData);
		}
	}

	private static void mergeTes3TransparentTriShape(OptimizeState optimizeState, NiToJ3dData niToJ3dData)
	{
		for (int i = 0; i < optimizeState.shapesToMerge.size(); i++)
		{
			MergeTriShape mergeTriShape = optimizeState.shapesToMerge.get(i);
			//compress him and attach to the it's root node
			compress(mergeTriShape, niToJ3dData);
		}

		// now for each other 
		// if the same appearance
		// compress and add into first trishape
		while (optimizeState.shapesToMerge.size() > 0)
		{
			// pull the first one out
			MergeTriShape firstTriShape = optimizeState.shapesToMerge.remove(0);
			move(firstTriShape, niToJ3dData);

			// for all the rest compare
			for (int i = 0; i < optimizeState.shapesToMerge.size(); i++)
			{
				MergeTriShape mergeTriShape = optimizeState.shapesToMerge.get(i);

				// do they share a root node?
				if (firstTriShape.path.get(0) == mergeTriShape.path.get(0))
				{
					// are they identical appearances
					if (equivilentAppearace(firstTriShape, mergeTriShape, niToJ3dData)
							&& equivilentShapeData(firstTriShape, mergeTriShape, niToJ3dData))
					{
						// merge them!
						mergeShapes(firstTriShape, mergeTriShape, niToJ3dData);
						discard(mergeTriShape, niToJ3dData);
						optimizeState.shapesToMerge.remove(i);
						i--;
					}
				}

			}
		}

	}

	private static boolean equivilentShapeData(MergeTriShape firstMerge, MergeTriShape mergeMerge, NiToJ3dData niToJ3dData)
	{

		NiTriShapeData firstTriShape = (NiTriShapeData) niToJ3dData.get(firstMerge.niTriShape.data);
		NiTriShapeData mergeTriShape = (NiTriShapeData) niToJ3dData.get(mergeMerge.niTriShape.data);

		return firstTriShape.hasNormals == mergeTriShape.hasNormals && firstTriShape.hasVertexColors == mergeTriShape.hasVertexColors
				&& firstTriShape.actNumUVSets == mergeTriShape.actNumUVSets && firstTriShape.hasTriangles == mergeTriShape.hasTriangles;
	}

	/**
	 * merge the mergeTriShape into the firstTriShape buffers
	 * @param firstTriShape
	 * @param mergeTriShape
	 */
	private static void mergeShapes(MergeTriShape firstMerge, MergeTriShape mergeMerge, NiToJ3dData niToJ3dData)
	{
		NiTriShapeData firstTriShape = (NiTriShapeData) niToJ3dData.get(firstMerge.niTriShape.data);
		NiTriShapeData mergeTriShape = (NiTriShapeData) niToJ3dData.get(mergeMerge.niTriShape.data);

		// create a new set of buffer and fill them up with the original data
		// then fill them up with the new data!

		//numVertices
		int newNumVertices = firstTriShape.numVertices + mergeTriShape.numVertices;

		//verts
		FloatBuffer newVerticesOptBuf = createFB(newNumVertices * 3);
		for (int i = 0; i < firstTriShape.numVertices * 3; i++)
		{
			newVerticesOptBuf.put(i, firstTriShape.verticesOptBuf.get(i));
		}
		for (int i = 0; i < mergeTriShape.numVertices * 3; i++)
		{
			newVerticesOptBuf.put(firstTriShape.numVertices * 3 + i, mergeTriShape.verticesOptBuf.get(i));
		}
		firstTriShape.verticesOptBuf = newVerticesOptBuf;

		if (firstTriShape.hasNormals)
		{
			// normals
			FloatBuffer newNormalsOptBuf = createFB(newNumVertices * 3);
			for (int i = 0; i < firstTriShape.numVertices * 3; i++)
			{
				newNormalsOptBuf.put(i, firstTriShape.normalsOptBuf.get(i));
			}
			for (int i = 0; i < mergeTriShape.numVertices * 3; i++)
			{
				newNormalsOptBuf.put(firstTriShape.numVertices * 3 + i, mergeTriShape.normalsOptBuf.get(i));
			}
			firstTriShape.normalsOptBuf = newNormalsOptBuf;
		}
		// vertexcolors
		if (firstTriShape.hasVertexColors)
		{
			// normals
			FloatBuffer newVertexColorsOptBuf = createFB(newNumVertices * 4);
			for (int i = 0; i < firstTriShape.numVertices * 4; i++)
			{
				newVertexColorsOptBuf.put(i, firstTriShape.vertexColorsOptBuf.get(i));
			}
			for (int i = 0; i < mergeTriShape.numVertices * 4; i++)
			{
				newVertexColorsOptBuf.put(firstTriShape.numVertices * 4 + i, mergeTriShape.vertexColorsOptBuf.get(i));
			}
			firstTriShape.vertexColorsOptBuf = newVertexColorsOptBuf;
		}

		// uvsets
		FloatBuffer[] newUVSetsOptBuf = new FloatBuffer[firstTriShape.actNumUVSets];
		for (int j = 0; j < firstTriShape.actNumUVSets; j++)
		{
			newUVSetsOptBuf[j] = createFB(newNumVertices * 2);
			for (int i = 0; i < firstTriShape.numVertices * 2; i++)
			{
				newUVSetsOptBuf[j].put(i, firstTriShape.uVSetsOptBuf[j].get(i));
			}
			for (int i = 0; i < mergeTriShape.numVertices * 2; i++)
			{
				newUVSetsOptBuf[j].put(firstTriShape.numVertices * 2 + i, mergeTriShape.uVSetsOptBuf[j].get(i));
			}
		}
		firstTriShape.uVSetsOptBuf = newUVSetsOptBuf;
		if (firstTriShape.hasTriangles)
		{
			// numTriPoints
			int newNumTrianglePoints = firstTriShape.numTrianglePoints + mergeTriShape.numTrianglePoints;
			firstTriShape.numTrianglePoints = newNumTrianglePoints;

			// numTris
			int newNumTriangles = firstTriShape.numTriangles + mergeTriShape.numTriangles;

			int[] newTrianglesOpt = new int[newNumTriangles * 3];
			for (int i = 0; i < firstTriShape.numTriangles * 3; i++)
			{
				newTrianglesOpt[i] = firstTriShape.trianglesOpt[i];

			}
			// triangle must be offset for the new data
			for (int i = 0; i < mergeTriShape.numTriangles * 3; i++)
			{
				newTrianglesOpt[firstTriShape.numTriangles * 3 + i] = mergeTriShape.trianglesOpt[i] + firstTriShape.numVertices;
			}

			firstTriShape.numTriangles = newNumTriangles;
			firstTriShape.trianglesOpt = newTrianglesOpt;
		}

		//finally update the vert count
		firstTriShape.numVertices = newNumVertices;

		// TODO: for num matchgroups has to be 0 test this

		// TODO: finish by setting the center and radius too? find the 2 most extreme radius points and ...

	}

	private static boolean equivilentAppearace(MergeTriShape firstTriShape, MergeTriShape mergeTriShape, NiToJ3dData niToJ3dData)
	{
		if (firstTriShape.niTriShape.numProperties != mergeTriShape.niTriShape.numProperties)
			return false;

		for (int i = 0; i < firstTriShape.niTriShape.numProperties; i++)
		{
			NiProperty firstNiProperty = (NiProperty) niToJ3dData.get(firstTriShape.niTriShape.properties[i]);
			NiProperty mergeNiProperty = (NiProperty) niToJ3dData.get(mergeTriShape.niTriShape.properties[i]);

			if (firstNiProperty.getClass() != mergeNiProperty.getClass())
				return false;

			//TODO: all of the below should be equals methods in their respective classes

			if (firstNiProperty instanceof NiAlphaProperty)
			{
				NiAlphaProperty firstNap = (NiAlphaProperty) firstNiProperty;
				NiAlphaProperty mergeNap = (NiAlphaProperty) mergeNiProperty;

				if (firstNap.flags.flags != mergeNap.flags.flags || //
						firstNap.threshold != mergeNap.threshold)
					return false;
			}
			else if (firstNiProperty instanceof NiTexturingProperty)
			{
				NiTexturingProperty firstNtp = (NiTexturingProperty) firstNiProperty;
				NiTexturingProperty mergeNtp = (NiTexturingProperty) mergeNiProperty;

				if (firstNtp.hasBaseTexture != true || mergeNtp.hasBaseTexture != true
						|| firstNtp.baseTexture.source.ref != mergeNtp.baseTexture.source.ref)
					return false;
			}
			else if (firstNiProperty instanceof NiMaterialProperty)
			{
				NiMaterialProperty firstNmp = (NiMaterialProperty) firstNiProperty;
				NiMaterialProperty mergeNmp = (NiMaterialProperty) mergeNiProperty;

				if (firstNmp.flags.flags != mergeNmp.flags.flags || //
						!firstNmp.ambientColor.equals(mergeNmp.ambientColor) || //
						!firstNmp.diffuseColor.equals(mergeNmp.diffuseColor) || //
						!firstNmp.specularColor.equals(mergeNmp.specularColor) || //
						!firstNmp.emissiveColor.equals(mergeNmp.emissiveColor) || //
						firstNmp.glossiness != mergeNmp.glossiness || //
						firstNmp.alpha != mergeNmp.alpha)
					return false;
			}

			else if (firstNiProperty instanceof NiVertexColorProperty)
			{
				NiVertexColorProperty firstNvcp = (NiVertexColorProperty) firstNiProperty;
				NiVertexColorProperty mergeNvcp = (NiVertexColorProperty) mergeNiProperty;
				if (firstNvcp.flags.flags != mergeNvcp.flags.flags || firstNvcp.lightingMode.mode != mergeNvcp.lightingMode.mode
						|| firstNvcp.vertexMode.mode != mergeNvcp.vertexMode.mode)
					return false;
			}
			else
			{
				// no other properies allowed for now
				return false;
			}

		}
		return true;
	}

	private static void discard(MergeTriShape mergeTriShape, NiToJ3dData niToJ3dData)
	{
		NiNode parentNode = mergeTriShape.path.get(mergeTriShape.path.size() - 1);

		//System.out.println("Moving from " + parentNode + " to " + rootNode);
		NiTriShape niTriShape = mergeTriShape.niTriShape;
		for (int i = 0; i < parentNode.numChildren; i++)
		{
			if (niToJ3dData.get(parentNode.children[i]) == niTriShape)
			{
				parentNode.children[i] = null;
				// don't touch numChildren must still traverse list
				return;
			}
		}
	}

	/**
	 * Moves the shape from it's current parents child list to the first node in the paths childrens list
	 * @param mergeTriShape
	 */
	private static void move(MergeTriShape mergeTriShape, NiToJ3dData niToJ3dData)
	{
		NiNode rootNode = mergeTriShape.path.get(0);
		NiNode parentNode = mergeTriShape.path.get(mergeTriShape.path.size() - 1);

		//System.out.println("Moving from " + parentNode + " to " + rootNode);
		NiTriShape niTriShape = mergeTriShape.niTriShape;
		for (int i = 0; i < parentNode.numChildren; i++)
		{
			if (niToJ3dData.get(parentNode.children[i]) == niTriShape)
			{
				NifRef[] rootChildren = rootNode.children;
				NifRef[] newRootChildren = new NifRef[rootChildren.length + 1];
				System.arraycopy(rootChildren, 0, newRootChildren, 0, rootChildren.length);
				newRootChildren[newRootChildren.length - 1] = parentNode.children[i];

				parentNode.children[i] = null;
				// don't touch numChildren must still traverse list

				rootNode.children = newRootChildren;
				rootNode.numChildren = newRootChildren.length;
				return;
			}
		}

	}

	private static void compress(MergeTriShape mergeTriShape, NiToJ3dData niToJ3dData)
	{
		// go from root to shape downwards

		// notice start at 1 mean first path item is not used (the root)
		Transform3D currentTrans = new Transform3D();
		Transform3D tempTrans = new Transform3D();
		for (int i = 1; i < mergeTriShape.path.size(); i++)
		{
			NiNode niNode = mergeTriShape.path.get(i);
			//NOTE the opt buffer are already in java3d format, so the transform must be as well (thank god)
			tempTrans.setRotation(ConvertFromNif.toJ3d(niNode.rotation));
			tempTrans.setTranslation(ConvertFromNif.toJ3d(niNode.translation));
			tempTrans.setScale(niNode.scale);
			currentTrans.mul(tempTrans);
			tempTrans.setIdentity();
		}

		NiTriShape niTriShape = mergeTriShape.niTriShape;
		tempTrans.setRotation(ConvertFromNif.toJ3d(niTriShape.rotation));
		tempTrans.setTranslation(ConvertFromNif.toJ3d(niTriShape.translation));
		tempTrans.setScale(niTriShape.scale);
		currentTrans.mul(tempTrans);

		// now to apply the change to the niTriShape, just verts, normals are already in object space
		NiTriShapeData niTriShapeData = (NiTriShapeData) niToJ3dData.get(niTriShape.data);
		if (niTriShapeData != null)
		{
			Point3f c = new Point3f();
			for (int i = 0; i < niTriShapeData.numVertices; i++)
			{
				c.set(niTriShapeData.verticesOptBuf.get(i * 3 + 0), niTriShapeData.verticesOptBuf.get(i * 3 + 1),
						niTriShapeData.verticesOptBuf.get(i * 3 + 2));
				currentTrans.transform(c);
				niTriShapeData.verticesOptBuf.put(i * 3 + 0, c.x);
				niTriShapeData.verticesOptBuf.put(i * 3 + 1, c.y);
				niTriShapeData.verticesOptBuf.put(i * 3 + 2, c.z);
			}
		}

		// now blank out the niTriShapes transform 
		niTriShape.rotation.m11 = niTriShape.rotation.m22 = niTriShape.rotation.m33 = 1f;
		niTriShape.rotation.m12 = niTriShape.rotation.m21 = niTriShape.rotation.m32 = 0f;
		niTriShape.rotation.m13 = niTriShape.rotation.m23 = niTriShape.rotation.m33 = 0f;
		niTriShape.translation.x = 0;
		niTriShape.translation.y = 0;
		niTriShape.translation.z = 0;
		niTriShape.scale = 1;

	}

	public void searchTes3TransparentTriShape(NiAVObject niAVObject, NiToJ3dData niToJ3dData, OptimizeState optimizeState)
	{
		if (niAVObject != null)
		{
			// don't process any bones at all, by ignoring the root bone
			// also completely ignore the physics branch
			if (!NifJ3dSkeletonRoot.isRootBoneName(niAVObject.name) && !(niAVObject instanceof RootCollisionNode))
			{
				if (niAVObject instanceof NiNode)
				{
					NiNode niNode = (NiNode) niAVObject;
					optimizeState.currentPath.add(niNode);
					for (int i = 0; i < niNode.numChildren; i++)
					{
						NiAVObject child = (NiAVObject) niToJ3dData.get(niNode.children[i]);
						if (child != null)
						{
							// restart path if an ninode with either a controller or not a simple NiNode
							if (!(child instanceof NiNode) || (child.controller.ref == -1 && child.getClass().equals(NiNode.class)))
							{
								//System.out.print("child " + child);
								searchTes3TransparentTriShape(child, niToJ3dData, optimizeState);
							}
							else
							{
								//System.out.println("I want to restart the path now as I've hit a controlled or non ninode node");
								OptimizeState optimizeState2 = new OptimizeState();
								searchTes3TransparentTriShape(child, niToJ3dData, optimizeState2);
								mergeTes3TransparentTriShape(optimizeState2, niToJ3dData);
							}
						}
					}
					//System.out.println("");

					optimizeState.currentPath.remove(optimizeState.currentPath.size() - 1);
				}
				else if (niAVObject instanceof NiTriShape)
				{

					NiTriShape niTriShape = (NiTriShape) niAVObject;
					// skins and controlled are never touched
					if (niTriShape.skin.ref == -1 && niTriShape.controller.ref == -1)
					{
						// is it a transparent bad boy, with no extra props
						NifRef[] properties = niTriShape.properties;
						boolean hasMergableAlpha = false;
						boolean hasTexture = false;
						boolean hasMaterial = false;
						boolean hasOther = false;
						for (int p = 0; p < properties.length; p++)
						{
							NiObject prop = niToJ3dData.get(properties[p]);
							if (prop != null)
							{
								if (prop instanceof NiProperty)
								{
									NiProperty property = (NiProperty) prop;

									if (property.controller.ref == -1)
									{
										if (property instanceof NiAlphaProperty)
										{
											//FOr now do both 237 and 13 which are in fact going to be  4608 + 237; 
											//4608 + 13; 
											//NiAlphaProperty nap = (NiAlphaProperty) property;
											//if (nap.flags.flags == 237)
											hasMergableAlpha = true;
										}
										else if (property instanceof NiMaterialProperty)
										{
											hasMaterial = true;
										}
										else if (property instanceof NiTexturingProperty)
										{
											NiTexturingProperty ntp = (NiTexturingProperty) property;
											if (ntp.hasBaseTexture && ntp.baseTexture.source.ref != -1)
												hasTexture = true;
										}
										else if (property instanceof NiVertexColorProperty)
										{
											//TODO: is it ok to leave these alone and merge anyway?
										}
										else
										{
											System.out.println("other prop seen in optimizer " + property);
										}
									}
								}
							}
						}

						if (hasMergableAlpha && hasTexture && hasMaterial && !hasOther)
						{
							optimizeState.addShape(niTriShape);
						}
					}
				}
			}

		}

	}

	protected static FloatBuffer createFB(int l)
	{
		ByteBuffer bb = ByteBuffer.allocateDirect(l * 4);
		bb.order(ByteOrder.nativeOrder());
		return bb.asFloatBuffer();
	}

}
