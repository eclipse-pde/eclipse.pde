/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
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
import org.eclipse.internal.provisional.equinox.p2.jarprocessor.JarProcessor;
import org.eclipse.internal.provisional.equinox.p2.jarprocessor.JarProcessorExecutor;

public class AntBasedProcessorExecutor extends JarProcessorExecutor {
	private final Project project;
	private final Properties signArguments;
	private final String antTaskName;

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
