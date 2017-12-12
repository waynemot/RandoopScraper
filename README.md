Randoop scraper is a project intended to allow quickly gathering literal values from Java source files
for use by the Randoop unit test generator tool https://github.com/randoop/randoop .  The tool scans
individual files or directory trees looking for Java source files and scans each file looking for
literal values, integers, characters, bytes, floating-point, and strings for use as literal values
during Randoop test suite creation.  The values found are then formatted for use by Randoop using
the --literals-file command-line flag.
<p>
The parsing performed on the Java source files is performed using the Javaparser library,
https://github.com/javaparser which permits extracting literal values from source with minimal
effort.  Each value found during the parsing is normalized and formatted as a string for
use in the Randoop compatible literals file creation phase.  Due to the way Javaparser
operates, certain types of literal values are treated as equivalent types despite their use in
in the Java source.  An example of this is byte, short and long values in the original Java source
are all considered to be integer values by the Javaparser library, despite these having been
defined explicitly as one of the aforementioned types.  This tool does not attempt determine
what was actually defined in the original code for these but instead checks each value to determine
what Java types it can be applicable to, and creates literal values according to this.  To do this
the tool compares the integer literal value to MAX_BYTE, MAX_SHORT and MAX_LONG as well as the minimum
values defined by Java, and assigns the value to all literal types that can support the value, e.g.
a value of 3 could be a byte, a short, a long or an int value, thus it would create one for each
type for inclusion into the literals file created for Randoop.  Similarly, the Java double and float
types also have this comparison to maximum/minimum to determine what types that particular literal
value could apply to.  The Javaparser also elides the negation operators from the literal values
it supplies to this tool, however this is mitigated by the mutation operations.
<p>
The tool also performs mutation upon the literal values found to enhance the value set and provide
more interesting values for use by Randoop.  The mutations applied are as follows:
<ul>
<li>
Integer and floating point value negation
</li>
<li>
Integer value mutation by off-by-one
</li>
Floating point reciprocal
<li>
</ul>
Each literal value found is associated with the particular Java class in which it was found,
and subclasses found within a Java program source file are considered to be part of the outer
class, so literals found within them are treated as belonging to the outer class.  Also,
redundant values are eliminated during the literals collection phase so only a single version
of any literal value is stored for a particular Java class.
<p>
During creation of the Randoop literals text file (the output of this tool) duplicate values
that are already part of the default Randoop literals set are eliminated.
<p>
Owing to the way Randoop works, this method may be suboptimal
due to the combinatoric explosion that can result when Randoop performs input selection for methods,
particularly those that require multiple literal values to be supplied.  Thus, using this tool you
should be aware of this and inspect the output manually to eliminate those values that are not
considered interesting.  Any values that are known to be required by Randoop for testing your
application should also be manually inserted into the literals file created, e.g. login and password
information for interfacing to web resources.
