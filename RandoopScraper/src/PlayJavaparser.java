import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

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
	Stack<String> filelist;
	
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
	
	
}
