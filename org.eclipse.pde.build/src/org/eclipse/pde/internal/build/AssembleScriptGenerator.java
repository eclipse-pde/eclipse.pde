/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build;

import java.io.*;
import java.util.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.build.ant.AntScript;

public class AssembleScriptGenerator extends AbstractScriptGenerator {
	private String directory; // representing the directory where to generate the file
	private AssemblyInformation assemblageInformation;
	private String featureId;

	private AssembleConfigScriptGenerator configScriptGenerator = new AssembleConfigScriptGenerator();

	public AssembleScriptGenerator(String directory, AssemblyInformation assemblageInformation, String featureId, String scriptFilename) throws CoreException {
		this.directory = directory;
		this.assemblageInformation = assemblageInformation;
		this.featureId = featureId;

		String filename = directory + '/' + (scriptFilename == null ? (DEFAULT_ASSEMBLE_NAME + "." + featureId + "." + DEFAULT_ASSEMBLE_ALL) : scriptFilename); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		try {
			script = new AntScript(new FileOutputStream(filename));
		} catch (FileNotFoundException e) {
			// ignore this exception
		} catch (IOException e) {
			String message = NLS.bind(Messages.exception_writingFile, filename);
			throw new CoreException(new Status(IStatus.ERROR, PI_PDEBUILD, EXCEPTION_WRITING_FILE, message, e));
		}
	}

	public void generate() throws CoreException {
		try {
			script.printProjectDeclaration("Assemble All Config of " + featureId, TARGET_MAIN, null); //$NON-NLS-1$
			generateMainTarget();
			script.printProjectEnd();
		} finally {
			script.close();
		}
	}

	private void generateMainTarget() throws CoreException {
		script.printTargetDeclaration(TARGET_MAIN, null, null, null, null);
		for (Iterator iter = getConfigInfos().iterator(); iter.hasNext();)
			generateAssembleConfigFileTargetCall((Config) iter.next());
		script.printTargetEnd();
	}

	protected void generateAssembleConfigFileTargetCall(Config aConfig) throws CoreException {
		// generate the script for a configuration
		configScriptGenerator.initialize(directory, null, featureId, aConfig, assemblageInformation.getPlugins(aConfig), assemblageInformation.getFeatures(aConfig), assemblageInformation.getRootFileProviders(aConfig));
		configScriptGenerator.generate();

		Map params = new HashMap(1);
		params.put("assembleScriptName", configScriptGenerator.getFilename()); //$NON-NLS-1$
		script.printAntTask(getPropertyFormat(DEFAULT_CUSTOM_TARGETS), null, configScriptGenerator.getTargetName(), null, null, params);
	}

	public void setSignJars(boolean value) {
		configScriptGenerator.setSignJars(value);
	}

	public void setGenerateJnlp(boolean value) {
		configScriptGenerator.setGenerateJnlp(value);
	}
}
