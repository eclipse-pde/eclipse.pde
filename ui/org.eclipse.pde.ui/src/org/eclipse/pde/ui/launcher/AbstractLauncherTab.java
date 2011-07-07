/*******************************************************************************
 *  Copyright (c) 2005, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.launcher;

import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 * An abstract class subclassed by all PDE tabs.
 * <p>
 * This class may be subclassed by clients.
 * </p>
 * @since 3.2
 */
public abstract class AbstractLauncherTab extends AbstractLaunchConfigurationTab {

	/**
	 * Creates an empty label and hence a space in the tab
	 * 
	 * @param parent the parent of the label
	 * @param span the span of the label
	 * @deprecated
	 */
	protected void createStartingSpace(Composite parent, int span) {
		Label label = new Label(parent, SWT.NULL);
		GridData data = new GridData();
		data.horizontalSpan = span;
		label.setLayoutData(data);
	}

	/**
	 * Returns whether the tab contains valid entries
	 * 
	 * @return <code>true</code> if the tab is valid, <code>false</code> otherwise
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#isValid(org.eclipse.debug.core.ILaunchConfiguration)
	 */
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
	 * Validates the page and updates the buttons and message of the launch configuration dialog.
	 * 
	 * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#updateLaunchConfigurationDialog()
	 */
	public void updateLaunchConfigurationDialog() {
		validateTab();
		super.updateLaunchConfigurationDialog();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.AbstractLaunchConfigurationTab#scheduleUpdateJob()
	 */
	public void scheduleUpdateJob() {
		super.scheduleUpdateJob();
	}

	/**
	 * Validates the data entered on the tab.
	 *
	 */
	public abstract void validateTab();

}
