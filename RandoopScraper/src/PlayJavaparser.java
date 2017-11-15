import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.commons.collections4.multimap.AbstractListValuedMap;
import org.apache.commons.collections4.multimap.AbstractMultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.*;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;


public class PlayJavaparser {
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
	/**
	 * Container used to store file names of java class files found
	 * during scan
	 */
	Stack<String> filelist;              // input files list to scan
	/**
	 * Container mapping input file names to class containing a stack
	 * of the literals found, literals are translated into strings
	 * so each will have a type associated with it.
	 */
	Map<String, ClassLiterals> literals; // maps files to literals found in each
	
    public PlayJavaparser() {
		filelist = new Stack<String>();
	}
    
    public void parseFilesArgs(String fpath) {
    	if(fpath != null) {
    		File fp = new File(fpath);
    		if(fp.exists()) {
    			if(fp.isDirectory()) {
    				File[] flist = fp.listFiles();
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
    	}
    }
    
    public Stack<String> getFiles() {
    	return this.filelist;
    }
    
    /**
     * writes a structured literals file in the format required by Randoop
     * for inclusion using the --literal-files= command line flag
     * Basic format is:
     * START CLASSLITERALS
     * CLASSNAME
     * full-qualified-path-name
     */
    public void buildOutFile() {
    	    if(!this.filelist.isEmpty()) {
    	    	    PrintStream outstream = null;
    	    	    try {
				outstream = new PrintStream("new_literals.txt");
			} catch (FileNotFoundException e) {
				System.err.println("Failed to create new_literals.txt output file");
				e.printStackTrace();
			}
    	    	    if(outstream != null) {
    	    	    	    outstream.println("START CLASSLITERALS");
    	    	    	    outstream.flush();
    	    	    	    Iterator<String> iter = filelist.iterator();
    	    	    	    while(iter.hasNext()) {
    	    	    	    	    String clname = iter.next();
    	    	    	    	    if(literals.containsKey(clname)) {
    	    	    	    	        outstream.println("CLASSNAME");
    	    	    	    	        outstream.println(literals.get(clname).getQName());
    	    	    	    	        Stack<Literal> lits = literals.get(clname).getLiterals();
    	    	    	    	        Iterator<Literal> lit_iter = lits.iterator();
    	    	    	    	        
    	    	    	    	    }
    	    	    	    }
    	    	    }
    	    	    return;
    	    }
    }

	public static void main(String[] args) {
		String fname = null;
		PlayJavaparser pj = new PlayJavaparser();
		if(args.length > 0) {
		    pj.parseFilesArgs(args[0]);
		    Stack<String> filestack = pj.getFiles();
		    if(!filestack.isEmpty()) {
		    	Iterator<String> file_iter = filestack.iterator();
		    	while(file_iter.hasNext()) {
		    		fname = file_iter.next();
					//fname = args[0];
					FileInputStream in = null;
					
					try {
						in = new FileInputStream(fname);
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
			            //System.out.println(cu.toString());
						int cntr = 1;
						List<Node> children = cu.getChildNodes();
						Iterator<Node> it = children.iterator();
						//while (it.hasNext()) {
						//	Node node = it.next();
						//	System.out.println("node_"+cntr+": "+node.toString());
						//	cntr++;
						//}
					    cu.accept(new MethodVisitor(), null);
					}
		        }
			}
			else {
				System.out.println("Usage: PlayJavaparser java-source-file-or-dir");
				System.exit(0);
			}
		
		}
	}
	
	private static class MethodVisitor extends VoidVisitorAdapter<Void> {
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
        	Iterator iter = nl.iterator();
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
	/**
	 * container class for storing the literals found for each class
	 * @author wmotycka
	 *
	 */
	private class ClassLiterals {
		String filename;
		String qualifiedname;
		Stack<Literal> literalvals;
		ArrayListValuedHashMap literals;
		public ClassLiterals() {
			literals = new ArrayListValuedHashMap<String, Literal>();
			literalvals = new Stack<Literal>();
			filename = null;
			qualifiedname = null;
		}
		public ClassLiterals(String name) {
			this.filename = name;
		}
		public void setQName(String qname) {
			this.qualifiedname = qname;
		}
		public String getQName() {
			return this.qualifiedname;
		}
		public void addLiteral(Literal lit) {
			this.literalvals.push(lit);
			this.literals.put(lit.lit_type, lit.lit_value);
		}
		public Stack<Literal> getLiterals() {
			return this.literalvals;
		}
		public List<String> getLiterals(String key) {
			return this.literals.get(key);
		}
	}
	/**
	 * container class to store each literal, types are converted to
	 * strings as well as values prior to being placed into the container.
	 * @author wmotycka
	 *
	 */
	private class Literal {
		String lit_type;
		String lit_value;
		
		public Literal() {
			lit_type = null;
			lit_value = null;
		}
		
		public Literal(String type, String value) {
			this.lit_type = type;
			this.lit_value = value;
		}
		
		public void setType(String type) {
			this.lit_type = type;
		}
		
		public void setValue(String value) {
			this.lit_value = value;
		}
		
		public String getType() {
			return this.lit_type;
		}
		
		public String getValue() {
			return this.lit_value;
		}
	}
	
}
