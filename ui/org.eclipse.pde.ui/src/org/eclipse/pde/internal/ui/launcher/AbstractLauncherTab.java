/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import org.eclipse.debug.core.*;
import org.eclipse.debug.ui.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.SWT;

/**
 * @version 	1.0
 * @author
 */
public abstract class AbstractLauncherTab extends AbstractLaunchConfigurationTab {

	protected void createStartingSpace(Composite parent, int span) {
		Label label = new Label(parent, SWT.NULL);
		GridData data = new GridData();
		data.horizontalSpan = span;
		label.setLayoutData(data);
	}

	public boolean isValid(ILaunchConfiguration config) {
		return getErrorMessage() == null;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#activated(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void activated(ILaunchConfigurationWorkingCopy workingCopy) {
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#deactivated(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void deactivated(ILaunchConfigurationWorkingCopy workingCopy) {
	}

	/**
	 * Updates the status line and the ok button depending on the status
	 */
	protected void updateStatus(IStatus status) {
		applyToStatusLine(this, status);
	}

	/**
	 * Applies the status to a dialog page
	 */
	public static void applyToStatusLine(AbstractLauncherTab tab, IStatus status) {
		String errorMessage= null;
		String warningMessage= null;
		String statusMessage= status.getMessage();
		if (statusMessage.length() > 0) {
			if (status.matches(IStatus.ERROR)) {
				errorMessage= statusMessage;
			} else if (!status.isOK()) {
				warningMessage= statusMessage;
			}
		}
		tab.setErrorMessage(errorMessage);
		tab.setMessage(warningMessage);
		tab.updateLaunchConfigurationDialog();
	}
	
	public static IStatus getMoreSevere(IStatus s1, IStatus s2) {
		return (s1.getSeverity() >= s2.getSeverity()) ? s1 : s2;
	}	
	
	public static IStatus createStatus(int severity, String message) {
		return new Status(severity, PDEPlugin.getPluginId(), severity, message, null);
	}
	
	/**
	 * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#updateLaunchConfigurationDialog()
	 */
	protected void updateLaunchConfigurationDialog() {
		super.updateLaunchConfigurationDialog();
	}
		
}
