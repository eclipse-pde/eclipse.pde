/**********************************************************************
 * Copyright (c) 2002 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Common Public License v0.5
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors: 
 * IBM - Initial API and implementation
 **********************************************************************/
package org.eclipse.pde.internal.build.ant;

import java.io.*;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.*;
/**
 * Class for producing Ant scripts.
 */
public class AntScript {
	
	protected PrintWriter output;
	protected final String XML_PROLOG = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"; //$NON-NLS-1$
	
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
	output.print("<antcall"); //$NON-NLS-1$
	printAttribute("target", target, true); //$NON-NLS-1$
	printAttribute("inheritAll", inheritAll, false); //$NON-NLS-1$
	if (params == null)
		output.println("/>"); //$NON-NLS-1$
	else {
		output.println(">"); //$NON-NLS-1$
		tab++;
		Set entries = params.entrySet();
		for (Iterator iter = entries.iterator(); iter.hasNext();) {
			Map.Entry entry = (Map.Entry) iter.next();
			printParam(tab, (String) entry.getKey(), (String) entry.getValue());
		}
		tab--;
		printTab(tab);
		output.println("</antcall>"); //$NON-NLS-1$
	}
}

public void printJarTask(int tab, String jarFile, String basedir) {
	printTab(tab);
	output.print("<jar"); //$NON-NLS-1$
	printAttribute("jarfile", jarFile, true); //$NON-NLS-1$
	printAttribute("basedir", basedir, false); //$NON-NLS-1$
	output.println("/>"); //$NON-NLS-1$
}

public void printAvailableTask(int tab, String property, String file) {
	printTab(tab);
	output.print("<available"); //$NON-NLS-1$
	printAttribute("property", property, true); //$NON-NLS-1$
	printAttribute("file", file, false); //$NON-NLS-1$
	output.println("/>"); //$NON-NLS-1$
}public void printAntTask(int tab, String antfile, String dir, String target, String outputParam, String inheritAll, Map properties) {
	printTab(tab);
	output.print("<ant"); //$NON-NLS-1$
	printAttribute("antfile", antfile, false); //$NON-NLS-1$
	printAttribute("dir", dir, false); //$NON-NLS-1$
	printAttribute("target", target, false); //$NON-NLS-1$
	printAttribute("output", outputParam, false); //$NON-NLS-1$
	printAttribute("inheritAll", inheritAll, false); //$NON-NLS-1$
	if (properties == null)
		output.println("/>"); //$NON-NLS-1$
	else {
		output.println(">"); //$NON-NLS-1$
		tab++;
		Set entries = properties.entrySet();
		for (Iterator iter = entries.iterator(); iter.hasNext();) {
			Map.Entry entry = (Map.Entry) iter.next();
			printProperty(tab, (String) entry.getKey(), (String) entry.getValue());
		}
		tab--;
		printTab(tab);
		output.println("</ant>"); //$NON-NLS-1$
	}
}

public void printZipTask(int tab, String zipfile, String basedir, boolean filesOnly, FileSet[] fileSets) {
	printTab(tab);
	output.print("<zip"); //$NON-NLS-1$
	printAttribute("zipfile", zipfile, true); //$NON-NLS-1$
	printAttribute("basedir", basedir, false); //$NON-NLS-1$
	printAttribute("filesonly", filesOnly ? "true" : "false", true); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	if (fileSets == null)
		output.println("/>"); //$NON-NLS-1$
	else {
		output.println(">"); //$NON-NLS-1$
		tab++;
		for (int i = 0; i < fileSets.length; i++)
			fileSets[i].print(this, tab);
		tab--;
		printTab(tab);
		output.println("</zip>"); //$NON-NLS-1$
	}
}

protected void printArg(int tab, String line) {
	printTab(tab);
	output.print("<arg"); //$NON-NLS-1$
	printAttribute("line", line, false); //$NON-NLS-1$
	output.println("/>"); //$NON-NLS-1$
}

public void printString(int tab, String string) {
	printTab(tab);
	output.println(string);
}

public void printComment(int tab, String comment) {
	printTab(tab);
	output.print("<!-- "); //$NON-NLS-1$
	output.print(comment);
	output.println(" -->"); //$NON-NLS-1$
}

protected void printAttribute(String name, String value, boolean mandatory) {
	if (mandatory && value == null)
		value = ""; //$NON-NLS-1$
	if (value != null) {
		output.print(" "); //$NON-NLS-1$
		output.print(name);
		output.print("="); //$NON-NLS-1$
		printQuotes(value);
	}
}

public void printCopyTask(int tab, String file, String todir, FileSet[] fileSets) {
	printTab(tab);
	output.print("<copy"); //$NON-NLS-1$
	printAttribute("file", file, false); //$NON-NLS-1$
	printAttribute("todir", todir, false); //$NON-NLS-1$
	if (fileSets == null)
		output.println("/>"); //$NON-NLS-1$
	else {
		output.println(">"); //$NON-NLS-1$
		tab++;
		for (int i = 0; i < fileSets.length; i++)
			fileSets[i].print(this, tab);
		tab--;
		printTab(tab);
		output.println("</copy>"); //$NON-NLS-1$
	}
}

