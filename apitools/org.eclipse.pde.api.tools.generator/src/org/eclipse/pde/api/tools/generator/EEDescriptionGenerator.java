/*******************************************************************************
 * Copyright (c) 2011, 2014 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.generator;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

public class EEDescriptionGenerator implements IApplication {

	/**
	 * Runs the application to generate the EE description
	 */
	@Override
	public Object start(IApplicationContext context) throws Exception {
		Object arguments = context.getArguments().get(IApplicationContext.APPLICATION_ARGS);
		if (arguments instanceof String[]) {
			String[] args = (String[]) arguments;
			EEGenerator.main(args);
		}
		return IApplication.EXIT_OK;
	}

	@Override
	public void stop() {
		// do nothing
	}
}
