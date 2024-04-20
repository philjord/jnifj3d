package nif.shader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.jogamp.java3d.Shader;

import nif.enums.BSLightingShaderType;
import nif.j3d.NiToJ3dData;
import nif.niobject.NiAlphaProperty;
import nif.niobject.NiGeometry;
import nif.niobject.NiObject;
import nif.niobject.NiTexturingProperty;
import nif.niobject.NiTriBasedGeom;
import nif.niobject.NiTriBasedGeomData;
import nif.niobject.NiVertexColorProperty;
import nif.niobject.bs.BSEffectShaderProperty;
import nif.niobject.bs.BSLightingShaderProperty;
import nif.niobject.bs.BSMeshLODTriShape;
import nif.niobject.bs.BSShaderPPLightingProperty;
import nif.niobject.bs.BSSubIndexTriShape;
import nif.niobject.bs.BSTriShape;
import nif.niobject.particle.NiPSysData;

public class ShaderPrograms {

	// set to load debug or alternative shaders form the file system, if present, be cautious
	public static File								fileSystemFolder	= null;

	private static HashMap<String, FileShader>		allFileShaders;
	// programs MUST be checked in order!
	public static LinkedHashMap<String, Program>	programs;

	public static void loadShaderPrograms() {
		if (programs == null) {
			programs = new LinkedHashMap<String, Program>();
			allFileShaders = new HashMap<String, FileShader>();

			// need to load via jar or file system, it depends
			BufferedReader fin = null;
			try {
				if (fileSystemFolder != null && fileSystemFolder.exists()) {
					System.out.println("Using shaders from the file system here: " + fileSystemFolder);
					String source = "ProgramFilesList.txt";
					File f = new File(fileSystemFolder, source);
					if (f.exists())
						fin = new BufferedReader(new FileReader(f));
				}

				if (fin == null) {
					//Note class relative
					String source = "shaders/nif/ProgramFilesList.txt";
					InputStream inputStream = ShaderPrograms.class.getResourceAsStream(source);

					if (inputStream != null) {
						fin = new BufferedReader(new InputStreamReader(inputStream));
					} else {
						fin = new BufferedReader(new FileReader(new File(source)));
					}
				}

				String name = fin.readLine();
				while (name != null) {
					if (!name.trim().startsWith("#") && name.trim().endsWith(".prog")) {
						Program program = new Program(name);
						try {
							program.load(name);
							programs.put(name, program);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}

					name = fin.readLine();
				}

			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					if (fin != null)
						fin.close();
				} catch (IOException e) {
				}
			}

		}
	}

