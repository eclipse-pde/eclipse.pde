/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.jarprocessor;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.SignJar;
import org.eclipse.update.internal.jarprocessor.SignCommandStep;

public class AntSignCommand extends SignCommandStep {
	private Project project;
	private Properties jarSignerArguments;
	private String antTaskName;

	public AntSignCommand(Properties options, Properties signArguments, Project project, String antTaskName, String command, boolean verbose) {
		super(options, command, verbose);
		this.project = project;
		this.jarSignerArguments = signArguments;
		this.antTaskName = antTaskName;
	}

	public File postProcess(File input, File workingDirectory, List containers) {
		if (command != null && input != null && shouldSign(input, containers)) {
			execute(input);
		}
		return null;
	}

	private void execute(File input) {
		try {
			SignJar jarSigner = new SignJar();
			jarSigner.setJar(input);
			jarSigner.setAlias(jarSignerArguments.getProperty(JarProcessorTask.ALIAS));
			jarSigner.setKeystore(jarSignerArguments.getProperty(JarProcessorTask.KEYSTORE));
			jarSigner.setStorepass(jarSignerArguments.getProperty(JarProcessorTask.STOREPASS));
			jarSigner.setProject(project);
			jarSigner.setTaskName(antTaskName);
			jarSigner.execute();
		} catch (BuildException e) {
			if (e.getCause() instanceof IOException) {
				throw new BuildException("The jarsigner could not be found. Make sure to run with the build with a JDK.", e); //$NON-NLS-1$
			}
			throw e;
		}
	}
}
