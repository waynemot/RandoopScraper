import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.nodeTypes.NodeWithVariables;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.stmt.ForStmt;
import com.github.javaparser.ast.stmt.IfStmt;
import com.github.javaparser.ast.stmt.SwitchEntryStmt;
import com.github.javaparser.ast.stmt.WhileStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

public class Scraper {

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
	
	public Scraper() {
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
		}
    	return ret;
    }
	
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
							// TODO: FORMAT AN OUTPUT STRING FOR EACH VALUE
						}
					}
				}
			}
		}
	}
	
	public static void main(String[] args) {
		String fname = null;
		Scraper rs = new Scraper();
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
				System.out.println("Usage: RandoopScraper java-source-file-or-dir");
				System.exit(0);
			}
		}
	}

	private static class MethodVisitor extends VoidVisitorAdapter<Void> {
		Scraper rs;
		public MethodVisitor(Scraper p) {
			this.rs = p;
		}
		
		@Override
		public void visit(ClassOrInterfaceDeclaration n, Void arg) {
			if(n.isInnerClass()) {
				System.out.println("InnerClassDec: "+n.toString());
			}
			else {
				rs.currentClass = n.getNameAsString();
				System.out.println("ContainingClassDec: "+n.getNameAsString());
			}
			super.visit(n, arg);
		}
		
		@Override
		public void visit(ExpressionStmt n, Void arg) {
			System.out.println("ExpressionStmt: "+n.toString());
			List<Node> children = n.getChildNodes();
	        	for(Node child : children) {
	        		child.accept(this, null);
	        	}
			super.visit(n, arg);
		}
		
		@Override
		public void visit(WhileStmt n, Void arg) {
			System.out.println("WhileStmt: "+n.toString());
			List<Node> children = n.getChildNodes();
	        	for(Node child : children) {
	        		child.accept(this, null);
	        	}
			super.visit(n, arg);
		}
		
		@Override
		public void visit(SwitchEntryStmt n, Void arg) {
			System.out.println("SwitchEntryStmt: "+n.toString());
			List<Node> children = n.getChildNodes();
	        	for(Node child : children) {
	        		child.accept(this, null);
	        	}
			super.visit(n, arg);
		}
		
		@Override
        public void visit(MethodCallExpr n, Void arg) {
        	    System.out.println("MethodCallExpr: "+n.toString());
        	    List<Node> children = n.getChildNodes();
        	    for(Node child : children) {
        	    	    child.accept(this, null);
        	    }
        	    super.visit(n, arg);
        }
		
		@Override
        public void visit(ForStmt n, Void arg) {
	        	NodeList<Expression> nl = n.getInitialization();
	        	int ncnt = 1;
	        	Iterator<Expression> iter = nl.iterator();
	        	System.out.print("ForStmtInit: ");
	        	while(iter.hasNext()) {
	        		Expression e = iter.next();
	        	    System.out.println("init op "+ncnt+": "+e.toString());
	        	    ncnt++;
	        	    e.accept(this, null);
	        	}
	        	Expression condex = n.getCompare().get();
	        	System.out.println("ForStmt condition: "+condex.toString());
	        	if(condex instanceof AssignExpr) {
	        		((AssignExpr)condex).accept(this, null);
	        	}
	        	super.visit(n, arg);
        }

        @Override
        public void visit(IfStmt n, Void arg) {
	        	System.out.println("IfStmt condtion: "+n.getCondition());
	        	System.out.println("Else: "+n.getElseStmt().toString());
	        	List<Node> children = n.getChildNodes();
	        	for(Node child : children) {
	        		child.accept(this, null);
	        	}
	        	super.visit(n, arg);
        }

		@Override
        public void visit(EnumDeclaration n, Void arg) {
	        	System.out.println(n.getNameAsString());
	        	List<Node> children = n.getChildNodes();
	        	for(Node child : children) {
	        		child.accept(this, null);
	        	}
	        	super.visit(n, arg);
        }

		@Override
		public void visit(FieldDeclaration n, Void arg) {
	        	System.out.println("Field Declaration: "+n.toString());
	        	if(n instanceof NodeWithVariables) {
	        		System.out.println("Field Declaration Node w/Vars");
	        		List<Node> children = n.getChildNodes();
	        		for(Node child : children) {
	        			child.accept(this, null);
	        		}
	        	}
	        	super.visit(n,arg);
		}
		
		@Override
		public void visit(ArrayInitializerExpr n, Void arg) {
			System.out.println("ArrayInitExpr: "+n.toString());
			List<Node> children = n.getChildNodes();
			for(Node child : children) {
				child.accept(this, null);
			}
			super.visit(n, arg);
		}

		@Override
		public void visit(EnclosedExpr n, Void arg) {
			System.out.println("EnclosedExpr: "+n.toString());
			List<Node> children = n.getChildNodes();
			for(Node child : children) {
				child.accept(this, null);
			}
			super.visit(n, arg);
		}
		@Override
	    public void visit(AssignExpr n, Void arg) {
	        	System.out.println("AssignExpr: "+n.toString());
	        	List<Node> children = n.getChildNodes();
	        	for(Node child : children) {
	        		child.accept(this, null);
	        	}
	        	super.visit(n, arg);
	    }
		
		@Override
		public void visit(VariableDeclarationExpr n, Void arg) {
			System.out.println("VarDeclExpr: "+n.toString());
			List<Node> children = n.getChildNodes();
			for(Node child : children) {
				child.accept(this, null);
			}
			super.visit(n, arg);
		}

        @Override
        public void visit(DoubleLiteralExpr n, Void arg) {
        		System.out.println("DoubleLiteralExpr: "+n.asDouble());
        		super.visit(n, arg);
        }
        
        @Override
        public void visit(CharLiteralExpr n, Void arg) {
        		System.out.println("CharLiteralExpr: "+n.asChar());
        		super.visit(n, arg);
        }
        
        @Override
        public void visit(IntegerLiteralExpr n, Void arg) {
        	    System.out.println("IntegerLiteralExpr: "+n.asInt());
        	    super.visit(n, arg);
        }
        
        @Override
        public void visit(LongLiteralExpr n, Void arg) {
        		System.out.println("LongLiteralExpr: "+n.asLong());
        		super.visit(n, arg);
        }
        
        @Override
        public void visit(StringLiteralExpr n, Void arg) {
        		System.out.println("StringLiteralExpr: "+n.asString());
        		super.visit(n, arg);
        }
	}
	
	@SuppressWarnings("serial")
	private class LiteralMap extends HashMap<String, ArrayList<String>> {
		HashMap<String, ArrayList<String>> type_vals;
		public LiteralMap() {
			type_vals = new HashMap<String, ArrayList<String>>();
		}
	}
}
