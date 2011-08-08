/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.ui.internal.markers;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.NotEnabledException;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;
import org.eclipse.pde.api.tools.ui.internal.IApiToolsConstants;
import org.eclipse.pde.api.tools.ui.internal.preferences.ApiErrorsWarningsConfigurationBlock;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IMarkerResolution2;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.progress.UIJob;

/**
 * Problem resolution to install EE descriptions
 * 
 * @since 1.0.400
 */
public class InstallEEDescriptionProblemResolution implements
		IMarkerResolution2 {

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IMarkerResolution#getLabel()
	 */
	public String getLabel() {
		return MarkerMessages.InstallEEDescriptionProblemResolution_0;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IMarkerResolution#run(org.eclipse.core.resources.IMarker)
	 */
	public void run(IMarker marker) {
		UIJob job  = new UIJob(MarkerMessages.DefaultApiProfileResolution_2) {
			/* (non-Javadoc)
			 * @see org.eclipse.ui.progress.UIJob#runInUIThread(org.eclipse.core.runtime.IProgressMonitor)
			 */
			public IStatus runInUIThread(IProgressMonitor monitor) {
				ICommandService commandService = (ICommandService) PlatformUI.getWorkbench().getService(ICommandService.class);
				final Command command = commandService.getCommand(ApiErrorsWarningsConfigurationBlock.P2_INSTALL_COMMAND_HANDLER);
				if (command.isHandled()) {
					IHandlerService handlerService = (IHandlerService) PlatformUI.getWorkbench().getService(IHandlerService.class);
					try {
						handlerService.executeCommand(ApiErrorsWarningsConfigurationBlock.P2_INSTALL_COMMAND_HANDLER, null);
					} catch (ExecutionException ex) {
						ApiErrorsWarningsConfigurationBlock.handleCommandException();
					} catch (NotDefinedException ex) {
						ApiErrorsWarningsConfigurationBlock.handleCommandException();
					} catch (NotEnabledException ex) {
						ApiErrorsWarningsConfigurationBlock.handleCommandException();
					} catch (NotHandledException ex) {
						ApiErrorsWarningsConfigurationBlock.handleCommandException();
					}
				}
				return Status.OK_STATUS;
			}
		};
		job.setSystem(true);
		job.schedule();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IMarkerResolution2#getDescription()
	 */
	public String getDescription() {
		return MarkerMessages.InstallEEDescriptionProblemResolution_1;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IMarkerResolution2#getImage()
	 */
	public Image getImage() {
		return ApiUIPlugin.getSharedImage(IApiToolsConstants.IMG_ELCL_SETUP_APITOOLS);
	}
}
