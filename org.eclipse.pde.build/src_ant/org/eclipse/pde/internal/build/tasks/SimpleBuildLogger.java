/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build.tasks;

import java.io.PrintStream;
import org.apache.tools.ant.BuildEvent;
import org.apache.tools.ant.BuildLogger;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.internal.build.BundleHelper;
import org.eclipse.pde.internal.build.IPDEBuildConstants;

/**
 * A simple logger for an AntRunner that logs an exception (if
 * one occurs) during a build. No messages are logged otherwise.
 *
 */
public class SimpleBuildLogger implements BuildLogger, IPDEBuildConstants {

	/**
	 * Overwrite the DefaultLogger implementation to log
	 * an exception only if one occured. 
	 */
	public void buildFinished(BuildEvent event) {
		Throwable exception = event.getException();
		
		if(exception != null) {
			String message = NLS.bind(TaskMessages.error_runningRetrieve, exception.getMessage());
			BundleHelper.getDefault().getLog().log(new Status(IStatus.ERROR, PI_PDEBUILD, message));
		}
	}

	public void setEmacsMode(boolean emacsMode) {
		// Do nothing	
	}

	public void setErrorPrintStream(PrintStream err) {
		// Do nothing	
	}

	public void setMessageOutputLevel(int level) {
		// Do nothing	
	}

	public void setOutputPrintStream(PrintStream output) {
		// Do nothing	
	}

	public void buildStarted(BuildEvent event) {
		// Do nothing	
	}

	public void messageLogged(BuildEvent event) {
		// Do nothing	
	}

	public void targetFinished(BuildEvent event) {
		// Do nothing	
	}

	public void targetStarted(BuildEvent event) {
		// Do nothing	
	}

	public void taskFinished(BuildEvent event) {
		// Do nothing	
	}

	public void taskStarted(BuildEvent event) {
		// Do nothing	
	}
}
