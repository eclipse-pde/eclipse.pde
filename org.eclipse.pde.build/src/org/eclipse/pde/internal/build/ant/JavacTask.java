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

/**
 *
 */
public class JavacTask implements ITask {
	
	protected String classpath;
	protected String bootclasspath;
	protected String destdir;
	protected String failonerror;
	protected String[] srcdir;
	protected String verbose;
	protected String includeAntRuntime;
	protected String fork;
	protected String debug;

public JavacTask() {
}

public void print(AntScript script, int tab) {
	script.printTab(tab);
	script.print("<javac"); //$NON-NLS-1$
	script.printAttribute("destdir", destdir, false); //$NON-NLS-1$
	script.printAttribute("failonerror", failonerror, false); //$NON-NLS-1$
	script.printAttribute("verbose", verbose, false); //$NON-NLS-1$
	script.printAttribute("fork", fork, false); //$NON-NLS-1$
	script.printAttribute("debug", debug, false); //$NON-NLS-1$
	script.printAttribute("includeAntRuntime", includeAntRuntime, false); //$NON-NLS-1$
	script.printAttribute("bootclasspath", bootclasspath, false); //$NON-NLS-1$
	script.printAttribute("classpath", classpath, false); //$NON-NLS-1$
	script.println(">"); //$NON-NLS-1$
	tab++;
	for (int i = 0; i < srcdir.length; i++) {
		script.printTab(tab);
		script.print("<src path="); //$NON-NLS-1$
		script.printQuotes(srcdir[i]);
		script.println("/>"); //$NON-NLS-1$
	}
	script.printEndTag(--tab, "javac"); //$NON-NLS-1$
}

public void setClasspath(String classpath) {
	this.classpath = classpath;
}

public void setBootClasspath(String bootclasspath) {
	this.bootclasspath = bootclasspath;
}

public void setDestdir(String destdir) {
	this.destdir = destdir;
}

public void setFailOnError(String failonerror) {
	this.failonerror = failonerror;
}

public void setIncludeAntRuntime(String include) {
	this.includeAntRuntime = include;
}

public void setSrcdir(String[] srcdir) {
	this.srcdir = srcdir;
}

public void setVerbose(String verbose) {
	this.verbose = verbose;
}

public void setFork(String fork) {
	this.fork = fork;
}

public void setDebug(String debug) {
	this.debug = debug;
}
}