	private static FileShader getShader(String source) {
		FileShader shader = allFileShaders.get(source);

		if (shader == null) {
			try {
				if (source.endsWith(".vert")) {
					shader = new FileShader(source, Shader.SHADER_TYPE_VERTEX);
					shader.load(source);
					allFileShaders.put(source, shader);
				}

				if (source.endsWith(".frag")) {
					shader = new FileShader(source, Shader.SHADER_TYPE_FRAGMENT);
					shader.load(source);
					allFileShaders.put(source, shader);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return shader;

	}

	static class FileShader {
		private GLSLSourceCodeShader	sourceCodeShader;
		public String					name;
		private boolean					status	= false;

		private int						type;

		public FileShader(String n, int t) {
			name = n;
			type = t;
		}

		boolean load(String source) {
			String shaderCode = null;
			if (fileSystemFolder != null && fileSystemFolder.exists()) {
				File f = new File(fileSystemFolder, source);
				if (f.exists())
					shaderCode = ShaderSourceIO.getTextFileAsString(fileSystemFolder + "/" + source);
			}

			if (shaderCode == null) {
				shaderCode = ShaderSourceIO.getTextFileAsString("shaders/nif/" + source);
			}
			/*			ArrayList<String> problems = GLSLSourceCodeShader.testForFFP(shaderCode);
						if (problems.size() > 0)
						{
							System.out.println("Shader file appears to be FFP style " + source);
							for (String problem : problems)
							{
								System.out.println(problem);
							}
						}*/

			sourceCodeShader = new GLSLSourceCodeShader(Shader.SHADING_LANGUAGE_GLSL, type, shaderCode);
			sourceCodeShader.setName(source);
			status = true;

			return true;
		}
	}

	static class Program {
		public GLSLShaderProgram2				shaderProgram	= new GLSLShaderProgram2();
		public ArrayList<GLSLSourceCodeShader>	shaders			= new ArrayList<GLSLSourceCodeShader>();
		private String							name;

		private boolean							status			= false;

		ConditionGroup							conditions		= new ConditionGroup();
		private HashMap<Integer, String>		texcoords		= new HashMap<Integer, String>();

		public Program(String name2) {
			this.name = name2;
			shaderProgram.name = name;

		}

		@Override
		public String toString() {
			return "Program from " + name + " " + status;
		}

		boolean load(String source) throws Exception {

			BufferedReader bfr = null;
			try {

				if (fileSystemFolder != null && fileSystemFolder.exists()) {
					File f = new File(fileSystemFolder, source);
					if (f.exists())
						bfr = new BufferedReader(new FileReader(f));
				}

				if (bfr == null) {
					InputStream inputStream = ShaderPrograms.class.getResourceAsStream("shaders/nif/" + source);
					if (inputStream != null) {
						bfr = new BufferedReader(new InputStreamReader(inputStream));
					} else {
						bfr = new BufferedReader(new FileReader(new File("shaders/nif/" + source)));
					}
				}

				ArrayList<ConditionGroup> chkgrps = new ArrayList<ConditionGroup>();
				chkgrps.add(conditions);

				String line = bfr.readLine();

				while (line != null) {
					line = line.trim().toLowerCase();
					if (line.startsWith("shaders")) {
						String[] list = line.split(" ");

						//skip the word shaders
						for (int i = 1; i < list.length; i++) {
							String s = list [i];

							FileShader shader = ShaderPrograms.getShader(s);

							if (shader != null) {
								if (shader.status)
									shaders.add(shader.sourceCodeShader);
								else
									throw new Exception(source	+ " program depends on shader " + s
														+ " which was not compiled successful");
							} else {
								throw new Exception(source + " program depends on shader " + s + " which is not found");
							}
						}

					} else if (line.startsWith("checkgroup")) {
						String[] list = line.split(" ");

						if (list [1].equals("begin")) {
							ConditionGroup group = new ConditionGroup(list.length >= 3 && list [2].equals("or"));
							chkgrps.get(chkgrps.size() - 1).addCondition(group);
							chkgrps.add(group);
						} else if (list [1].equals("end")) {
							if (chkgrps.size() > 1)
								chkgrps.remove(chkgrps.size() - 1);
							else
								throw new Exception("mismatching checkgroup end tag in " + source);
						} else {
							throw new Exception("expected begin or end after checkgroup in " + source);
						}
					} else if (line.startsWith("check")) {
						line = line.substring("check".length()).trim();

						boolean invert = false;

						if (line.startsWith("not ")) {
							invert = true;
							line = line.substring("not ".length()).trim();
						}

						chkgrps.get(chkgrps.size() - 1).addCondition(new ConditionSingle(line, invert));
					} else if (line.startsWith("texcoords")) {
						line = line.substring("texcoords".length()).trim();
						String[] list = line.split(" ");

						Integer unit = new Integer(list [0]);
						String id = list [1].toLowerCase();

						if (id.length() == 0)
							throw new Exception("malformed texcoord tag in " + source);

						if (!id.equals("tangents") && !id.equals("bitangents") && TexturingPropertygetId(id) < 0)
							throw new Exception("texcoord tag refers to unknown texture id '" + id + "' in " + source);

						if (texcoords.containsKey(unit))
							throw new Exception("texture unit " + unit + " is assigned twiced in " + source);

						texcoords.put(unit, id);
					}

					line = bfr.readLine();
				}
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					if (bfr != null)
						bfr.close();
				} catch (IOException e) {
				}
			}

			//Quick test to ensure texcoords are fixed, recall sk_msn has the last 2 commented out
			if ((texcoords.size() > 0 && !texcoords.get(new Integer(0)).equals("base")) || //
				(texcoords.size() > 1 && !texcoords.get(new Integer(1)).equals("tangents")) || //
				(texcoords.size() > 2 && !texcoords.get(new Integer(2)).equals("bitangents"))) {
				System.err.println("texcords not loaded as expected in file " + source);
			}

			Shader[] shaderArray = shaders.toArray(new Shader[] {});
			shaderProgram.setShaders(shaderArray);

			status = true;
			return true;
		}

		public boolean isStatusOk() {
			return status;
		}

		public String getName() {
			return name;
		}

	}

	static interface Condition {
		boolean eval(NiGeometry niGeometry, NiToJ3dData niToJ3dData, PropertyList props);
	}

	static class ConditionGroup implements Condition {
		private ArrayList<Condition>	conditions	= new ArrayList<Condition>();
		private boolean					_or			= false;

		public ConditionGroup(boolean or) {
			this._or = or;
		}

		public ConditionGroup() {

		}

		@Override
		public boolean eval(NiGeometry niGeometry, NiToJ3dData niToJ3dData, PropertyList props) {
			if (conditions.isEmpty())
				return true;

			if (isOrGroup()) {
				for (Condition cond : conditions) {
					if (cond.eval(niGeometry, niToJ3dData, props))
						return true;
				}
				return false;
			} else {
				for (Condition cond : conditions) {
					if (!cond.eval(niGeometry, niToJ3dData, props))
						return false;
				}
				return true;
			}
		}

		void addCondition(Condition c) {
			conditions.add(c);
		}

		boolean isOrGroup() {
			return _or;
		}
	}

	enum Type {
		EQ(" == "), NE(" != "), LE(" <= "), GE(" >= "), LT(" < "), GT(" > "), AND(" & "), NONE("");

		private final String sign;

		Type(String sign) {
			this.sign = sign;

		}

		private String sign() {
			return sign;
		}
	};

	static class ConditionSingle implements Condition {

		private String	left, right;
		private Type	comp;
		private boolean	invert;

		public ConditionSingle(String line, boolean neg) {
			invert = neg;

			int pos = -1;
			for (Type t : Type.values()) {
				pos = line.indexOf(t.sign());

				if (pos > 0) {
					left = line.substring(0, pos).trim();
					right = line.substring(pos + t.sign().length()).trim();

					if (right.startsWith("\"") && right.endsWith("\""))
						right = right.substring(1, right.length() - 2);

					comp = t;
					break;
				}
			}

			//Note NONE type means test for -1 cannot be used
			if (left == null) {
				left = line;
				comp = Type.NONE;
			}

		}

		@Override
		public boolean eval(NiGeometry niGeometry, NiToJ3dData niToJ3dData, PropertyList props) {
			if (left.equalsIgnoreCase("HEADER/Version")) {
				// note decode as teh input is like "0x14020007"
				return compare(niGeometry.nVer.LOAD_VER, Integer.decode(right).intValue()) ^ invert;
			} else if (left.equalsIgnoreCase("HEADER/User Version")) {
				return compare(niGeometry.nVer.LOAD_USER_VER, Integer.parseInt(right)) ^ invert;
			} else if (left.equalsIgnoreCase("HEADER/User Version 2")) {
				return compare(niGeometry.nVer.BS_Version, Integer.parseInt(right)) ^ invert;
			} else if (left.equalsIgnoreCase("NiAVObject/Vertex Flag 1")) {
				//Possible mistype only BSTriShape has this attribute (I've called it vertexType) FO4 file
				if (niGeometry instanceof BSTriShape) {
					BSTriShape bsTriShape = (BSTriShape)niGeometry;
					return compare(bsTriShape.dwordsPerVertex, Integer.parseInt(right)) ^ invert;
				} else {
					return invert;
				}
			} else if (left.equalsIgnoreCase("NiTriBasedGeom/Has Shader")) {
				if (niGeometry instanceof NiTriBasedGeom) {
					NiTriBasedGeom niTriBasedGeom = (NiTriBasedGeom)niGeometry;
					return compare(niTriBasedGeom.hasShader ? 1 : 0, Integer.parseInt(right)) ^ invert;
				} else {
					return invert;
				}
			} else if (left.equalsIgnoreCase("NiTriBasedGeomData/Has Vertices")) {
				NiObject data = niToJ3dData.get(niGeometry.data);
				if (data instanceof NiTriBasedGeomData) {
					NiTriBasedGeomData ntbgd = (NiTriBasedGeomData)data;
					return compare(ntbgd.hasVertices ? 1 : 0, Integer.parseInt(right)) ^ invert;
				} else if (data instanceof NiPSysData) {
					NiPSysData npsd = (NiPSysData)data;
					return compare(npsd.hasVertices ? 1 : 0, Integer.parseInt(right)) ^ invert;
				} else {
					return invert;
				}
			} else if (left.equalsIgnoreCase("NiTriBasedGeomData/Has Normals")) {
				NiObject data = niToJ3dData.get(niGeometry.data);
				if (data instanceof NiTriBasedGeomData) {
					NiTriBasedGeomData ntbgd = (NiTriBasedGeomData)data;
					return compare(ntbgd.hasNormals ? 1 : 0, Integer.parseInt(right)) ^ invert;
				} else {
					return invert;
				}
			} else if (left.equalsIgnoreCase("NiTriBasedGeomData/Has Vertex Colors")) {
				NiObject data = niToJ3dData.get(niGeometry.data);
				if (data instanceof NiTriBasedGeomData) {
					NiTriBasedGeomData ntbgd = (NiTriBasedGeomData)data;
					return compare(ntbgd.hasVertexColors ? 1 : 0, Integer.parseInt(right)) ^ invert;
				} else {
					return invert;
				}
			} else if (left.equalsIgnoreCase("BSTriShape")) {
				return (niGeometry instanceof BSTriShape) ^ invert;
			} else if (left.equalsIgnoreCase("BSSubIndexTriShape")) {
				return (niGeometry instanceof BSSubIndexTriShape) ^ invert;
			} else if (left.equalsIgnoreCase("BSMeshLODTriShape")) {
				return (niGeometry instanceof BSMeshLODTriShape) ^ invert;
			} else if (left.equalsIgnoreCase("NiAlphaProperty")) {
				return (props.get(NiAlphaProperty.class) != null) ^ invert;
			} else if (left.equalsIgnoreCase("BSEffectShaderProperty")) {
				return (props.get(BSEffectShaderProperty.class) != null) ^ invert;
			} else if (left.equalsIgnoreCase("BSLightingShaderProperty")) {
				return (props.getBSLightingShaderProperty() != null) ^ invert;
			} else if (left.equalsIgnoreCase("BSLightingShaderProperty/Skyrim Shader Type")) {
				BSLightingShaderProperty bslsp = props.getBSLightingShaderProperty();
				if (bslsp == null)
					return invert;
				else
					return compare(bslsp.ShaderType.getType(), Integer.parseInt(right)) ^ invert;
			} else if (left.equalsIgnoreCase("NiTexturingProperty/Apply Mode")) {
				NiTexturingProperty p = (NiTexturingProperty)props.get(NiTexturingProperty.class);
				if (p != null) {
					return compare(p.applyMode.applyMode, Integer.parseInt(right)) ^ invert;
				} else {
					return invert;
				}

			} else if (left.equalsIgnoreCase("NiVertexColorProperty")) {
				return (props.get(NiVertexColorProperty.class) != null) ^ invert;
			} else if (left.equalsIgnoreCase("NiVertexColorProperty/Vertex Mode")) {
				NiVertexColorProperty p = (NiVertexColorProperty)props.get(NiVertexColorProperty.class);
				if (p == null)
					return invert;
				else
					return compare(p.vertexMode.mode, Integer.parseInt(right)) ^ invert;
			} else if (left.equalsIgnoreCase("NiVertexColorProperty/Lighting Mode")) {
				NiVertexColorProperty p = (NiVertexColorProperty)props.get(NiVertexColorProperty.class);
				if (p == null)
					return invert;
				else
					return compare(p.lightingMode.mode, Integer.parseInt(right)) ^ invert;
			} else if (left.equalsIgnoreCase("BSShaderPPLightingProperty")) {
				return (props.get(BSShaderPPLightingProperty.class) != null) ^ invert;
			} else if (left.equalsIgnoreCase("BSEffectShaderProperty")) {
				return (props.get(BSEffectShaderProperty.class) != null) ^ invert;
			}
			new Throwable("Unknown prog condition " + left + "/" + right + " " + (invert ? "not " : ""));
			if (comp == Type.NONE)
				return !invert;

			return false;
		}

		boolean compare(int a, int b) {
			switch (comp) {
				case EQ:
					return a == b;
				case NE:
					return a != b;
				case LE:
					return a <= b;
				case GE:
					return a >= b;
				case LT:
					return a < b;
				case GT:
					return a > b;
				default:
					return true;
			}
		}

		boolean compare(float a, float b) {
			switch (comp) {
				case EQ:
					return a == b;
				case NE:
					return a != b;
				case LE:
					return a <= b;
				case GE:
					return a >= b;
				case LT:
					return a < b;
				case GT:
					return a > b;
				default:
					return true;
			}
		}

		boolean compare(String a, String b) {
			switch (comp) {
				case EQ:
					return a == b;
				case NE:
					return a != b;
				default:
					return false;
			}
		}

	}

	enum tpnames {
		base, dark, detail, gloss, glow, bumpmap, decal0, decal1, decal2, decal3
	};

	static int TexturingPropertygetId(String texname) {
		return tpnames.valueOf(texname).ordinal();
	}

	enum bsslpnames {
		base, dark, detail, gloss, glow, bumpmap, decal0, decal1
	};

	static int BSShaderLightingPropertygetId(String id) {
		return bsslpnames.valueOf(id).ordinal();
	}
}
