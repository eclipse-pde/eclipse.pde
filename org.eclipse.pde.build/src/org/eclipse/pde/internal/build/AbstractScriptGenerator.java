package org.eclipse.pde.internal.core;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import java.io.PrintWriter;
import java.util.*;

import org.eclipse.core.runtime.CoreException;

/**
 * 
 */
public abstract class AbstractScriptGenerator implements IPDECoreConstants, IXMLConstants {



protected void printAntCallTask(PrintWriter output, int tab, String target, String inheritAll, Map params) {
	printTab(output, tab);
	output.print("<antcall");
	printAttribute(output, "target", target, true);
	printAttribute(output, "inheritAll", inheritAll, false);
	if (params == null)
		output.println("/>");
	else {
		output.println(">");
		tab++;
		Set entries = params.entrySet();
		for (Iterator iter = entries.iterator(); iter.hasNext();) {
			Map.Entry entry = (Map.Entry) iter.next();
			printParam(output, tab, (String) entry.getKey(), (String) entry.getValue());
		}
		tab--;
		printTab(output, tab);
		output.println("</antcall>");
	}
}

protected void printAntJarTask(PrintWriter output, int tab, String jarFile, String basedir) {
	printTab(output, tab);
	output.print("<jar");
	printAttribute(output, "jarfile", jarFile, true);
	printAttribute(output, "basedir", basedir, false);
	output.println("/>");
}

protected void printAntTask(PrintWriter output, int tab, String antfile, String dir, String target, String outputParam, String inheritAll, Map properties) {
	printTab(output, tab);
	output.print("<ant");
	printAttribute(output, "antfile", antfile, false);
	printAttribute(output, "dir", dir, false);
	printAttribute(output, "target", target, false);
	printAttribute(output, "output", outputParam, false);
	printAttribute(output, "inheritAll", inheritAll, false);
	if (properties == null)
		output.println("/>");
	else {
		output.println(">");
		tab++;
		Set entries = properties.entrySet();
		for (Iterator iter = entries.iterator(); iter.hasNext();) {
			Map.Entry entry = (Map.Entry) iter.next();
			printProperty(output, tab, (String) entry.getKey(), (String) entry.getValue());
		}
		tab--;
		printTab(output, tab);
		output.println("</ant>");
	}
}

protected void printAntZipTask(PrintWriter output, int tab, String zipfile, String basedir) {
	printTab(output, tab);
	output.print("<zip");
	printAttribute(output, "zipfile", zipfile, true);
	printAttribute(output, "basedir", basedir, false);
	output.println("/>");
}

protected void printArg(PrintWriter output, int tab, String line) {
	printTab(output, tab);
	output.print("<arg");
	printAttribute(output, "line", line, false);
	output.println("/>");
}

protected void printString(PrintWriter output, int tab, String string) {
	printTab(output, tab);
	output.println(string);
}

protected void printComment(PrintWriter output, int tab, String comment) {
	printTab(output, tab);
	output.print("<!-- ");
	output.print(comment);
	output.println(" -->");
}

protected void printAttribute(PrintWriter output, String name, String value, boolean mandatory) {
	if (mandatory && value == null)
		value = "";
	if (value != null) {
		output.print(" ");
		output.print(name);
		output.print("=");
		printQuotes(output, value);
	}
}

protected void printCopyTask(PrintWriter output, int tab, String file, String todir, FileSet[] fileSets) {
	printTab(output, tab);
	output.print("<copy");
	printAttribute(output, "file", file, false);
	printAttribute(output, "todir", todir, false);
	if (fileSets == null)
		output.println("/>");
	else {
		output.println(">");
		tab++;
		for (int i = 0; i < fileSets.length; i++)
			fileSets[i].print(output, tab);
		tab--;
		printTab(output, tab);
		output.println("</copy>");
	}
}

protected void printDeleteTask(PrintWriter output, int tab, String dir, String file, FileSet[] fileSets) {
	printTab(output, tab);
	output.print("<delete");
	printAttribute(output, "dir", dir, false);
	printAttribute(output, "file", file, false);
	if (fileSets == null)
		output.println("/>");
	else {
		output.println(">");
		tab++;
		for (int i = 0; i < fileSets.length; i++)
			fileSets[i].print(output, tab);
		tab--;
		printTab(output, tab);
		output.println("</delete>");
	}
}

protected void printExecTask(PrintWriter output, int tab, String executable, String dir, List lineArgs) {
	printTab(output, tab);
	output.print("<exec");
	printAttribute(output, "executable", executable, true);
	printAttribute(output, "dir", dir, false);
	if (lineArgs == null || lineArgs.size() == 0)
		output.println("/>");
	else {
		output.println(">");
		tab++;
		for (int i = 0; i < lineArgs.size(); i++)
			printArg(output, tab, (String) lineArgs.get(i));
		tab--;
		printTab(output, tab);
		output.println("</exec>");
	}
}



protected void printMkdirTask(PrintWriter output, int tab, String dir) {
	printTab(output, tab);
	output.print("<mkdir");
	printAttribute(output, "dir", dir, false);
	output.println("/>");
}

protected void printEchoTask(PrintWriter output, int tab, String message) {
	printTab(output, tab);
	output.print("<echo");
	printAttribute(output, "message", message, true);
	output.println("/>");
}

protected void printCVSTask(PrintWriter output, int tab, String command, String cvsRoot, String dest, String module, String tag, String quiet, String passFile) {
	printTab(output, tab);
	output.print("<cvs");
	printAttribute(output, "command", command, false);
	printAttribute(output, "cvsRoot", cvsRoot, false);
	printAttribute(output, "dest", dest, false);
	printAttribute(output, "package", module, false);
	printAttribute(output, "tag", tag, false);
	printAttribute(output, "quiet", quiet, false);
	printAttribute(output, "passfile", passFile, false);
	output.println("/>");
}

protected void printCVSPassTask(PrintWriter output, int tab, String cvsRoot, String password, String passFile) {
	printTab(output, tab);
	output.print("<cvspass");
	printAttribute(output, "cvsRoot", cvsRoot, true);
	printAttribute(output, "password", password, true);
	printAttribute(output, "passfile", passFile, false);
	output.println("/>");
}

protected void printParam(PrintWriter output, int tab, String name, String value) {
	printTab(output, tab);
	output.print("<param");
	printAttribute(output, "name", name, true);
	printAttribute(output, "value", value, true);
	output.println("/>");
}

protected void printProjectDeclaration(PrintWriter output, String name, String target, String basedir) {
	output.print("<project");
	printAttribute(output, "name", name, false);
	printAttribute(output, "default", target, true);
	printAttribute(output, "basedir", basedir, false);
	output.println(">");
}

protected void printProperty(PrintWriter output, int tab, String name, String value) {
	printTab(output, tab);
	output.print("<property");
	printAttribute(output, "name", name, true);
	printAttribute(output, "value", value, true);
	output.println("/>");
}

protected void printQuotes(PrintWriter output, String str) {
	output.print("\"");
	output.print(str);
	output.print("\"");
}

protected void printTab(PrintWriter output, int n) {
	for (int i = 0; i < n; i++)
		output.print("\t");
}

protected void printTargetDeclaration(PrintWriter output, int tab, String name, String depends, String ifClause, String unlessClause, String description) {
	printTab(output, tab);
	output.print("<target");
	printAttribute(output, "name", name, true);
	printAttribute(output, "depends", depends, false);
	printAttribute(output, "if", ifClause, false);
	printAttribute(output, "unless", unlessClause, false);
	printAttribute(output, "description", description, false);
	output.println(">");
}


/**
 * Starting point for script generation.
 */
public abstract void generate() throws CoreException;

protected String getPropertyFormat(String propertyName) {
	StringBuffer sb = new StringBuffer();
	sb.append(PROPERTY_ASSIGNMENT_PREFIX);
	sb.append(propertyName);
	sb.append(PROPERTY_ASSIGNMENT_SUFFIX);
	return sb.toString();
}


}
