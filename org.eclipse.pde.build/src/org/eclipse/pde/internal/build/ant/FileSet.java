package org.eclipse.pde.internal.build.ant;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import java.io.PrintWriter;

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

public FileSet(String dir, String defaultexcludes, String includes, String includesfile, String excludes, String excludesfile, String casesensitive) {
	this.dir = dir;
	this.defaultexcludes = defaultexcludes;
	this.includes = includes;
	this.includesfile = includesfile;
	this.excludes = excludes;
	this.excludesfile = excludesfile;
	this.casesensitive = casesensitive;
}

protected void print(AntScript script, int tab) {
	script.printTab(tab);
	script.print("<fileset"); //$NON-NLS-1$
	script.printAttribute("dir", dir, true); //$NON-NLS-1$
	script.printAttribute("defaultexcludes", defaultexcludes, false); //$NON-NLS-1$
	script.printAttribute("includes", includes, false); //$NON-NLS-1$
	script.printAttribute("includesfile", includesfile, false); //$NON-NLS-1$
	script.printAttribute("excludes", excludes, false); //$NON-NLS-1$
	script.printAttribute("excludesfile", excludesfile, false); //$NON-NLS-1$
	script.printAttribute("casesensitive", casesensitive, false); //$NON-NLS-1$
	script.println("/>"); //$NON-NLS-1$
}
}