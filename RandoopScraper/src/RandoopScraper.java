import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.nodeTypes.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.util.*;

/**
 * Main class of the Randoop Scraper/Scanner.  This class
 * performs the collection of source class filenames, locating
 * of literal values within each class in that collection and
 * creation of the Randoop literals output file.  The scraping
 * scan is performed using the Javaparser library {@link http://javaparser.org}
 * The resulting output file created is compatible with the Randoop tool
 * {@link http://github.com/randoop/randoop} and should be included
 * in the analysis using the --literals-file= command line flag of Randoop.
 * @author Wayne Motycka
 * @version 1.0
 * @date November 2017
 */
public class RandoopScraper {
    /**
     * constants defining the standard test values used
     * by randoop in forming tests.  These should be avoided
     * since adding more of these will not enhance the
     * value set used.
     */
	/** byte literal defaults of Randoop */
	static final byte[] bytevals = {-1, 0, 1, 10, 100};
	/** short literal defaults of Randoop */
	static final short[] shortvals = {-1, 0, 1, 10, 100};
	/** int literal defaults of Randoop */
	static final int[] intvals = {-1, 0, 1, 10, 100};
	/** long literal defaults of Randoop */
	static final long[] longvals = { -1, 0, 1, 10, 100};
	/** float literal defaults of Randoop */
	static final float[] floatvals = { -1, 0, 1, 10, 100};
	/** double literal defaults of Randoop */
	static final double[] doublevals =  {-1, 0, 1, 10, 100};
	/** char literal defaults of Randoop */
	static final char[] charvals = { '#', ' ', '4', 'a'};
	/** String literal defaults of Randoop */
	static final java.lang.String[] stringvals = { "", "hi!"};
	// none of these are currently used.
	protected ArrayList<String> intNames;
	protected ArrayList<String> byteNames;
	protected ArrayList<String> shortNames;
	protected ArrayList<String> longNames;
	protected ArrayList<String> floatNames;
	protected ArrayList<String> doubleNames;
	protected ArrayList<String> charNames;
	protected ArrayList<String> stringNames;
	protected Stack<String> filelist;
	volatile String currentClass;
	volatile String currentPkg;

	/**
	 * Container for storing the literal values by type for each
	 * Java source file.  Key is the class name, value is a LiteralMap object.
	 */
	HashMap<String, LiteralMap> literalslist; // maps class to data-types/values map

	/**
	 * Create a new RandoopScraper object to perform the literal values
	 * collecting operation on the filename or path specified on the
	 * command line.
	 */
    public RandoopScraper() {
		filelist = new Stack<String>();
		initVarNameLists();
		currentClass = "";
		currentPkg = "";
		literalslist = new HashMap<String, LiteralMap>();
	}
   
    public void initVarNameLists() {
    	intNames = new ArrayList<>();
		byteNames = new ArrayList<>();
		shortNames = new ArrayList<>();
		longNames = new ArrayList<>();
		floatNames = new ArrayList<>();
		doubleNames = new ArrayList<>();
		charNames = new ArrayList<>();
		stringNames = new ArrayList<>();
    }
    
    /**
     * parse the file path specified by the user to locate the
     * source file or files found there.  Append each of these to
     * the Stack container of the class for the iterative scanning
     * process.
     * @param fpath
     */
    public void parseFilesArgs(String fpath) {
    	if(fpath != null) {
    		File fp = new File(fpath);
    		if(fp.exists()) {
    			if(fp.isDirectory()) {
    				File[] flist = fp.listFiles();
    				if(flist != null) {
	    				for(int i = 0; i < flist.length; i++) {
	    					if(flist[i].isFile()) {
	    						if(flist[i].getName().matches(".*.java$")) {
	    							try {
										filelist.push(flist[i].getCanonicalPath());
									} catch (IOException e) {
										System.err.println("Error: failed to get canonical path for file in path");
										e.printStackTrace();
										System.exit(1);
									}
	    						}
	    					}
	    					else if(flist[i].isDirectory()) {
	    						try {
									parseFilesArgs(flist[i].getCanonicalPath());
								} catch (IOException e) {
									System.err.println("Error: failed to get canonical path for file path");
									e.printStackTrace();
								}
	    					}
	    				}
	    			}
    			}
    			else {
    				if(fp.getName().matches(".*.java$")) {
    				    try {
							filelist.push(fp.getCanonicalPath());
						} catch (IOException e) {
							System.err.println("Error: failed to get canonical path for specified file");
							e.printStackTrace();
							System.exit(1);
						}
    				}
    			}
    		}
    	}
    }
    /**
     * get the list of files found by the user specified path 
     * to be scanned for literals.
     * @return a Stack object containing all the java source files
     */
    public Stack<String> getFiles() {
    	return this.filelist;
    }
    /**
     * gets the hashmap object containing the literals found for each class
     * @return
     */
    public HashMap<String, LiteralMap> getLiteralsList() {
    	return this.literalslist;
    }
    
