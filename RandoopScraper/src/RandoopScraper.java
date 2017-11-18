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
			System.out.println("file open failed on java source-file "+e.getMessage());
			e.printStackTrace();
		}

        CompilationUnit cu = null;
        // parse the file
		if(in != null) {
			Optional<PackageDeclaration> pd = cu.getPackageDeclaration();
			if(pd.isPresent()) {
				this.currentPkg = pd.get().toString();
			} else {
				this.currentPkg = "";
			}
            cu = JavaParser.parse(in);
		}

        // prints the resulting compilation unit to default system output
		if(cu != null) {
            //System.out.println(cu.toString());
			//int cntr = 1;
			//List<Node> children = cu.getChildNodes();
			//Iterator<Node> it = children.iterator();
			//while (it.hasNext()) {
			//	Node node = it.next();
			//	System.out.println("node_"+cntr+": "+node.toString());
			//	cntr++;
			//}
		    cu.accept(new MethodVisitor(this), null);
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
		    		rs.visitFile(fname);
		        }
		    	// Done processing files, create Randoop literals file
		    	rs.createLiteralsFile();
			}
			else {
				System.out.println("Usage: PlayJavaparser java-source-file-or-dir");
				System.exit(0);
			}
		
		}
	}
	
	/**
	 * create the Randoop literals file from the contents of the
	 * literalslist object.
	 */
	private void createLiteralsFile() {
		if(!literalslist.isEmpty()) {
			File out = new File("literals_list.txt");
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
							// TODO: FORMAT AN OUTPUT STRING FOR EACH VALUE
						}
					}
				}
			}
		}
	}

	private static class MethodVisitor extends VoidVisitorAdapter<Void> {
		RandoopScraper rs;
		public MethodVisitor(RandoopScraper p) {
			this.rs = p;
		}
		@Override
		public void visit(ClassOrInterfaceDeclaration n, Void arg) {
			if(n.isInnerClass()) {
				System.out.println("InnerClassDec: "+n.toString());
			}
			else {
				rs.currentClass = n.getNameAsString();
				//String nas = n.getNameAsString();
				//SimpleName sname = n.getName();
				//String snas = sname.asString();
				System.out.println("ContainingClassDec: "+n.getNameAsString());
				super.visit(n, arg);
			}
		}
		
        //@Override
        //public void visit(MethodDeclaration n, Void arg) {
            /* here you can access the attributes of the method.
             this method will be called for all methods in this 
             CompilationUnit, including inner class methods */
        //    System.out.println(n.getName());
        //    super.visit(n, arg);
        //}
		
        @Override
        public void visit(EnumDeclaration n, Void arg) {
        	System.out.println(n.getNameAsString());
        	super.visit(n, arg);
        }
        
        @Override
        public void visit(FieldDeclaration n, Void arg) {
        	String type = ((FieldDeclaration)n).getElementType().asString();
        	if(((FieldDeclaration)n).getElementType().isPrimitiveType()) {
        		if(type.equals("int")) {
        			System.out.print("INT ");
        		}
        		else if(type.equals("short")) {
        			System.out.print("SHORT ");
        		}
                else if(type.equals("long")) {
        			System.out.print("LONG ");
        		}
        		else if(type.equals("double")) {
        			System.out.print("DOUBLE ");
        		}
        		else if(type.equals("float")) {
        			System.out.print("FLOAT ");
        		}
        		else if(type.equals("byte")) {
        			System.out.print("BYTE ");
        		}
        		else if(type.equals("char")) {
        			System.out.print("CHAR ");
        		}
        		else if(type.equals("String")) {
        			System.out.print("STRING ");
        		}
        		else if(type.equals("Object")) {
        			System.out.print("OBJECT ");
        		}
        	}
        	System.out.println("Field Declaration: "+type+" "+n.getVariables().toString());
        	super.visit(n,arg);
        }
        
        @Override
        public void visit(AssignExpr n, Void arg) {
        	System.out.println("AssignExpr: "+n.toString());
        	super.visit(n, arg);
        }
        @Override
        public void visit(ForStmt n, Void arg) {
        	NodeList<Expression> nl = n.getInitialization();
        	int ncnt = 1;
        	Iterator<Expression> iter = nl.iterator();
        	System.out.print("ForStmtInit: ");
        	while(iter.hasNext()) {
        	    System.out.println("init op "+ncnt+": "+iter.next().toString());
        	    ncnt++;
        	}
        	Expression condex = n.getCompare().get();
        	System.out.println("ForStmt condition: "+condex.toString());
        }
        @Override
        public void visit(IfStmt n, Void arg) {
        	System.out.println("IfStmt condtion: "+n.getCondition());
        	System.out.println("Else: "+n.getElseStmt().toString());
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

