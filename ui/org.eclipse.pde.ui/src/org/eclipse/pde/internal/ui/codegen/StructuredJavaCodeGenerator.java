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

package org.eclipse.pde.internal.ui.codegen;

import java.io.PrintWriter;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;

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
