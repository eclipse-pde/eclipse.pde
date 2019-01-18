/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
package org.eclipse.pde.internal.core.ant;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.Project;
import org.eclipse.pde.internal.core.exports.FeatureExportOperation;
import org.eclipse.pde.internal.core.exports.ProductExportOperation;

public class ExportBuildListener implements BuildListener {

	@Override
	public void buildStarted(BuildEvent event) {
	}

	@Override
	public void buildFinished(BuildEvent event) {
	}

	@Override
	public void targetStarted(BuildEvent event) {
	}

	@Override
	public void targetFinished(BuildEvent event) {
	}

	@Override
	public void taskStarted(BuildEvent event) {
	}

	private static final String RUN_DIRECTOR = "runDirector"; //$NON-NLS-1$
	private static final String DIRECTOR_OUTPUT = "p2.director.java.output"; //$NON-NLS-1$

	@Override
	public void taskFinished(BuildEvent event) {
		if (event.getException() != null && event.getTarget().getName().equals(RUN_DIRECTOR)) {
			String directorOutput = event.getProject().getProperty(DIRECTOR_OUTPUT);
			if (directorOutput != null) {
				int idx = directorOutput.indexOf("Installation failed."); //$NON-NLS-1$
				if (idx > -1) {
					String part2 = directorOutput.substring(idx);
					ProductExportOperation.setErrorMessage(part2);
				}
			}
		}
	}

	@Override
	public void messageLogged(BuildEvent event) {
		if (event.getPriority() == Project.MSG_ERR) {
			FeatureExportOperation.errorFound();
		}
	}

}