public void printDeleteTask(int tab, String dir, String file, FileSet[] fileSets) {
	printTab(tab);
	output.print("<delete"); //$NON-NLS-1$
	printAttribute("dir", dir, false); //$NON-NLS-1$
	printAttribute("file", file, false); //$NON-NLS-1$
	if (fileSets == null)
		output.println("/>"); //$NON-NLS-1$
	else {
		output.println(">"); //$NON-NLS-1$
		tab++;
		for (int i = 0; i < fileSets.length; i++)
			fileSets[i].print(this, tab);
		tab--;
		printTab(tab);
		output.println("</delete>"); //$NON-NLS-1$
	}
}

public void printExecTask(int tab, String executable, String dir, List lineArgs) {
	printTab(tab);
	output.print("<exec"); //$NON-NLS-1$
	printAttribute("executable", executable, true); //$NON-NLS-1$
	printAttribute("dir", dir, false); //$NON-NLS-1$
	if (lineArgs == null || lineArgs.size() == 0)
		output.println("/>"); //$NON-NLS-1$
	else {
		output.println(">"); //$NON-NLS-1$
		tab++;
		for (int i = 0; i < lineArgs.size(); i++)
			printArg(tab, (String) lineArgs.get(i));
		tab--;
		printTab(tab);
		output.println("</exec>"); //$NON-NLS-1$
	}
}

public void printMkdirTask(int tab, String dir) {
	printTab(tab);
	output.print("<mkdir"); //$NON-NLS-1$
	printAttribute("dir", dir, false); //$NON-NLS-1$
	output.println("/>"); //$NON-NLS-1$
}

public void printEchoTask(int tab, String message) {
	printTab(tab);
	output.print("<echo"); //$NON-NLS-1$
	printAttribute("message", message, true); //$NON-NLS-1$
	output.println("/>"); //$NON-NLS-1$
}

public void printCVSTask(int tab, String command, String cvsRoot, String dest, String module, String tag, String quiet, String passFile) {
	printTab(tab);
	output.print("<cvs"); //$NON-NLS-1$
	printAttribute("command", command, false); //$NON-NLS-1$
	printAttribute("cvsRoot", cvsRoot, false); //$NON-NLS-1$
	printAttribute("dest", dest, false); //$NON-NLS-1$
	printAttribute("package", module, false); //$NON-NLS-1$
	printAttribute("tag", tag, false); //$NON-NLS-1$
	printAttribute("quiet", quiet, false); //$NON-NLS-1$
	printAttribute("passfile", passFile, false); //$NON-NLS-1$
	output.println("/>"); //$NON-NLS-1$
}

public void printCVSPassTask(int tab, String cvsRoot, String password, String passFile) {
	printTab(tab);
	output.print("<cvspass"); //$NON-NLS-1$
	printAttribute("cvsRoot", cvsRoot, true); //$NON-NLS-1$
	printAttribute("password", password, true); //$NON-NLS-1$
	printAttribute("passfile", passFile, false); //$NON-NLS-1$
	output.println("/>"); //$NON-NLS-1$
}

protected void printParam(int tab, String name, String value) {
	printTab(tab);
	output.print("<param"); //$NON-NLS-1$
	printAttribute("name", name, true); //$NON-NLS-1$
	printAttribute("value", value, true); //$NON-NLS-1$
	output.println("/>"); //$NON-NLS-1$
}

public void printProjectDeclaration(String name, String target, String basedir) {
	output.print("<project"); //$NON-NLS-1$
	printAttribute("name", name, false); //$NON-NLS-1$
	printAttribute("default", target, true); //$NON-NLS-1$
	printAttribute("basedir", basedir, false); //$NON-NLS-1$
	output.println(">"); //$NON-NLS-1$
}

public void printProperty(int tab, String name, String value) {
	printTab(tab);
	output.print("<property"); //$NON-NLS-1$
	printAttribute("name", name, true); //$NON-NLS-1$
	printAttribute("value", value, true); //$NON-NLS-1$
	output.println("/>"); //$NON-NLS-1$
}

protected void printQuotes(String str) {
	output.print("\""); //$NON-NLS-1$
	output.print(str);
	output.print("\""); //$NON-NLS-1$
}

public void printStartTag(int tab, String tag) {
	printTab(tab);
	output.print("<"); //$NON-NLS-1$
	output.print(tag);
	output.println(">"); //$NON-NLS-1$
}

public void printEndTag(int tab, String tag) {
	printTab(tab);
	output.print("</"); //$NON-NLS-1$
	output.print(tag);
	output.println(">"); //$NON-NLS-1$
}

protected void printTab(int n) {
	for (int i = 0; i < n; i++)
		output.print("\t"); //$NON-NLS-1$
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

public void print(int tab, ITask task) {
	task.print(this, tab);
}

public void printTargetDeclaration(int tab, String name, String depends, String ifClause, String unlessClause, String description) {
	printTab(tab);
	output.print("<target"); //$NON-NLS-1$
	printAttribute("name", name, true); //$NON-NLS-1$
	printAttribute("depends", depends, false); //$NON-NLS-1$
	printAttribute("if", ifClause, false); //$NON-NLS-1$
	printAttribute("unless", unlessClause, false); //$NON-NLS-1$
	printAttribute("description", description, false); //$NON-NLS-1$
	output.println(">"); //$NON-NLS-1$
}

public void printRefreshLocalTask(int tab, String resource, String depth) {
	printTab(tab);
	output.print("<eclipse.refreshLocal"); //$NON-NLS-1$
	printAttribute("resource", resource, true); //$NON-NLS-1$
	printAttribute("depth", depth, false); //$NON-NLS-1$
	output.println("/>"); //$NON-NLS-1$
}

}