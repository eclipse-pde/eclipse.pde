/*
 * Copyright (c) 2002 IBM Corp.  All rights reserved.
 * This file is made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 */
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
	private IStatus currentStatus;
	private boolean valid=true;
	private boolean changed=false;

	public AbstractLauncherTab() {
		currentStatus= createStatus(IStatus.OK, "");
	}

	protected boolean isChanged() {
		return changed;
	}
	
	public void setChanged(boolean changed) {
		this.changed = changed;
	}

	protected void createStartingSpace(Composite parent, int span) {
		Label label = new Label(parent, SWT.NULL);
		GridData data = new GridData();
		//data.heightHint = 15;
		data.horizontalSpan = span;
		label.setLayoutData(data);
	}

	public void launched(ILaunch launch) {
	}

	public boolean isValid(ILaunchConfiguration config) {
		return valid;
	}
	
	public void setValid(boolean value) {
		this.valid = value;
	}

	/**
	 * Updates the status line and the ok button depending on the status
	 */
	protected void updateStatus(IStatus status) {
		IStatus oldStatus = currentStatus;
		currentStatus = status;
		setValid(!status.matches(IStatus.ERROR));
		if (oldStatus.getSeverity() != currentStatus.getSeverity()
			|| !oldStatus.getMessage().equals(currentStatus.getMessage()))
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
		if (s1.getSeverity() >= s2.getSeverity()) {
			return s1;
		} else {
			return s2;
		}
	}	
	
	public static IStatus createStatus(int severity, String message) {
		return new Status(severity, PDEPlugin.getPluginId(), severity, message, null);
	}		
}
