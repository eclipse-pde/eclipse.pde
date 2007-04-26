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
import java.util.List;
import java.util.Properties;
import org.eclipse.update.internal.jarprocessor.SignCommandStep;

public class UnsignCommand extends SignCommandStep {

	public UnsignCommand(Properties options, String command, boolean verbose) {
		super(options, command, verbose);
	}

	public File postProcess(File input, File workingDirectory, List containers) {
		if (command != null && input != null && shouldSign(input, containers)) {
			execute(input);
		}
		return null;
	}

	private void execute(File input) {
		Unsigner jarUnsigner = new Unsigner();
		jarUnsigner.setJar(input);
		jarUnsigner.setKeepManifestEntries(false);
		jarUnsigner.execute();
	}
}
