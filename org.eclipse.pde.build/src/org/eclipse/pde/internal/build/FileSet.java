package org.eclipse.pde.internal.core;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import java.io.PrintWriter;

/**
 * Represents an Ant fileset.
 */
public class FileSet {
	protected void printQuotes(PrintWriter output, String str) {
	output.print("\"");
	output.print(str);
	output.print("\"");
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

	protected void printTab(PrintWriter output, int n) {
	for (int i = 0; i < n; i++)
		output.print("\t");
}

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

public void print(PrintWriter output, int tab) {
	printTab(output, tab);
	output.print("<fileset");
	printAttribute(output, "dir", dir, true);
	printAttribute(output, "defaultexcludes", defaultexcludes, false);
	printAttribute(output, "includes", includes, false);
	printAttribute(output, "includesfile", includesfile, false);
	printAttribute(output, "excludes", excludes, false);
	printAttribute(output, "excludesfile", excludesfile, false);
	printAttribute(output, "casesensitive", casesensitive, false);
	output.println("/>");
}
}