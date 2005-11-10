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

import java.util.*;
import org.eclipse.core.runtime.CoreException;

public class AssembleScriptGenerator extends AbstractScriptGenerator {
	protected String directory; // representing the directory where to generate the file
	protected AssemblyInformation assemblageInformation;
	protected String featureId;
	protected HashMap archivesFormat;

	protected AssembleConfigScriptGenerator configScriptGenerator;

	public AssembleScriptGenerator(String directory, AssemblyInformation assemblageInformation, String featureId) {
		this.directory = directory;
		this.assemblageInformation = assemblageInformation;
		this.featureId = featureId;
		configScriptGenerator = getConfigScriptGenerator();
	}

	protected String getScriptName() {
		return DEFAULT_ASSEMBLE_NAME + '.' + (featureId.equals("") ? "" : featureId + '.') + DEFAULT_ASSEMBLE_ALL;  //$NON-NLS-1$//$NON-NLS-2$
	}

	protected AssembleConfigScriptGenerator getConfigScriptGenerator() {
		return new AssembleConfigScriptGenerator();
	}

	public void generate() throws CoreException {
		try {
			openScript(directory, getScriptName());
			printProjectDeclaration();
			generateMainTarget();
			script.printProjectEnd();
		} finally {
			script.close();
			script = null;
		}
	}

	protected void printProjectDeclaration() {
		script.printProjectDeclaration("Assemble All Config of " + featureId, TARGET_MAIN, null); //$NON-NLS-1$
	}
	
	protected void generateMainTarget() throws CoreException {
		script.printTargetDeclaration(TARGET_MAIN, null, null, null, null);
		for (Iterator iter = getConfigInfos().iterator(); iter.hasNext();)
			generateAssembleConfigFileTargetCall((Config) iter.next());
		script.printTargetEnd();
	}

	protected void generateAssembleConfigFileTargetCall(Config aConfig) throws CoreException {
		// generate the script for a configuration
		configScriptGenerator.initialize(directory, featureId, aConfig, assemblageInformation.getCompiledPlugins(aConfig), assemblageInformation.getCompiledFeatures(aConfig), assemblageInformation.getFeatures(aConfig), assemblageInformation.getRootFileProviders(aConfig));
		configScriptGenerator.setArchiveFormat((String) archivesFormat.get(aConfig));
		configScriptGenerator.setBuildSiteFactory(siteFactory);
		configScriptGenerator.generate();

		Map params = new HashMap(1);
		params.put("assembleScriptName", configScriptGenerator.getTargetName() + ".xml"); //$NON-NLS-1$ //$NON-NLS-2$
		script.printAntTask(Utils.getPropertyFormat(DEFAULT_CUSTOM_TARGETS), null, configScriptGenerator.getTargetName(), null, null, params);
	}

	public void setSignJars(boolean value) {
		configScriptGenerator.setSignJars(value);
	}

	public void setProduct(String value) {
		configScriptGenerator.setProduct(value);
	}

	public void setGenerateJnlp(boolean value) {
		configScriptGenerator.setGenerateJnlp(value);
	}

	public void setArchivesFormat(HashMap outputFormat) {
		archivesFormat = outputFormat;
	}
}
