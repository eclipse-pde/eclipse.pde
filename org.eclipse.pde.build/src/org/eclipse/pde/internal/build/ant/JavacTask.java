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

/**
 * Wrapper class for the Ant javac task.
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

/**
 * Default constructor for the class.
 */
public JavacTask() {
}

/**
 * @see ITask#print(AntScript, int)
 */
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

/**
 * Set the javac task classpath attribute to be the given value.
 * 
 * @param classpath the classpath attribute
 */
public void setClasspath(String classpath) {
	this.classpath = classpath;
}

/**
 * Set the javac task boot classpath to be the given value.
 * 
 * @param bootclasspath the boot classpath attribute
 */
public void setBootClasspath(String bootclasspath) {
	this.bootclasspath = bootclasspath;
}

/**
 * Set the javac task destination directory to be the given value.
 * 
 * @param destdir the destination directory
 */
public void setDestdir(String destdir) {
	this.destdir = destdir;
}

/**
 * Set the javac task failOnError attribute to be the given value. Valid values
 * are <code>"true"</code> and <code>"false"</code>.
 * 
 * @param failonerror either <code>"true"</code> or <code>"false"</code>
 */
public void setFailOnError(String failonerror) {
	this.failonerror = failonerror;
}

/**
 * Set the javac task includeAntRuntime attribute to be the given value. Valid
 * values are <code>"no"</code> and <code>"yes"</code>.
 * 
 * @param include either <code>"no"</code> or <code>"yes"</code>
 */
public void setIncludeAntRuntime(String include) {
	this.includeAntRuntime = include;
}

/**
 * Set the javac task source directory attribute to be the given value.
 * 
 * @param srcdir the source directory
 */
public void setSrcdir(String[] srcdir) {
	this.srcdir = srcdir;
}

/**
 * Set the javac task verbose attribute to be the given value. Valid values
 * are <code>"true"</code> and <code>"false"</code>.
 * 
 * @param verbose either <code>"true"</code> or <code>"false"</code>
 */
public void setVerbose(String verbose) {
	this.verbose = verbose;
}

/**
 * Set the javac task fork attribute to be the given value. Valid values
 * are <code>"true"</code> and <code>"false"</code>.
 * 
 * @param fork either <code>"true"</code> or <code>"false"</code>
 */
public void setFork(String fork) {
	this.fork = fork;
}

/**
 * Set the javac task debug attribute to be the given value. Valid values
 * are <code>"on"</code> and <code>"off"</code>.
 * 
 * @param debug either <code>"on"</code> or <code>"off"</code>
 */
public void setDebug(String debug) {
	this.debug = debug;
}
}
