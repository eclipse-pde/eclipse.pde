
package org.eclipse.pde.internal.codegen;

import java.io.PrintWriter;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.pde.internal.codegen.*;

public class StructuredJavaCodeGenerator extends JavaCodeGenerator {

	/**
	 * Constructor for StructuredJavaCodeGenerator.
	 * @param project
	 * @param qualifiedClassName
	 */
	public StructuredJavaCodeGenerator(
		IProject project,
		String qualifiedClassName) {
		super(project, qualifiedClassName);
	}

	/**
	 * Constructor for StructuredJavaCodeGenerator.
	 * @param project
	 * @param sourceFolder
	 * @param qualifiedClassName
	 */
	public StructuredJavaCodeGenerator(
		IProject project,
		IFolder sourceFolder,
		String qualifiedClassName) {
		super(project, sourceFolder, qualifiedClassName);
	}

	/*
	 * @see JavaCodeGenerator#generateContents(String, String, PrintWriter)
	 */
	public void generateContents(
		String packageName,
		String className,
		PrintWriter writer) {
	}

}
