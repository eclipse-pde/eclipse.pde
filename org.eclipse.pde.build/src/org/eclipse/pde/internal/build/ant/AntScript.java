/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.ant;

import java.io.*;
import java.util.*;

/**
 * Class for producing Ant scripts. Contains convenience methods for creating the
 * XML elements required for Ant scripts. See the <a href="http://jakarta.apache.org/ant">Ant</a> 
 * website for more details on Ant scripts and the particular Ant tasks.
 */
public class AntScript {
	
	protected PrintWriter output;
	protected final String XML_PROLOG = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"; //$NON-NLS-1$

/**
 * Constructor for the class.
 * 
 * @param out the output stream to write the script to
 * @throws IOException
 */
public AntScript(OutputStream out) throws IOException {
	output = new PrintWriter(new OutputStreamWriter(out, "UTF8")); //$NON-NLS-1$
	output.println(XML_PROLOG);
}

/**
 * Close the output stream.
 */
public void close() {
	output.flush();
	output.close();
}

/**
 * Print an <code>antcall</code> task to the script. This calls Ant on the given 
 * target which is located within the same build file. 
 * 
 * @param tab the number of tabs to indent the task
 * @param target the target of the ant call
 * @param inheritAll <code>true</code> if the parameters should be pass to the
 * 	called target
 * @param params table of parameters for the call
 */
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

/**
 * Print a <code>jar</code> Ant task to this script. This jars together a group of 
 * files into a single file.
 * 
 * @param tab the number of tabs to indent
 * @param jarFile the destination file name
 * @param basedir the base directory
 */
public void printJarTask(int tab, String jarFile, String basedir) {
	printTab(tab);
	output.print("<jar"); //$NON-NLS-1$
	printAttribute("jarfile", jarFile, true); //$NON-NLS-1$
	printAttribute("basedir", basedir, false); //$NON-NLS-1$
	output.println("/>"); //$NON-NLS-1$
}

/**
 * Print the <code>available</code> Ant task to this script. This task sets a property
 * value if the given file exists at runtime.
 * 
 * @param tab the number of tabs to indent
 * @param property the property to set
 * @param file the file to look for
 */
public void printAvailableTask(int tab, String property, String file) {
	printTab(tab);
	output.print("<available"); //$NON-NLS-1$
	printAttribute("property", property, true); //$NON-NLS-1$
	printAttribute("file", file, false); //$NON-NLS-1$
	output.println("/>"); //$NON-NLS-1$
}

/**
 * Print an <code>ant</code> task to this script. This calls Ant on the specified 
 * target contained in the specified Ant file with the given parameters.
 * 
 * @param tab the number of tabs to indent
 * @param antfile the name of the Ant file which contains the target to run
 * @param dir the basedir for the target
 * @param target the name of the target
 * @param outputParam filename to write the output to
 * @param inheritAll <code>true</code> if the parameters should be passed on
 * 	to the ant target
 * @param properties the table of properties
 */
public void printAntTask(int tab, String antfile, String dir, String target, String outputParam, String inheritAll, Map properties) {
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

/**
 * Print a <code>zip</code> task to this script.
 * 
 * @param tab the number of tabs to indent
 * @param zipfile the destination file name
 * @param basedir the source directory to start the zip
 * @param filesOnly <code>true</code> if the resulting zip file should contain
 *   only files and not directories
 * @param excludes files which will be excluded from the zip
 * @param update <code>true</code> if the zip file should be updated 
 *     and <code>false</code> otherwise
 * @param fileSets the inclusion/exclusion rules to use when zipping
 */
public void printZipTask(int tab, String zipfile, String basedir, boolean filesOnly, String excludes, boolean update, FileSet[] fileSets) {
	printTab(tab);
	output.print("<zip"); //$NON-NLS-1$
	printAttribute("zipfile", zipfile, true); //$NON-NLS-1$
	printAttribute("basedir", basedir, false); //$NON-NLS-1$
	printAttribute("filesonly", filesOnly ? "true" : "false", true); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	printAttribute("update", update ? "yes" : "no", true); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	printAttribute("excludes", excludes, false); //$NON-NLS-1$ 
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

/**
 * Print an <code>arg</code> element to the Ant file.
 * 
 * @param tab the number of tabs to indent
 * @param line
 */
protected void printArg(int tab, String line) {
	printTab(tab);
	output.print("<arg"); //$NON-NLS-1$
	printAttribute("line", line, false); //$NON-NLS-1$
	output.println("/>"); //$NON-NLS-1$
}

/**
 * Print the given string to the Ant script.
 * 
 * @param tab the number of tabs to indent
 * @param string the string to write to the file
 */
public void printString(int tab, String string) {
	printTab(tab);
	output.println(string);
}

/**
 * Print the given comment to the Ant script.
 * 
 * @param tab the number of tabs to indent
 * @param comment the comment to write out
 */
public void printComment(int tab, String comment) {
	printTab(tab);
	output.print("<!-- "); //$NON-NLS-1$
	output.print(comment);
	output.println(" -->"); //$NON-NLS-1$
}

/**
 * Add the given name/value attribute pair to the script. Do not write the attribute
 * if the value is <code>null</code> unless a <code>true</code> is specified
 * indicating that it is mandatory.
 * 
 * @param name the name of the attribute
 * @param value the value of the attribute or <code>null</code>
 * @param mandatory <code>true</code> if the attribute should be printed even
 *   if it is <code>null</code>
 */
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

/**
 * Print a <code>copy</code> task to the script. The source file is specified 
 * by the <code>file</code> parameter. The destination directory is specified by 
 * the <code>todir</code> parameter. 
 * 
 * @param tab the number of tabs to indent
 * @param file the source file
 * @param todir the destination directory
 * @param fileSets the inclusion/exclusion rules to use when copying
 */
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

/**
 * Print a <code>delete</code> task to the Ant script. At least one of <code>dir</code>
 * or <code>file</code> is required unless some <code>fileSets</code> are
 * present.
 * 
 * @param tab the number of tabs to indent
 * @param dir the name of the directory to delete
 * @param file the name of the file to delete
 * @param fileSets the specification for the files to delete
 */
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

/**
 * Print an <code>exec</code> task to the Ant script.
 * 
 * @param tab the number of tabs to indent
 * @param executable the program to execute
 * @param dir the working directory for the executable
 * @param lineArgs the arguments for the executable
 */
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

/**
 * Print a <code>mkdir</code> task to the Ant script.
 * 
 * @param tab the number of tabs to indent
 * @param dir the name of the directory to create.
 */
public void printMkdirTask(int tab, String dir) {
	printTab(tab);
	output.print("<mkdir"); //$NON-NLS-1$
	printAttribute("dir", dir, false); //$NON-NLS-1$
	output.println("/>"); //$NON-NLS-1$
}

/**
 * Print an <code>echo</code> task to the Ant script.
 * 
 * @param tab the number of tabs to indent
 * @param message the message to echo to the output
 */
public void printEchoTask(int tab, String message) {
	printTab(tab);
	output.print("<echo"); //$NON-NLS-1$
	printAttribute("message", message, true); //$NON-NLS-1$
	output.println("/>"); //$NON-NLS-1$
}

/**
 * Print a <code>cvs</code> task to the Ant script.
 * 
 * @param tab the number of tabs to indent
 * @param command the CVS command to run
 * @param cvsRoot value for the CVSROOT variable
 * @param dest the destination directory for the checked out resources
 * @param module the module name to check out
 * @param tag the tag of the module to check out
 * @param quiet whether or not to print informational messages to the output
 * @param passFile the name of the password file
 */
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

/**
 * Print a <code>cvspass</code> task to the Ant script.
 * 
 * @param tab the number of tabs to indent
 * @param cvsRoot the name of the repository
 * @param password the password
 * @param passFile the name of the password file
 */
public void printCVSPassTask(int tab, String cvsRoot, String password, String passFile) {
	printTab(tab);
	output.print("<cvspass"); //$NON-NLS-1$
	printAttribute("cvsRoot", cvsRoot, true); //$NON-NLS-1$
	printAttribute("password", password, true); //$NON-NLS-1$
	printAttribute("passfile", passFile, false); //$NON-NLS-1$
	output.println("/>"); //$NON-NLS-1$
}

/**
 * Print a <code>param</code> tag to the Ant script.
 * 
 * @param tab the number of tabs to indent
 * @param name the parameter name
 * @param value the parameter value
 */
protected void printParam(int tab, String name, String value) {
	printTab(tab);
	output.print("<param"); //$NON-NLS-1$
	printAttribute("name", name, true); //$NON-NLS-1$
	printAttribute("value", value, true); //$NON-NLS-1$
	output.println("/>"); //$NON-NLS-1$
}

/**
 * Print a <code>project</code> tag to the Ant script.
 * 
 * @param name the name of the project
 * @param target the name of default target
 * @param basedir the base directory for all the project's path calculations
 */
public void printProjectDeclaration(String name, String target, String basedir) {
	output.print("<project"); //$NON-NLS-1$
	printAttribute("name", name, false); //$NON-NLS-1$
	printAttribute("default", target, true); //$NON-NLS-1$
	printAttribute("basedir", basedir, false); //$NON-NLS-1$
	output.println(">"); //$NON-NLS-1$
}

/**
 * Print a <code>project</code> end tag to the Ant script.
 */
public void printProjectEnd() {
	printEndTag(0, "project"); //$NON-NLS-1$
}

/**
 * Print a <code>property</code> tag to the Ant script.
 * 
 * @param tab the number of tabs to indent
 * @param name the property name
 * @param value the property value
 */
public void printProperty(int tab, String name, String value) {
	printTab(tab);
	output.print("<property"); //$NON-NLS-1$
	printAttribute("name", name, true); //$NON-NLS-1$
	printAttribute("value", value, true); //$NON-NLS-1$
	output.println("/>"); //$NON-NLS-1$
}

/**
 * Print the given string to the Ant script within quotes.
 * 
 * @param message the string to print
 */
protected void printQuotes(String message) {
	output.print("\""); //$NON-NLS-1$
	output.print(message);
	output.print("\""); //$NON-NLS-1$
}

/**
 * Print a start tag in the Ant script for the given element name.
 * 
 * @param tab the number of tabs to indent
 * @param tag the name of the element
 */
public void printStartTag(int tab, String tag) {
	printTab(tab);
	output.print("<"); //$NON-NLS-1$
	output.print(tag);
	output.println(">"); //$NON-NLS-1$
}

/**
 * Print an end tag in the Ant script for the given element name.
 * 
 * @param tab the number of tabs to indent
 * @param tag the name of the element
 */
public void printEndTag(int tab, String tag) {
	printTab(tab);
	output.print("</"); //$NON-NLS-1$
	output.print(tag);
	output.println(">"); //$NON-NLS-1$
}

/**
 * Print the given number of tabs to the Ant script.
 * 
 * @param indent the number of tabs to indent
 */
protected void printTab(int indent) {
	for (int i = 0; i < indent; i++)
		output.print("\t"); //$NON-NLS-1$
}

/**
 * Print the given string to the Ant script followed by a carriage-return.
 * 
 * @param message the string to print
 */
public void println(String message) {
	output.println(message);
}

/**
 * Print the given string to the Ant script.
 * 
 * @param message
 */
public void print(String message) {
	output.print(message);
}

/**
 * Print a carriage-return to the Ant script.
 */
public void println() {
	output.println();
}

/**
 * Print the given task to the Ant script.
 * 
 * @param tab the number of tabs to indent
 * @param task the task to print
 */
public void print(int tab, ITask task) {
	task.print(this, tab);
}

/**
 * Print a <code>target</code> tag to the Ant script.
 * 
 * @param tab the number of tabs to indent
 * @param name the name of the target
 * @param depends a comma separated list of required targets
 * @param ifClause the name of the property that this target depends on
 * @param unlessClause the name of the property that this target cannot have
 * @param description a user-readable description of this target
 */
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

/**
 * Print a closing <code>target</code> tag to the script. Indent the specified
 * number of tabs.
 * 
 * @param tab the number of tabs to use when indenting
 */
public void printTargetEnd(int tab) {
	printEndTag(tab, "target"); //$NON-NLS-1$
}

/**
 * Print a <code>eclipse.refreshLocal</code> task to the script. This task refreshes
 * the specified resource in the workspace, to the specified depth. 
 * 
 * @param tab the number of tabs to use when indenting
 * @param resource the resource to refresh
 * @param depth one of <code>IResource.DEPTH_ZERO</code>,
 *   <code>IResource.DEPTH_ONE</code>, or <code>IResource.DEPTH_INFINITY</code>
 */
public void printRefreshLocalTask(int tab, String resource, String depth) {
	printTab(tab);
	output.print("<eclipse.refreshLocal"); //$NON-NLS-1$
	printAttribute("resource", resource, true); //$NON-NLS-1$
	printAttribute("depth", depth, false); //$NON-NLS-1$
	output.println("/>"); //$NON-NLS-1$
}


/**
 * Print a <code> eclipse.convertTask</code> task to the script. This task convert a file path to 
 * an Eclipse resource or vice-versa. 
 *
 * @param tab the number of tabs to use when indenting
 * @param toConvert the entry to convert 
 * @param propertyName the property where to store the result of the convertion
 * @param isEclipseResource true if toConvert refers to an eclipse resource. 
 */
public void printConvertPathTask(int tab, String toConvert, String propertyName, boolean isEclipseResource) {
	printTab(tab);
	output.print("<eclipse.convertPath"); //$NON-NLS-1$
	// Don't use the IXMLConstants for these attributes since those constants
	// are different than the ones used to build Ant scripts.
	if (isEclipseResource)
		printAttribute("resourcePath", toConvert, true); //$NON-NLS-1$
	else
		printAttribute("fileSystemPath", toConvert, true); //$NON-NLS-1$
	printAttribute("property", propertyName, true);//$NON-NLS-1$
	output.println("/>");//$NON-NLS-1$
}
}
