/*******************************************************************************
 * Copyright (c) 2007, 2013 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
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
 */
public class SimpleBuildLogger implements BuildLogger, IPDEBuildConstants {

	/**
	 * Overwrite the DefaultLogger implementation to log
	 * an exception only if one occured.
	 */
	@Override
	public void buildFinished(BuildEvent event) {
		Throwable exception = event.getException();

		if(exception != null) {
			String message = NLS.bind(TaskMessages.error_runningRetrieve, exception.getMessage());
			BundleHelper.getDefault().getLog().log(new Status(IStatus.ERROR, PI_PDEBUILD, message));
		}
	}

	@Override
	public void setEmacsMode(boolean emacsMode) {
		// Do nothing
	}

	@Override
	public void setErrorPrintStream(PrintStream err) {
		// Do nothing
	}

	@Override
	public void setMessageOutputLevel(int level) {
		// Do nothing
	}

	@Override
	public void setOutputPrintStream(PrintStream output) {
		// Do nothing
	}

	@Override
	public void buildStarted(BuildEvent event) {
		// Do nothing
	}

	@Override
	public void messageLogged(BuildEvent event) {
		// Do nothing
	}

	@Override
	public void targetFinished(BuildEvent event) {
		// Do nothing
	}

	@Override
	public void targetStarted(BuildEvent event) {
		// Do nothing
	}

	@Override
	public void taskFinished(BuildEvent event) {
		// Do nothing
	}

	@Override
	public void taskStarted(BuildEvent event) {
		// Do nothing
	}
}
