/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
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

	protected void refreshStatus() {
		getLaunchConfigurationDialog().updateButtons();
		getLaunchConfigurationDialog().updateMessage();
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
		currentStatus = status;
		setValid(!status.matches(IStatus.ERROR));
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
		tab.refreshStatus();
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