    /**
     * add a literal found to the literals list container object
     * The literals used by Randoop are of 7 types: <br />
     * int, short, long, byte, float, double and String
     * @param type the type of literal to add
     * @param value
     */
    public void addLiteral(String type, String value) {
    	if(literalslist == null) {
    		literalslist = new HashMap<String, LiteralMap>();
    	}
    	if(!literalslist.containsKey(this.currentClass)) {
    		// literalslist needs this class added
    		TreeSet<String> al = new TreeSet<String>();
    		al.add(value);
    		LiteralMap lm = new LiteralMap();
    		lm.put(type, al);
    		literalslist.put(this.currentClass, lm);
    	}
    	else { // literalslist contains current class key
    		//check if this type exists yet for this class in literalsmap
			if(literalslist.get(this.currentClass).containsKey(type)) {
				 // we have this data type for this class in HM, add new value
				literalslist.get(this.currentClass).get(type).add(value);
			}
			else { // Don't have this type in HashMap for this class
				// create a new array list for this data type & add value to it
				TreeSet<String> al = new TreeSet<String>();
				al.add(value);
				literalslist.get(currentClass).put(type, al);
			}
    	}
    }
    /**
     * visit the file specified by the path parameter and collect the literal
     * values found there.  When collecting them it is necessary to identify the
     * type (int/double/float/String) of each found and put them into an ArrayList
     * 
     * @param path
     * @return
     */
    public boolean visitFile(String path) {
    	boolean ret = false;
    	FileInputStream in = null;
		
		try {
			in = new FileInputStream(path);
		} catch (FileNotFoundException e) {
			System.out.println("file open failed on java source-file "+path+": "+e.getMessage());
			e.printStackTrace();
		}

        CompilationUnit cu = null;
        // parse the file
		if(in != null) {
			cu = JavaParser.parse(in);
		}

        // prints the resulting compilation unit to default system output
		if(cu != null) {
			Optional<PackageDeclaration> pd = cu.getPackageDeclaration();
			if(pd.isPresent()) {
				this.currentPkg = pd.get().toString();
			} else {
				this.currentPkg = "";
			}
		    cu.accept(new MethodVisitor(this), null);
		    ret = true;
		}
    	return ret;
    }

	public static void main(String[] args) {
		String fname = null;
		RandoopScraper rs = new RandoopScraper();
		if(args.length > 0) {
		    rs.parseFilesArgs(args[0]);
		    Stack<String> filestack = rs.getFiles();
		    if(!filestack.isEmpty()) {
		    	Iterator<String> file_iter = filestack.iterator();
		    	while(file_iter.hasNext()) {
		    		fname = file_iter.next();
		    		boolean ret = rs.visitFile(fname);
		    		if(!ret) {
		    			System.out.println("Failure in parsing file "+fname);
		    		}
		        }
		    	// Done processing files, create Randoop literals file
		    	if(!rs.literalslist.isEmpty())
		    	    rs.createLiteralsFile();
			}
			else {
				System.out.println("Usage: java RandoopScraper java-source-file-or-dir-tree");
				System.exit(0);
			}
		}
	}
	
