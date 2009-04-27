/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.ant;

import org.apache.tools.ant.*;
import org.eclipse.pde.internal.core.exports.FeatureExportOperation;
import org.eclipse.pde.internal.core.exports.ProductExportOperation;

public class ExportBuildListener implements BuildListener {

	/* (non-Javadoc)
	 * @see org.apache.tools.ant.BuildListener#buildStarted(org.apache.tools.ant.BuildEvent)
	 */
	public void buildStarted(BuildEvent event) {
	}

	/* (non-Javadoc)
	 * @see org.apache.tools.ant.BuildListener#buildFinished(org.apache.tools.ant.BuildEvent)
	 */
	public void buildFinished(BuildEvent event) {
	}

	/* (non-Javadoc)
	 * @see org.apache.tools.ant.BuildListener#targetStarted(org.apache.tools.ant.BuildEvent)
	 */
	public void targetStarted(BuildEvent event) {
	}

	/* (non-Javadoc)
	 * @see org.apache.tools.ant.BuildListener#targetFinished(org.apache.tools.ant.BuildEvent)
	 */
	public void targetFinished(BuildEvent event) {
	}

	/* (non-Javadoc)
	 * @see org.apache.tools.ant.BuildListener#taskStarted(org.apache.tools.ant.BuildEvent)
	 */
	public void taskStarted(BuildEvent event) {
	}

	/* (non-Javadoc)
	 * @see org.apache.tools.ant.BuildListener#taskFinished(org.apache.tools.ant.BuildEvent)
	 */
	private static final String RUN_DIRECTOR = "runDirector"; //$NON-NLS-1$
	private static final String DIRECTOR_OUTPUT = "p2.director.java.output"; //$NON-NLS-1$

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

	/* (non-Javadoc)
	 * @see org.apache.tools.ant.BuildListener#messageLogged(org.apache.tools.ant.BuildEvent)
	 */
	public void messageLogged(BuildEvent event) {
		if (event.getPriority() == Project.MSG_ERR) {
			FeatureExportOperation.errorFound();
		}
	}

}
