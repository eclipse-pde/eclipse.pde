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
	protected String srcdir;
	protected String verbose;

public JavacTask() {
}

public void print(AntScript script, int tab) {
	script.printTab(tab);
	script.print("<javac");
	script.println();
	script.printTab(++tab);
	script.printAttribute("srcdir", srcdir, false);
	script.println();
	script.printTab(tab);
	script.printAttribute("destdir", destdir, false);
	script.println();
	script.printTab(tab);
	script.printAttribute("failonerror", failonerror, false);
	script.println();
	script.printTab(tab);
	script.printAttribute("verbose", verbose, false);
	script.println();
	script.printTab(tab--);
	script.printAttribute("classpath", classpath, false);
	script.println(">");
	script.printEndTag(tab, "javac");
}
	
	/**
	 * Sets the classpath.
	 * @param classpath The classpath to set
	 */
	public void setClasspath(String classpath) {
		this.classpath = classpath;
	}

	
	/**
	 * Sets the destdir.
	 * @param destdir The destdir to set
	 */
	public void setDestdir(String destdir) {
		this.destdir = destdir;
	}

	
	/**
	 * Sets the failonerror.
	 * @param failonerror The failonerror to set
	 */
	public void setFailOnError(String failonerror) {
		this.failonerror = failonerror;
	}

	
	/**
	 * Sets the srcdir.
	 * @param srcdir The srcdir to set
	 */
	public void setSrcdir(String srcdir) {
		this.srcdir = srcdir;
	}

	
	/**
	 * Sets the verbose.
	 * @param verbose The verbose to set
	 */
	public void setVerbose(String verbose) {
		this.verbose = verbose;
	}

}