	/**
	 * Create the Randoop literals file from the contents of the
	 * literalslist container object.
	 */
	private void createLiteralsFile() {
		boolean ok = true; // write/access failure state
		if(!literalslist.isEmpty()) {
			File out = new File("new_literals_list.txt");
			FileOutputStream ofs = null;
			try {
				if(!out.exists()) {
					out.createNewFile();
				}
				ofs = new FileOutputStream(out);
			} catch (FileNotFoundException e) {
				System.err.println("open of file output stream failed: "+e.getMessage());
				e.printStackTrace();
				ok = false;
			} catch(IOException ioe) {
				System.err.println("Create of output file failed: "+ioe.getMessage());
				ioe.printStackTrace();
				ok = false;
			}
			String preamble = "START CLASSLITERALS\n";
			try {
				ofs.write(preamble.getBytes());
				ofs.flush();
			} catch (IOException e1) {
				System.err.println("Error writing preamble to output file "+e1.getMessage());
				e1.printStackTrace();
				ok = false;
			}
			Set<String> class_set = literalslist.keySet();
			Iterator<String> class_iter = class_set.iterator();
			while(ok && class_iter.hasNext()) {
				String currClass = class_iter.next();
				LiteralMap lm = literalslist.get(currClass);
				if(lm != null) {
					// Have a set of literals for this currClass
					String classpreamble = "CLASSNAME\n"+currClass+"\nLITERALS\n";
					try {
						ofs.write(classpreamble.getBytes());
						ofs.flush();
					} catch (IOException e1) {
						System.err.println("Error writing classname preamble to output file "+e1.getMessage());
						e1.printStackTrace();
						ok = false;
					}
					Set<String> type_set = lm.keySet();
					Iterator<String> type_iter = type_set.iterator();
					while(type_iter.hasNext()) {
						String currType = type_iter.next();
						TreeSet<String> type_values = lm.get(currType);
						for(String tvalue : type_values) {
							if(!eliminateDup(currType, tvalue)) {
								try {
									ofs.write((currType+":"+tvalue+"\n").getBytes());
									ofs.flush();
								} catch (IOException e1) {
									System.err.println("Error writing type:value to output file "+e1.getMessage());
									e1.printStackTrace();
									ok = false;
								}
							}
						}
					}
					String classpost = "END CLASSLITERALS\n";
					try {
						ofs.write(classpost.getBytes());
					} catch (IOException e1) {
						System.err.println("Error writing classpost to output file "+e1.getMessage());
						e1.printStackTrace();
						ok = false;
					}
				}
			}
			if(ofs != null)
				try {
					ofs.close();
				} catch (IOException e) {
					System.err.println("error closing: "+out.getName()+" "+e.getMessage());
					e.printStackTrace();
				}
		}
	}
	
	/**
	 * method to determine if the supplied value is already a
	 * Randoop default test input parameter.  If it is, then this
	 * returns false, indicating that the value need not be included
	 * into the set of literals.
	 * @param type variable type: one of int, short, byte, long, char, double, float, String
	 * @param value string representation of the value applied to the type
	 * @return false if this not an existing Randoop default test input parameter
	 * true if it already is an existing Randoop input.
	 */
	public boolean eliminateDup(String type, String value) {
		boolean ret = false;
		if(type.equals("String")) {
			for(String s : stringvals) {
				if(s.equals(value)) { // if ever found return false
					ret = true;
					break;
				}
			}
		}
		else if(type.equals("int")) {
			for(int i : intvals) {
				if(Integer.parseInt(value) == i) {
					ret = true;
					break;
				}
			}
		}
		else if(type.equals("byte")) {
			for(byte b : bytevals) {
				if(value.matches("^0x[0-9a-fA-F]+")) { // hex byte case
					String b_str = Integer.toHexString(b);
					if(("0x"+b_str).equalsIgnoreCase(value)) {
						ret = true;
						break;
					}
				}
				else if(value.matches("[a-zA-Z]")) { // char byte case
					int value_int = Character.getNumericValue(value.toCharArray()[0]);
					if(b == value_int) {
						ret = true;
						break;
					}
				}
				else if(value.matches("^0[0-7]+")) { // octal byte case
					int octnum = Integer.parseInt(value);
					int decnum = 0, i = 0;
					while(octnum != 0)
			        {
			            decnum = decnum + (octnum%10) * (int) Math.pow(8, i);
			            i++;
			            octnum = octnum/10;
			        }
					if(decnum == b) {
						ret = true;
						break;
					}
				}
				else {
					if(Byte.parseByte(value) == b) {
						ret = true;
						break;
					}
				}
			}
		}
		else if(type.equals("short")) {
			for(short s : shortvals) {
				if(Short.parseShort(value) == s) {
					ret = true;
					break;
				}
			}
		}
		else if(type.equals("long")) {
			for(long l : longvals) {
				if(Long.parseLong(value) == l) {
					ret = true;
					break;
				}
			}
		}
		else if(type.equals("float")) {
			for(float f : floatvals) {
				if(Float.parseFloat(value) == f) {
					ret = true;
					break;
				}
			}
		}
		else if(type.equals("double")) {
			for(double d : doublevals) {
				if(Double.parseDouble(value) == d) {
					ret = true;
					break;
				}
			}
		}
		else if(type.equals("char")) {
			for(char c : charvals) {
				if(value.indexOf(c) >= 0) {
					ret = true;
					break;
				}
			}
		}
		return ret;
	}

