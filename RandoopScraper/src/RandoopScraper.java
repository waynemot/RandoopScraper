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
	/** container used to store java source names */
	protected Stack<String> filelist;
	/** current class being scanned */
	volatile String currentClass;
	/** current package name of the class being scanned */
	volatile String currentPkg;

	/**
	 * Container for storing the literal values by type for each
	 * Java source file.  Key is the class name, value is a LiteralMap object.
	 */
	protected HashMap<String, LiteralMap> literalslist; // maps class to data-types/values map

	/**
	 * Create a new RandoopScraper object to perform the literal values
	 * collecting operation on the filename or path specified on the
	 * command line.
	 */
    public RandoopScraper() {
		filelist = new Stack<String>();
		currentClass = "";
		currentPkg = "";
		literalslist = new HashMap<String, LiteralMap>();
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
    	HashMap<String, LiteralMap> literalslist = getLiteralsList();
    	if(literalslist == null) {
    		literalslist = new HashMap<String, LiteralMap>();
    	}
    	String pkgClass = this.currentPkg.replaceAll(";[\\s]{0,2}$", "")+"."+this.currentClass;
    	if(!literalslist.containsKey(pkgClass)) {
    		// literalslist needs this class added
    		TreeSet<String> al = new TreeSet<String>();
    		al.add(value);
    		LiteralMap lm = new LiteralMap();
    		lm.put(type, al);
    		literalslist.put(pkgClass, lm);
    	}
    	else { // literalslist contains current class key
    		//check if this type exists yet for this class in literalsmap
			if(literalslist.get(pkgClass).containsKey(type)) {
				 // we have this data type for this class in HM, add new value
				literalslist.get(pkgClass).get(type).add(value);
			}
			else { // Don't have this type in HashMap for this class
				// create a new array list for this data type & add value to it
				TreeSet<String> al = new TreeSet<String>();
				al.add(value);
				literalslist.get(pkgClass).put(type, al);
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
				this.currentPkg = pd.get().toString().replaceAll("^package ", "").trim();
			} else {
				this.currentPkg = "";
			}
		    cu.accept(new MethodVisitor(this), null);
		    ret = true;
		}
    	return ret;
    }

    /**
     * main method for RandoopScraper application.
     * @param args the path or java source file name to scrape for literals.
     */
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
		HashMap<String, LiteralMap> literalslist = getLiteralsList();
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
			boolean hasLiterals = false;
			final String preamble = "START CLASSLITERALS\n";
			final String classpost = "END CLASSLITERALS\n";
			final String classpreamble = "CLASSNAME\n";
			final String literalslabel = "LITERALS\n";
			Set<String> class_set = literalslist.keySet();
			Iterator<String> class_iter = class_set.iterator();
			StringBuffer cbuf = new StringBuffer();
			while(ok && class_iter.hasNext()) {
				String currClass = class_iter.next();
				LiteralMap lm = literalslist.get(currClass);
				if(lm != null && !lm.isEmpty()) {
					// Have a set of literals for this currClass
					cbuf.append(preamble);
					cbuf.append(classpreamble);
					if(currClass.matches("^\\..*")) {
						currClass = currClass.replaceAll("^\\.", "");
					}
					cbuf.append(currClass);
					cbuf.append("\n");
					cbuf.append(literalslabel);
					//String classpreamble = "CLASSNAME\n"+currClass+"\nLITERALS\n";
					//try {
					//	ofs.write(preamble.getBytes());
					//	ofs.write(classpreamble.getBytes());
					//	ofs.flush();
					//} catch (IOException e1) {
					//	System.err.println("Error writing classname preamble to output file "+e1.getMessage());
					//	e1.printStackTrace();
					//	ok = false;
					//}
					Set<String> type_set = lm.keySet();
					Iterator<String> type_iter = type_set.iterator();
					while(type_iter.hasNext()) {
						String currType = type_iter.next();
						TreeSet<String> type_values = lm.get(currType);
						for(String tvalue : type_values) {
							if(!eliminateDup(currType, tvalue)) {
								cbuf.append(currType);
								cbuf.append(":");
								if(currType.equals("String")) {
									if(tvalue.contains("\"")) {
										String tmpval = tvalue.replaceAll("\"", "");
										tvalue = tmpval;
										
									}
								    cbuf.append("\"");
									cbuf.append(tvalue);
									cbuf.append("\"");
								}
								else {
								    cbuf.append(tvalue);
								}
								cbuf.append("\n");
								hasLiterals = true;
							}
						}
					}
				}
				if(hasLiterals) {
					cbuf.append(classpost);
					try {
						ofs.write(cbuf.toString().getBytes());
					} catch (IOException e1) {
						System.err.println("Error writing classliterals block output file "+e1.getMessage());
						e1.printStackTrace();
						ok = false;
					}
					hasLiterals = false;
				}
				cbuf.delete(0, cbuf.length());
			}
			if(ofs != null) {
				try {
					ofs.close();
				} catch (IOException e) {
					System.err.println("error closing: "+out.getName()+" "+e.getMessage());
					e.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * method to determine if the supplied value is already a
	 * Randoop default test input parameter.  If it is, then this
	 * returns true, indicating that the value need not be included
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
					int value_int = value.toCharArray()[0];
					if(b == value_int) {
						ret = true;
						break;
					}
				}
				else if(value.matches("^0[0-7]+")) { // octal byte case
					int dval = Integer.decode(value);
					if(dval == b) {
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
				if(value.startsWith("0")) {// hex & octal
					int tmpval = Integer.decode(value).intValue();
					if(tmpval >= Short.MIN_VALUE && 
							tmpval <= Short.MAX_VALUE &&
							tmpval == s) {
						ret = true;
						break;
					}
				}
				else if (value.matches("[a-z]{1}") && value.length() == 1) { // char literal in a short
					int tmpval = Character.getNumericValue(value.charAt(0));
					if(tmpval == s) {
						ret = true;
						break;
					}
				}
				else {
					if(Short.parseShort(value) == s) {
						ret = true;
						break;
					}
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

	/**
	 * Visitor class used to identify and collect literal values
	 * from the scanned class.
	 * @author Wayne Motycka
	 *
	 */
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
        	if(n.getValue().endsWith("f")) { // doubles and floats are same in Javaparser
        		rs.addLiteral("float", n.getValue());
        		rs.addLiteral("float", "-"+n.getValue());
        	}
        	rs.addLiteral("double", n.getValue());
        	rs.addLiteral("double", "-"+n.getValue());
        	double d = 1/n.asDouble();
            rs.addLiteral("double", Double.toString(d));// add reciprocal
			super.visit(n, arg);
        }
        
        @Override
        public void visit(CharLiteralExpr n, Void arg) {
        	//System.out.println("CharLiteralExpr: "+n.asChar());
        	rs.addLiteral("char", n.getValue());
			super.visit(n, arg);
        }
        
        @Override
        public void visit(IntegerLiteralExpr n, Void arg) {
        	//System.out.println("IntegerLiteralExpr: "+n.getValue());
       		int tmpval = n.asInt();
        	rs.addLiteral("int", Integer.toString(tmpval));
        	rs.addLiteral("int", "-"+Integer.toString(tmpval));
        	rs.addLiteral("int", Integer.toString(tmpval-1));
        	rs.addLiteral("int", Integer.toString(tmpval+1));
        	if(tmpval <= Short.MAX_VALUE && tmpval >= Short.MIN_VALUE) {
        	    rs.addLiteral("short", Integer.toString(tmpval));
        	    rs.addLiteral("short", "-"+Integer.toString(tmpval));
        	}
        	if(tmpval <= Byte.MAX_VALUE && tmpval >= Byte.MIN_VALUE) {
        	    rs.addLiteral("byte", Integer.toString(tmpval));
        	    rs.addLiteral("byte", "-"+Integer.toString(tmpval));
        	}
			super.visit(n, arg);
        }
        
        @Override
        public void visit(LongLiteralExpr n, Void arg) {
        	//System.out.println("LongLiteralExpr: "+n.asLong());
        	rs.addLiteral("long", n.getValue());
        	rs.addLiteral("long", "-"+n.getValue());
			super.visit(n, arg);
        }
        
        @Override
        public void visit(StringLiteralExpr n, Void arg) {
        	//System.out.println("StringLiteralExpr: "+n.asString());
        	rs.addLiteral("String", n.asString());
			super.visit(n, arg);
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
	protected class LiteralMap extends HashMap<String, TreeSet<String>> {
		HashMap<String, TreeSet<String>> type_vals;
		public LiteralMap() {
			type_vals = new HashMap<String, TreeSet<String>>();
		}
	}
}

