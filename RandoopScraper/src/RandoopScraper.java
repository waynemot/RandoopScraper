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
import com.github.javaparser.ast.nodeTypes.NodeWithVariables;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

import java.util.*;

public class RandoopScraper {
    /**
     * constants defining the standard test values used
     * by randoop in forming tests.  These should be avoided
     * since adding more of these will not enhance the
     * value set used.
     */
	
	static final byte[] bytevals = {-1, 0, 1, 10, 100};
	static final short[] shortvals = {-1, 0, 1, 10, 100};
	static final int[] intvals = {-1, 0, 1, 10, 100};
	static final long[] longvals = { -1, 0, 1, 10, 100};
	static final float[] floatvals = { -1, 0, 1, 10, 100};
	static final double[] doublevals =  {-1, 0, 1, 10, 100};
	static final char[] charvals = { '#', ' ', '4', 'a'};
	static final java.lang.String[] stringvals = { "", "hi!"};
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

	HashMap<String, LiteralMap> literalslist; // maps class to data-types/values map

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
     * get the list of files scanned
     * @return
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
    
    public void addLiteral(String type, String value) {
    	if(literalslist == null) {
    		literalslist = new HashMap<String, LiteralMap>();
    	}
    	if(!literalslist.containsKey(this.currentClass)) {
    		// literalslist needs this class added
    		ArrayList<String> al = new ArrayList<String>();
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
				ArrayList<String> al = new ArrayList<String>();
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
		if(!literalslist.isEmpty()) {
			File out = new File("new_literals_list.txt");
			try {
				if(!out.exists()) {
					out.createNewFile();
				}
				FileOutputStream ofs = new FileOutputStream(out);
			} catch (FileNotFoundException e) {
				System.err.println("open of file output stream failed "+e.getMessage());
				e.printStackTrace();
			} catch(IOException ioe) {
				System.err.println("Create of output file failed "+ioe.getMessage());
				ioe.printStackTrace();
			}
			// TODO: CREATE PREAMBLE OF LITERALS FILE HERE
			Set<String> class_set = literalslist.keySet();
			Iterator<String> class_iter = class_set.iterator();
			while(class_iter.hasNext()) {
				String currClass = class_iter.next();
				LiteralMap lm = literalslist.get(currClass);
				if(lm != null) {
					// Have a set of literals for this currClass
					// TODO: FORMAT AN OUTPUT STREAM WRITE TO ANNOUNCE THIS CLASS
					Set<String> type_set = lm.keySet();
					Iterator<String> type_iter = type_set.iterator();
					while(type_iter.hasNext()) {
						String currType = type_iter.next();
						ArrayList<String> type_values = lm.get(currType);
						for(String tvalue : type_values) {
							if(!eliminateDup(currType, tvalue)) {
								
							}
							// TODO: FORMAT AN OUTPUT STRING FOR EACH VALUE
						}
					}
				}
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
	 * @return true if this not an existing Randoop default test input parameter
	 */
	public boolean eliminateDup(String type, String value) {
		boolean ret = true;
		if(type.equals("String")) {
			for(String s : stringvals) {
				if(s.equals(value)) { // if ever found return false
					ret = false;
					break;
				}
			}
		}
		else if(type.equals("int")) {
			for(int i : intvals) {
				if(Integer.parseInt(value) == i) {
					ret = false;
					break;
				}
			}
		}
		else if(type.equals("byte")) {
			for(byte b : bytevals) {
				if(Byte.parseByte(value) == b) {
					ret = false;
					break;
				}
			}
		}
		else if(type.equals("short")) {
			for(short s : shortvals) {
				if(Short.parseShort(value) == s) {
					ret = false;
					break;
				}
			}
		}
		else if(type.equals("long")) {
			for(long l : longvals) {
				if(Long.parseLong(value) == l) {
					ret = false;
					break;
				}
			}
		}
		else if(type.equals("float")) {
			for(float f : floatvals) {
				if(Float.parseFloat(value) == f) {
					ret = false;
					break;
				}
			}
		}
		else if(type.equals("double")) {
			for(double d : doublevals) {
				if(Double.parseDouble(value) == d) {
					ret = false;
					break;
				}
			}
		}
		else if(type.equals("char")) {
			for(char c : charvals) {
				if(value.indexOf(c) >= 0) {
					ret = false;
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
        	System.out.println("DoubleLiteralExpr: "+n.asDouble());
        	if(n.getValue().endsWith("f")) // doubles and floats are same in Javaparser
        		rs.addLiteral("float", n.getValue());
        	rs.addLiteral("double", n.getValue());
        }
        
        @Override
        public void visit(CharLiteralExpr n, Void arg) {
        	System.out.println("CharLiteralExpr: "+n.asChar());
        	rs.addLiteral("char", n.getValue());
        }
        
        @Override
        public void visit(IntegerLiteralExpr n, Void arg) {
        	System.out.println("IntegerLiteralExpr: "+n.asInt());
        	rs.addLiteral("int", n.getValue());
        	rs.addLiteral("byte", n.getValue()); // Can't tell diff btwn int/short/byte
        	rs.addLiteral("short", n.getValue()); // so over approximate
        }
        
        @Override
        public void visit(LongLiteralExpr n, Void arg) {
        	System.out.println("LongLiteralExpr: "+n.asLong());
        	rs.addLiteral("long", n.getValue());
        }
        
        @Override
        public void visit(StringLiteralExpr n, Void arg) {
        	System.out.println("StringLiteralExpr: "+n.asString());
        	rs.addLiteral("String", n.asString());
        }
	}

	@SuppressWarnings("serial")
	protected class LiteralMap extends HashMap<String, ArrayList<String>> {
		HashMap<String, ArrayList<String>> type_vals;
		public LiteralMap() {
			type_vals = new HashMap<String, ArrayList<String>>();
		}
	}
	
}