	private static class MethodVisitor extends VoidVisitorAdapter<Void> {
		RandoopScraper rs;
		public MethodVisitor(RandoopScraper p) {
			this.rs = p;
		}
		
		// conscience decision to keep all literals found within
		// a class file, even if they occur within contained inner
		// classes to be used by the outer class
		@Override
		public void visit(ClassOrInterfaceDeclaration n, Void arg) {
			if(!n.isInnerClass())
			    rs.currentClass = n.getNameAsString();
			super.visit(n, arg);
		}

        @Override
        public void visit(DoubleLiteralExpr n, Void arg) {
        	//System.out.println("DoubleLiteralExpr: "+n.asDouble());
        	if(n.getValue().endsWith("f")) // doubles and floats are same in Javaparser
        		rs.addLiteral("float", n.getValue());
        	rs.addLiteral("double", n.getValue());
        }
        
        @Override
        public void visit(CharLiteralExpr n, Void arg) {
        	//System.out.println("CharLiteralExpr: "+n.asChar());
        	rs.addLiteral("char", n.getValue());
        }
        
        @Override
        public void visit(IntegerLiteralExpr n, Void arg) {
        	//System.out.println("IntegerLiteralExpr: "+n.asInt());
        	rs.addLiteral("int", n.getValue());
        	if(Integer.parseInt(n.getValue()) <= Short.MAX_VALUE && Integer.parseInt(n.getValue()) >= Short.MIN_VALUE)
        	    rs.addLiteral("short", n.getValue());
        	if(Integer.parseInt(n.getValue()) <= Byte.MAX_VALUE && Integer.parseInt(n.getValue()) >= Byte.MIN_VALUE)
        	    rs.addLiteral("byte", n.getValue());
        }
        
        @Override
        public void visit(LongLiteralExpr n, Void arg) {
        	//System.out.println("LongLiteralExpr: "+n.asLong());
        	rs.addLiteral("long", n.getValue());
        }
        
        @Override
        public void visit(StringLiteralExpr n, Void arg) {
        	//System.out.println("StringLiteralExpr: "+n.asString());
        	rs.addLiteral("String", n.asString());
        }
	}

	/**
	 * A HashMap type object to contain mappings between literal
	 * type and specific literal values found during the scanning
	 * process for that type.
	 * @author Wayne Motycka
	 *
	 */
	@SuppressWarnings("serial")
	/*protected class LiteralMap extends HashMap<String, ArrayList<String>> {
		
		HashMap<String, ArrayList<String>> type_vals;
		public LiteralMap() {
			type_vals = new HashMap<String, ArrayList<String>>();
		}
	}*/
	protected class LiteralMap extends HashMap<String, TreeSet<String>> {
		HashMap<String, TreeSet<String>> type_vals;
		public LiteralMap() {
			type_vals = new HashMap<String, TreeSet<String>>();
		}
	}
	
}

