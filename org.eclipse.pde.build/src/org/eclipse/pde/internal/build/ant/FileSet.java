/*******************************************************************************
 *  Copyright (c) 2000, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.ant;

import java.util.StringTokenizer;

/**
 * Represents an Ant fileset.
 */
public class FileSet {

	protected String dir; // true
	protected String defaultexcludes;
	protected String includes;
	protected String includesfile;
	protected String excludes;
	protected String excludesfile;
	protected String casesensitive;

	/**
	 * Constructor for the file set.
	 * 
	 * @param dir
	 * @param defaultexcludes
	 * @param includes
	 * @param includesfile
	 * @param excludes
	 * @param excludesfile
	 * @param casesensitive
	 */
	public FileSet(String dir, String defaultexcludes, String includes, String includesfile, String excludes, String excludesfile, String casesensitive) {
		this.dir = dir;
		this.defaultexcludes = defaultexcludes;
		this.includes = includes;
		this.includesfile = includesfile;
		this.excludes = excludes;
		this.excludesfile = excludesfile;
		this.casesensitive = casesensitive;
	}

	/**
	 * Print this fileset to the given Ant script.
	 * 
	 * @param script the script to output to
	 */
	protected void print(AntScript script) {
		script.printTab();
		script.print("<fileset"); //$NON-NLS-1$
		script.printAttribute("dir", dir, true); //$NON-NLS-1$
		script.printAttribute("defaultexcludes", defaultexcludes, false); //$NON-NLS-1$
		script.printAttribute("includesfile", includesfile, false); //$NON-NLS-1$
		script.printAttribute("excludesfile", excludesfile, false); //$NON-NLS-1$
		script.printAttribute("casesensitive", casesensitive, false); //$NON-NLS-1$
		script.print(">"); //$NON-NLS-1$
		script.println();

		if (includes != null)
			printNames(script, "include", includes); //$NON-NLS-1$
		if (excludes != null)
			printNames(script, "exclude", excludes); //$NON-NLS-1$
		script.println("</fileset>"); //$NON-NLS-1$
	}

	protected void printAs(String name, AntScript script) {
		script.printTab();
		script.print("<" + name); //$NON-NLS-1$
		script.printAttribute("dir", dir, true); //$NON-NLS-1$
		script.printAttribute("defaultexcludes", defaultexcludes, false); //$NON-NLS-1$
		script.printAttribute("includesfile", includesfile, false); //$NON-NLS-1$
		script.printAttribute("excludesfile", excludesfile, false); //$NON-NLS-1$
		script.printAttribute("casesensitive", casesensitive, false); //$NON-NLS-1$
		script.printAttribute("includes", includes, false); //$NON-NLS-1$
		script.printAttribute("excludes", excludes, false); //$NON-NLS-1$
		script.println("/>"); //$NON-NLS-1$
	}

	private void printNames(AntScript script, String tag, String names) {
		script.indent++;
		for (StringTokenizer tokenizer = new StringTokenizer(names, ","); tokenizer.hasMoreTokens();) { //$NON-NLS-1$
			script.printTabs();
			script.print("<"); //$NON-NLS-1$
			script.print(tag);
			script.printAttribute("name", tokenizer.nextToken().trim(), true); //$NON-NLS-1$
			script.print("/>"); //$NON-NLS-1$
			script.println();
		}
		script.indent--;
	}
}
