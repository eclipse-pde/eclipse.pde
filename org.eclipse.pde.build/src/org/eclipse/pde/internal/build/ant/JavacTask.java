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
	protected String destdir;
	protected String failonerror;
	protected String[] srcdir;
	protected String verbose;
	protected String includeAntRuntime;

public JavacTask() {
}

public void print(AntScript script, int tab) {
	script.printTab(tab);
	script.print("<javac");
	script.printAttribute("destdir", destdir, false);
	script.printAttribute("failonerror", failonerror, false);
	script.printAttribute("verbose", verbose, false);
	script.printAttribute("includeAntRuntime", includeAntRuntime, false);
	script.printAttribute("classpath", classpath, false);
	script.println(">");
	tab++;
	for (int i = 0; i < srcdir.length; i++) {
		script.printTab(tab);
		script.print("<src path=");
		script.printQuotes(srcdir[i]);
		script.println("/>");
	}
	script.printEndTag(--tab, "javac");
}

public void setClasspath(String classpath) {
	this.classpath = classpath;
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

}
