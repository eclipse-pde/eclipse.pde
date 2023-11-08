/*******************************************************************************
 * Copyright (c) 2019 Red Hat Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * - Mickael Istria (Red Hat Inc.)
 *******************************************************************************/
package org.eclipse.pde.api.tools.internal;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;

/**
 * Prefer using the tycho-apitools-plugin to generate the api description file
 *
 */
@Deprecated
public class ApiFileGenerationApplication implements IApplication {

	private static String DEPRECATION_WARNING = "DEPRECATED, PLEASE MOVE TO THE tycho-apitools-plugin TO GENERATE THE API DESCRIPTION FILE"; //$NON-NLS-1$
	@Override
	public Object start(IApplicationContext context) throws Exception {
		ApiPlugin.logErrorMessage(DEPRECATION_WARNING);
		APIFileGenerator generator = new APIFileGenerator();
		String[] args = (String[]) context.getArguments().get(IApplicationContext.APPLICATION_ARGS);
		generator.projectName = find("projectName", args); //$NON-NLS-1$
		generator.projectLocation = find("project", args); //$NON-NLS-1$
		generator.binaryLocations = find("binary", args); //$NON-NLS-1$
		generator.targetFolder = find("target", args); //$NON-NLS-1$
		try {
			generator.generateAPIFile();
			return 0;
		} catch (Exception ex) {
			ApiPlugin.log(ex);
			return 1;
		}
	}

	private String find(String argName, String[] args) {
		if (argName == null || argName.isEmpty()) {
			return null;
		}
		String token = argName;
		if (!argName.startsWith("-")) { //$NON-NLS-1$
			token = "-" + argName; //$NON-NLS-1$
		}
		int tokenIndex = -1;
		for (int i = 0; i < args.length && tokenIndex == -1; i++) {
			if (token.equals(args[i])) {
				tokenIndex = i + 1;
			}
		}
		if (tokenIndex >= 0 && tokenIndex < args.length) {
			return args[tokenIndex];
		}
		return null;
	}

	@Override
	public void stop() {
		// Nothing to do
	}

}
