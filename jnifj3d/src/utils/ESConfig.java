package utils;

public class ESConfig
{
	//NOTE fallout modular dungeon sections are 2.56 meters or 128 units each (size of a human?)

	// NOTE es x,y,z to j3d x,z,-y

	// 1/0.02 = 50
	// TES units are in fact definitively one half inch per unit!
	public static float ES_TO_METERS_SCALE = 0.0254f / 2f; //0.0127

	public static String TES_TEXTURE_PATH = "Textures\\";

	public static String TES_MESH_PATH = "Meshes\\";

	public static String TES_SOUND_PATH = "Sound\\";
	
	//FO4 onwards
	public static String TES_MATERIALS_PATH = "Materials\\";

}
