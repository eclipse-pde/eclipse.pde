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

import java.util.Properties;
import org.apache.tools.ant.Project;
import org.eclipse.update.internal.jarprocessor.JarProcessor;
import org.eclipse.update.internal.jarprocessor.JarProcessorExecutor;
import org.eclipse.update.internal.jarprocessor.Main.Options;

public class AntBasedProcessorExecutor extends JarProcessorExecutor {
	private Project project;
	private Properties signArguments;
	private String antTaskName;

	public AntBasedProcessorExecutor(Properties signArguments, Project project, String antTaskName) {
		this.signArguments = signArguments;
		this.project = project;
		this.antTaskName = antTaskName;
	}

	public void addSignStep(JarProcessor processor, Properties properties, Options options) {
		if (signArguments.get(JarProcessorTask.UNSIGN) != null)
			processor.addProcessStep(new UnsignCommand(properties, options.signCommand, options.verbose));
		if (signArguments.get(JarProcessorTask.SIGN) != null)
			processor.addProcessStep(new AntSignCommand(properties, signArguments, project, antTaskName, options.signCommand, options.verbose));
	}
}
