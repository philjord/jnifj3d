package nif.character;

import java.util.HashMap;

public class AttachedParts
{
	public HashMap<Part, String> parts = new HashMap<Part, String>();

	public AttachedParts()
	{

	}

	/**
	 * Returns true if a part was swapped out by this addition
	 */
	public boolean addPart(Part part, String nifFileName)
	{
		boolean ret = parts.get(part) != null;
		parts.put(part, nifFileName);
		return ret;
	}

	public boolean removePart(Part part)
	{
		boolean ret = parts.get(part) != null;
		parts.remove(part);
		return ret;
	}

	public boolean hasPart(Part part)
	{
		return parts.get(part) != null;
	}

	public static boolean isLeftSide(int loc)
	{
		return loc == Part.Left_Hand.loc || //
				loc == Part.Left_Wrist.loc || //
				loc == Part.Left_Forearm.loc || //
				loc == Part.Left_Upper_Arm.loc || //
				loc == Part.Left_Foot.loc || //
				loc == Part.Left_Ankle.loc || //
				loc == Part.Left_Knee.loc || //
				loc == Part.Left_Upper_Leg.loc || //
				loc == Part.Left_Clavicle.loc;//
	}

	public static boolean hasFirstPersonModel(Part part)
	{
		return part == Part.Left_Hand || //
				part == Part.Right_Hand; //
	}

	public static boolean isFirstPersonVisible(Part part)
	{
		return part == Part.Left_Hand || //
				part == Part.Left_Wrist || //
				part == Part.Left_Forearm || //
				part == Part.Left_Upper_Arm || //
				part == Part.Right_Hand || //
				part == Part.Right_Wrist || //
				part == Part.Right_Forearm || //
				part == Part.Right_Upper_Arm || //
				part == Part.Shield || //
				part == Part.Weapon; //

	}

	public static boolean isSkinned(int loc, boolean includeFeet)
	{
		return loc == Part.Chest.loc || //
				loc == Part.Skirt.loc || //
				loc == Part.Right_Hand.loc || //
				loc == Part.Left_Hand.loc || //
				(includeFeet && loc == Part.Right_Foot.loc) || //
				(includeFeet && loc == Part.Left_Foot.loc);//
	}

	public static Part getPartForLoc(int loc)
	{
		// notice relies on index being loc id which could be wrong
		return Part.values()[loc];
	}

	public enum Part
	{

		Head(0, "Head"), //
		Hair(1, "Head"), //or helmet I assume
		Neck(2, "Neck"), //
		Chest(3, "Chest"), //Skinned Cuirass
		Groin(4, "Groin"), //?? not skinned 
		Skirt(5, "Skirt"), //Skinned
		Right_Hand(6, "Right Hand"), //Skinned
		Left_Hand(7, "Left Hand"), //Skinned
		Right_Wrist(8, "Right Wrist"), //
		Left_Wrist(9, "Left Wrist"), //
		Shield(10, "Shield"), //
		Right_Forearm(11, "Right Forearm"), //
		Left_Forearm(12, "Left Forearm"), //
		Right_Upper_Arm(13, "Right Upper Arm"), //
		Left_Upper_Arm(14, "Left Upper Arm"), //
		Right_Foot(15, "Right Foot"), //
		Left_Foot(16, "Left Foot"), //
		Right_Ankle(17, "Right Ankle"), //
		Left_Ankle(18, "Left Ankle"), //
		Right_Knee(19, "Right Knee"), //
		Left_Knee(20, "Left Knee"), //
		Right_Upper_Leg(21, "Right Upper Leg"), //
		Left_Upper_Leg(22, "Left Upper Leg"), //
		Right_Clavicle(23, "Right Clavicle"), //"Right Pauldron"), 
		Left_Clavicle(24, "Left Clavicle"), //"Left Pauldron"), 
		Weapon(25, "Weapon"), //
		Tail(26, "Tail"), //
		Root(27, "Root Bone");// made up for CREA to use

		private Part(int loc, String node)
		{
			this.loc = loc;
			this.node = node;
		}

		private final int loc;
		private final String node;

		public int getLoc()
		{
			return loc;
		}

		public String getNode()
		{
			return node;
		}

	}
}
