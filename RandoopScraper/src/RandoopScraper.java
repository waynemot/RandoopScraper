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
				System.out.println("Usage: java RandoopScraper java-source-file-or-dir-tree");
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
        	if(n instanceof NodeWithVariables) {
        		System.out.println("Field Declaration Node w/Vars");
        	}
        	String type = ((FieldDeclaration)n).getElementType().asString();
        	if(((FieldDeclaration)n).getElementType().isPrimitiveType()) {
        		if(type.equals("int")) {
        			System.out.print("INT ");
        			rs.intNames.add(n.getVariable(0).getNameAsString());
        			if(n.getVariable(0).getInitializer().isPresent()) {
        				System.out.println("initializer to int: "+
        				    n.getVariable(0).getInitializer().get().toString());
        				NodeList<VariableDeclarator> vdnodes = n.getVariables();
        				for(VariableDeclarator vd : vdnodes) {
        					List<Node> vd_children = vd.getChildNodes();
        					for(Node vd_child : vd_children) {
        						vd_child.accept(this, null);
        					}
        				}
                        // Add this literal value to the literals hashmap
        				rs.addLiteral(type, n.getVariable(0).getInitializer().get().toString());
        			}
        		}
        		else if(type.equals("short")) {
        			System.out.print("SHORT ");
        			rs.shortNames.add(n.getVariable(0).getNameAsString());
        			if(n.getVariable(0).getInitializer().isPresent()) {
        				System.out.println("initializer to short: "+
        				    n.getVariable(0).getInitializer().get().toString());
                        // Add this literal value to the literals hashmap
        				rs.addLiteral(type, n.getVariable(0).getInitializer().get().toString());
        			}
        		}
                else if(type.equals("long")) {
        			System.out.print("LONG ");
        			rs.longNames.add(n.getVariable(0).getNameAsString());
        			if(n.getVariable(0).getInitializer().isPresent()) {
        				System.out.println("initializer to long: "+
        				    n.getVariable(0).getInitializer().get().toString());
                        // Add this literal value to the literals hashmap
        				rs.addLiteral(type, n.getVariable(0).getInitializer().get().toString());
        			}
        		}
        		else if(type.equals("double")) {
        			System.out.print("DOUBLE ");
        			rs.doubleNames.add(n.getVariable(0).getNameAsString());
        			if(n.getVariable(0).getInitializer().isPresent()) {
        				System.out.println("initializer to double: "+
        				    n.getVariable(0).getInitializer().get().toString());
                        // Add this literal value to the literals hashmap
        				rs.addLiteral(type, n.getVariable(0).getInitializer().get().toString());
        			}
        		}
        		else if(type.equals("float")) {
        			System.out.print("FLOAT ");
        			rs.floatNames.add(n.getVariable(0).getNameAsString());
        			if(n.getVariable(0).getInitializer().isPresent()) {
        				System.out.println("initializer to float: "+
        				    n.getVariable(0).getInitializer().get().toString());
                        // Add this literal value to the literals hashmap
        				rs.addLiteral(type, n.getVariable(0).getInitializer().get().toString());
        			}
        		}
        		else if(type.equals("byte")) {
        			System.out.print("BYTE ");
        			rs.byteNames.add(n.getVariable(0).getNameAsString());
        			if(n.getVariable(0).getInitializer().isPresent()) {
        				System.out.println("initializer to byte: "+
        				    n.getVariable(0).getInitializer().get().toString());
                        // Add this literal value to the literals hashmap
        				String bvalue;
        				if(n.getVariable(0).getInitializer().get().toString().matches("\'.*\'")) {
        					String value = n.getVariable(0).getInitializer().get().toString();
        					bvalue = value.substring(1, value.length()-1);
        				} else {
        					bvalue = n.getVariable(0).getInitializer().get().toString();
        				}
        				rs.addLiteral(type, bvalue);
        			}
        		}
        		else if(type.equals("char")) {
        			System.out.print("CHAR ");
        			rs.charNames.add(n.getVariable(0).getNameAsString());
        			if(n.getVariable(0).getInitializer().isPresent()) {
        				System.out.println("initializer to char: "+
        				    n.getVariable(0).getInitializer().get().toString());
        				if(n.getVariable(0).getInitializer().get().toString().matches("\'.*\'")) {
        					String value = n.getVariable(0).getInitializer().get().toString();
        					String bvalue = value.substring(1, value.length()-1);
        				    rs.addLiteral(type, bvalue);
        				}
                        // Add this literal value to the literals hashmap
        				//rs.addLiteral(type, n.getVariable(0).getInitializer().get().toString());
        			}
        		}
        	}
        	else {
        		if(type.equals("String")) {
        			System.out.print("STRING ");
        			rs.stringNames.add(n.getVariable(0).getNameAsString());
        			if(n.getVariable(0).getInitializer().isPresent()) {
        				System.out.println("initializer to String: "+
        				    n.getVariable(0).getInitializer().get().toString());
                        // Add this literal value to the literals hashmap, trim "'s from ends
        				String stemp = n.getVariable(0).getInitializer().get().toString().substring
        						(1, n.getVariable(0).getInitializer().get().toString().length()-1);
        				rs.addLiteral(type,stemp);
        			}
        		}
        		else if(type.equals("Object")) {
        			System.out.print("OBJECT ");
        			if(n.getVariable(0).getInitializer().isPresent()) {
        				String sval = null;
        				List<Node> nodelist = n.getChildNodes();
        				Iterator<Node> n_iter = nodelist.iterator();
        				while(n_iter.hasNext()) {
        					Node onode = n_iter.next();
        					if(onode instanceof VariableDeclarator) {
        						if(((VariableDeclarator)onode).getInitializer().isPresent()) {
	        						Optional<Expression> oe = ((VariableDeclarator)onode).getInitializer();
	        						Expression expr = oe.get();
	        						sval = expr.toString();
	        						boolean clean = false;
	        						while(!clean) {
		        						if(sval.contains("(")) {
		        							int start = sval.indexOf("(");
		        							int end = sval.lastIndexOf(")");
		        							String tmpstr = sval.substring(start,end);
		        							sval = tmpstr;
		        						}
		        						else clean = true;
	        						}
	        						System.out.println("declared value: "+sval);
	        						if(sval.matches("\".*\"")) {
	        							sval = sval.substring(1, sval.length()-1);
	        						}
        						}
        					}
        				}
        				if(sval != null) { // svar has the unquoted string, if its a string
	        				String varname = n.getVariable(0).getInitializer().get().toString();
	        				System.out.println("initializer to Object: "+varname);
	        				ConcreteType ct = new ConcreteType();
	        				ct.setValue(varname); // need the quoted type here
	        				ct.findType();
	        				String[] cttypes = ct.getAlternates();
	        				if(cttypes.length > 0) {
	        					String rsname = null;
	        					if(varname.matches("\".*\"")) {
		         			        rsname = n.getVariable(0).getNameAsString();
		        				}
	        					for(String ctype : cttypes) {
	        						if(ctype.equals("String")) {
	        							if(rsname != null) rs.stringNames.add(rsname);
	        							rs.addLiteral(ctype, sval);
	        						}
	        						else if(ctype.equals("int")) {
	        							if(rsname != null) rs.intNames.add(rsname);
	        							rs.addLiteral(ctype, sval);
	        						}
	        						else if(ctype.equals("short")) {
	        							if(rsname != null) rs.shortNames.add(rsname);
	        							rs.addLiteral(ctype, sval);
	        						}
	        						else if(ctype.equals("long")) {
	        							if(rsname != null) rs.longNames.add(rsname);
	        							rs.addLiteral(ctype, sval);
	        						}
	        						else if(ctype.equals("double")) {
	        							if(rsname != null) rs.doubleNames.add(rsname);
	        							rs.addLiteral(ctype, sval);
	        						}
	        						else if(ctype.equals("float")) {
	        							if(rsname != null) rs.floatNames.add(rsname);
	        							rs.addLiteral(ctype, sval);
	        						}
	        						else if(ctype.equals("byte")) {
	        							if(rsname != null) rs.byteNames.add(rsname);
	        							rs.addLiteral(ctype, sval);
	        						}
	        						else if(ctype.equals("char")) {
	        							if(rsname != null) rs.charNames.add(rsname);
	        							rs.addLiteral(ctype, sval);
	        						}
	        					}
	        				}
	        				//if(varname.matches("\".*\"")) {
	         			    //    rs.stringNames.add(n.getVariable(0).getNameAsString());
	        				//}
	                       // Add this literal value to the literals hashmap, trim "'s @ ends
	        				//String stemp = n.getVariable(0).getInitializer().get().toString().substring
	        				//		(1, n.getVariable(0).getInitializer().get().toString().length()-1);
	        				//rs.addLiteral("String", sval);
        				}
        			}
        		}
        		else {
        			System.out.println("Non-Primative "+type);
        		}
        	}
        	System.out.println("Field Declaration: "+type+" "+n.getVariables().toString());
        	super.visit(n,arg);
        }
        
        /**
         * helper method to parse Object declarations into concrete types
         * @param o
         * @return
         */
        public Object parseObject(Object o) {
        	Object ret = null;
        	FieldDeclaration n = null;
        	if(o instanceof FieldDeclaration) {
        		n = (FieldDeclaration)o;
        	}
        	if(n.getVariable(0).getInitializer().isPresent()) {
				String sval = null;
				List<Node> nodelist = n.getChildNodes();
				Iterator<Node> n_iter = nodelist.iterator();
				while(n_iter.hasNext()) {
					Node onode = n_iter.next();
					if(onode instanceof VariableDeclarator) {
						if(((VariableDeclarator)onode).getInitializer().isPresent()) {
    						Optional<Expression> oe = ((VariableDeclarator)onode).getInitializer();
    						Expression expr = oe.get();
    						sval = expr.toString();
    						ConcreteType ct = new ConcreteType();
    						ct.setValue(sval);
    						ct.setType(null);
    						System.out.println("declared value: "+sval);
    						if(sval.matches("\".*\"")) {
    							sval = sval.substring(1, sval.length()-1);
    						}
						}
					}
				}
        	}
        	return ret;
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
        
        @Override
        public void visit(DoubleLiteralExpr n, Void arg) {
        	System.out.println("DoubleLiteralExpr: "+n.asDouble());
        }
        
        @Override
        public void visit(CharLiteralExpr n, Void arg) {
        	System.out.println("CharLiteralExpr: "+n.asChar());
        }
        
        @Override
        public void visit(IntegerLiteralExpr n, Void arg) {
        	System.out.println("IntegerLiteralExpr: "+n.asInt());
        }
        
        @Override
        public void visit(LongLiteralExpr n, Void arg) {
        	System.out.println("LongLiteralExpr: "+n.asLong());
        }
        
        @Override
        public void visit(StringLiteralExpr n, Void arg) {
        	System.out.println("StringLiteralExpr: "+n.asString());
        }
        
        
        private class ConcreteType {
        	private String type;
        	private String value;
        	private String[] altvalues;
        	private int altidx;
        	public ConcreteType() {
        		type = null;
        		value = null;
        		altvalues = new String[0];
        		altidx = 0;
        	}
        	public String getType() {
        		return type;
        	}
        	public String getValue() {
        		return value;
        	}
        	public String[] getAlternates() {
        		return altvalues;
        	}
        	public void findType() {
        		if(value != null) {
        			if(value.matches("^\"\\w.+\"$")) {
        				if(this.type == null) this.type = "String";
        				this.altvalues = new String[this.altvalues.length+1];
        				this.altvalues[altidx++] = "String";
        			}
        			else if(value.length() > 9 && value.matches("\\d*\\.\\d+") && value.charAt(0) != ('-' | '+')) {
        				if(this.type == null) this.type = "double";
        				this.altvalues = new String[this.altvalues.length+2];
        				this.altvalues[altidx++] = "double";
        				this.altvalues[altidx++] = "float";
    				}
    				else if(value.matches("^[-]?\\d*\\.\\d+E\\d+$")) {
    					if(this.type == null) this.type = "double";
        				this.altvalues = new String[this.altvalues.length+2];
        				this.altvalues[altidx++] = "double";
        				this.altvalues[altidx++] = "float";
    				}
    				else if(value.matches("^\\d+\\.\\d*E\\d+$")) {
    					if(this.type == null) this.type = "double";
        				this.altvalues = new String[this.altvalues.length+2];
        				this.altvalues[altidx++] = "double";
        				this.altvalues[altidx++] = "float";
    				}
        			else if(value.matches("^\\d*\\.\\d+$")) {
        				if(this.type == null) this.type = "double";
        				this.altvalues = new String[this.altvalues.length+2];
        				this.altvalues[altidx++] = "double";
        				this.altvalues[altidx++] = "float";
        			}
        			else if(value.matches("^.+f$")) {
        				if(this.type == null) this.type = "float";
        				this.altvalues = new String[this.altvalues.length+2];
                        this.altvalues[altidx++] = "float";
        				this.altvalues[altidx++] = "double";
       			    }
        			else if(value.matches("^\\d*\\.\\df$")) {
        				if(this.type == null) this.type = "float";
        				this.altvalues = new String[this.altvalues.length+2];
        				this.altvalues[altidx++] = "float";
        				this.altvalues[altidx++] = "double";
       			}
        			else if(value.matches("^\\d+$")) {
        				if(this.type == null) this.type = "int";
        				this.altvalues = new String[this.altvalues.length+3];
        				this.altvalues[altidx++] = "int";
        				this.altvalues[altidx++] = "byte";
        				this.altvalues[altidx++] = "char";
       			}
        			else if(value.matches("^0x\\d+$")) {
        				if(this.type == null) this.type = "byte";
        				this.altvalues = new String[this.altvalues.length+3];
        				this.altvalues[altidx++] = "byte";
        				this.altvalues[altidx++] = "int";
        				this.altvalues[altidx++] = "char";
        			}
        			else if(value.matches("^[0][0-7]+$")) {
        				if(this.type == null) this.type = "char";
        				this.altvalues = new String[this.altvalues.length+3];
        				this.altvalues[altidx++] = "char";
        				this.altvalues[altidx++] = "int";
        				this.altvalues[altidx++] = "byte";
        			}
        			else if(value.matches("^\'.\'$") && value.length() == 3) {
        				if(this.type == null) this.type = "char";
        				this.altvalues = new String[this.altvalues.length+3];
        				this.altvalues[altidx++] = "char";
        				this.altvalues[altidx++] = "int";
        				this.altvalues[altidx++] = "byte";
        			}
        		}
        	}
        	public void setType(String t) {
        		type = t;
        	}
        	public void setValue(String v) {
        		value = v;
        	}
        	public void setValue(int i) {
        		value = new Integer(i).toString();
        	}
        	public void setValue(short s) {
        		value = new Short(s).toString();
        	}
        	public void setValue(double d) {
        		value = new Double(d).toString();
        	}
        	public void setValue(float f) {
        		value = new Float(f).toString();
        	}
        	public void setValue(byte b) {
        		value = new Byte(b).toString();
        	}
        	public void setValue(char c) {
        		value = new Character(c).toString();
        	}
        	public void setValue(long l) {
        		value = new Long(l).toString();
        	}
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

