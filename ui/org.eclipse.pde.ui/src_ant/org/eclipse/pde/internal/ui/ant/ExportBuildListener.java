/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.ant;

import java.io.PrintWriter;

import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildListener;
import org.apache.tools.ant.Project;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.wizards.exports.*;

/**
 * @author wassimm
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class ExportBuildListener implements BuildListener {
	
	PrintWriter writer = null;

	/* (non-Javadoc)
	 * @see org.apache.tools.ant.BuildListener#buildStarted(org.apache.tools.ant.BuildEvent)
	 */
	public void buildStarted(BuildEvent event) {
		writer = FeatureExportJob.getWriter();
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
	public void taskFinished(BuildEvent event) {
	}

	/* (non-Javadoc)
	 * @see org.apache.tools.ant.BuildListener#messageLogged(org.apache.tools.ant.BuildEvent)
	 */
	public void messageLogged(BuildEvent event) {
		if (writer != null && event.getPriority() == Project.MSG_ERR) {
			if (event.getMessage() != null)
				writer.println(
					PDEPlugin.getResourceString("ExportWizard.build.error.error")
						+ " "
						+ event.getMessage());
			if (event.getProject() != null && event.getProject().getName() != null)
				writer.println(
					"\t"
						+ PDEPlugin.getResourceString("ExportWizard.build.error.project")
						+ " "
						+ event.getProject().getName());
			if (event.getTarget() != null && event.getTarget().getName() != null)
				writer.println(
					"\t"
						+ PDEPlugin.getResourceString("ExportWizard.build.error.target")
						+ " "
						+ event.getTarget().getName());
			if (event.getException() != null
				&& event.getException().getMessage() != null)
				writer.println("\t" + event.getException().getMessage());
		}
	}

}
