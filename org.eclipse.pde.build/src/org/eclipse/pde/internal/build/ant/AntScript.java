package org.eclipse.pde.internal.build.ant;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import java.io.*;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.*;
/**
 * Class for producing Ant scripts.
 */
public class AntScript {
	
	protected PrintWriter output;
	protected final String XML_PROLOG = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
	
public AntScript(OutputStream out) {
	output = new PrintWriter(out);
	output.println(XML_PROLOG);
}

public void close() {
	output.flush();
	output.close();
}

public void printAntCallTask(int tab, String target, String inheritAll, Map params) {
	printTab(tab);
	output.print("<antcall");
	printAttribute("target", target, true);
	printAttribute("inheritAll", inheritAll, false);
	if (params == null)
		output.println("/>");
	else {
		output.println(">");
		tab++;
		Set entries = params.entrySet();
		for (Iterator iter = entries.iterator(); iter.hasNext();) {
			Map.Entry entry = (Map.Entry) iter.next();
			printParam(tab, (String) entry.getKey(), (String) entry.getValue());
		}
		tab--;
		printTab(tab);
		output.println("</antcall>");
	}
}

public void printJarTask(int tab, String jarFile, String basedir) {
	printTab(tab);
	output.print("<jar");
	printAttribute("jarfile", jarFile, true);
	printAttribute("basedir", basedir, false);
	output.println("/>");
}

public void printAntTask(int tab, String antfile, String dir, String target, String outputParam, String inheritAll, Map properties) {
	printTab(tab);
	output.print("<ant");
	printAttribute("antfile", antfile, false);
	printAttribute("dir", dir, false);
	printAttribute("target", target, false);
	printAttribute("output", outputParam, false);
	printAttribute("inheritAll", inheritAll, false);
	if (properties == null)
		output.println("/>");
	else {
		output.println(">");
		tab++;
		Set entries = properties.entrySet();
		for (Iterator iter = entries.iterator(); iter.hasNext();) {
			Map.Entry entry = (Map.Entry) iter.next();
			printProperty(tab, (String) entry.getKey(), (String) entry.getValue());
		}
		tab--;
		printTab(tab);
		output.println("</ant>");
	}
}

public void printZipTask(int tab, String zipfile, String basedir) {
	printTab(tab);
	output.print("<zip");
	printAttribute("zipfile", zipfile, true);
	printAttribute("basedir", basedir, false);
	output.println("/>");
}

protected void printArg(int tab, String line) {
	printTab(tab);
	output.print("<arg");
	printAttribute("line", line, false);
	output.println("/>");
}

public void printString(int tab, String string) {
	printTab(tab);
	output.println(string);
}

public void printComment(int tab, String comment) {
	printTab(tab);
	output.print("<!-- ");
	output.print(comment);
	output.println(" -->");
}

protected void printAttribute(String name, String value, boolean mandatory) {
	if (mandatory && value == null)
		value = "";
	if (value != null) {
		output.print(" ");
		output.print(name);
		output.print("=");
		printQuotes(value);
	}
}

public void printCopyTask(int tab, String file, String todir, FileSet[] fileSets) {
	printTab(tab);
	output.print("<copy");
	printAttribute("file", file, false);
	printAttribute("todir", todir, false);
	if (fileSets == null)
		output.println("/>");
	else {
		output.println(">");
		tab++;
		for (int i = 0; i < fileSets.length; i++)
			fileSets[i].print(this, tab);
		tab--;
		printTab(tab);
		output.println("</copy>");
	}
}

public void printDeleteTask(int tab, String dir, String file, FileSet[] fileSets) {
	printTab(tab);
	output.print("<delete");
	printAttribute("dir", dir, false);
	printAttribute("file", file, false);
	if (fileSets == null)
		output.println("/>");
	else {
		output.println(">");
		tab++;
		for (int i = 0; i < fileSets.length; i++)
			fileSets[i].print(this, tab);
		tab--;
		printTab(tab);
		output.println("</delete>");
	}
}

public void printExecTask(int tab, String executable, String dir, List lineArgs) {
	printTab(tab);
	output.print("<exec");
	printAttribute("executable", executable, true);
	printAttribute("dir", dir, false);
	if (lineArgs == null || lineArgs.size() == 0)
		output.println("/>");
	else {
		output.println(">");
		tab++;
		for (int i = 0; i < lineArgs.size(); i++)
			printArg(tab, (String) lineArgs.get(i));
		tab--;
		printTab(tab);
		output.println("</exec>");
	}
}

public void printMkdirTask(int tab, String dir) {
	printTab(tab);
	output.print("<mkdir");
	printAttribute("dir", dir, false);
	output.println("/>");
}

public void printEchoTask(int tab, String message) {
	printTab(tab);
	output.print("<echo");
	printAttribute("message", message, true);
	output.println("/>");
}

public void printCVSTask(int tab, String command, String cvsRoot, String dest, String module, String tag, String quiet, String passFile) {
	printTab(tab);
	output.print("<cvs");
	printAttribute("command", command, false);
	printAttribute("cvsRoot", cvsRoot, false);
	printAttribute("dest", dest, false);
	printAttribute("package", module, false);
	printAttribute("tag", tag, false);
	printAttribute("quiet", quiet, false);
	printAttribute("passfile", passFile, false);
	output.println("/>");
}

public void printCVSPassTask(int tab, String cvsRoot, String password, String passFile) {
	printTab(tab);
	output.print("<cvspass");
	printAttribute("cvsRoot", cvsRoot, true);
	printAttribute("password", password, true);
	printAttribute("passfile", passFile, false);
	output.println("/>");
}

protected void printParam(int tab, String name, String value) {
	printTab(tab);
	output.print("<param");
	printAttribute("name", name, true);
	printAttribute("value", value, true);
	output.println("/>");
}

public void printProjectDeclaration(String name, String target, String basedir) {
	output.print("<project");
	printAttribute("name", name, false);
	printAttribute("default", target, true);
	printAttribute("basedir", basedir, false);
	output.println(">");
}

public void printProperty(int tab, String name, String value) {
	printTab(tab);
	output.print("<property");
	printAttribute("name", name, true);
	printAttribute("value", value, true);
	output.println("/>");
}

protected void printQuotes(String str) {
	output.print("\"");
	output.print(str);
	output.print("\"");
}

public void printStartTag(int tab, String tag) {
	printTab(tab);
	output.print("<");
	output.print(tag);
	output.println(">");
}

public void printEndTag(int tab, String tag) {
	printTab(tab);
	output.print("</");
	output.print(tag);
	output.println(">");
}

protected void printTab(int n) {
	for (int i = 0; i < n; i++)
		output.print("\t");
}

public void println(String s) {
	output.println(s);
}

public void print(String s) {
	output.print(s);
}

public void println() {
	output.println();
}

public void print(int tab, ConditionTask task) {
	task.print(this, tab);
}

public void printTargetDeclaration(int tab, String name, String depends, String ifClause, String unlessClause, String description) {
	printTab(tab);
	output.print("<target");
	printAttribute("name", name, true);
	printAttribute("depends", depends, false);
	printAttribute("if", ifClause, false);
	printAttribute("unless", unlessClause, false);
	printAttribute("description", description, false);
	output.println(">");
}
}