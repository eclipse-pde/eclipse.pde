/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.shared.target;

import java.io.File;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.internal.core.target.provisional.IBundleContainer;
import org.eclipse.pde.internal.ui.SWTFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;

/**
 * Wizard page for creating a profile (installation) bundle container.
 * 
 * @see AddBundleContainerWizard
 * @see AddBundleContainerSelectionPage
 * @see IBundleContainer
 */
public class AddProfileContainerPage extends AddDirectoryContainerPage {

	private Label fConfigLabel;
	private Text fConfigLocation;
	private Button fConfigBrowse;

	public AddProfileContainerPage(String pageName) {
		super(pageName);
		setTitle(Messages.AddProfileContainerPage_0);
		setMessage(Messages.AddProfileContainerPage_1);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.shared.target.AddDirectoryContainerPage#createLocationArea(org.eclipse.swt.widgets.Composite)
	 */
	protected void createLocationArea(Composite parent) {
		super.createLocationArea(parent);

		Composite configComp = SWTFactory.createComposite(parent, 3, 1, GridData.FILL_HORIZONTAL);

		Button defaultConfigButton = new Button(configComp, SWT.CHECK | SWT.RIGHT);
		GridData gd = new GridData();
		gd.horizontalSpan = 3;
		defaultConfigButton.setLayoutData(gd);
		defaultConfigButton.setText(Messages.AddProfileContainerPage_2);
		defaultConfigButton.setSelection(true);
		defaultConfigButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				Button button = (Button) e.getSource();
				fConfigLabel.setEnabled(!button.getSelection());
				fConfigLocation.setEnabled(!button.getSelection());
				fConfigBrowse.setEnabled(!button.getSelection());
				locationChanged();
				updateTable();
			}
		});
		defaultConfigButton.setSelection(true);

		fConfigLabel = SWTFactory.createLabel(configComp, Messages.AddProfileContainerPage_3, 1);
		fConfigLabel.setEnabled(false);

		fConfigLocation = SWTFactory.createText(configComp, SWT.BORDER, 1);
		fConfigLocation.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				locationChanged();
				updateTable();
			}
		});
		fConfigLocation.setEnabled(false);

		fConfigBrowse = SWTFactory.createPushButton(configComp, Messages.AddProfileContainerPage_4, null);
		fConfigBrowse.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dialog = new DirectoryDialog(getShell());
				dialog.setFilterPath(fConfigLocation.getText());
				dialog.setText(Messages.AddProfileContainerPage_5);
				dialog.setMessage(Messages.AddProfileContainerPage_6);
				String result = dialog.open();
				if (result != null)
					fConfigLocation.setText(result);
			}
		});
		fConfigBrowse.setEnabled(false);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.shared.target.AddDirectoryContainerPage#locationChanged()
	 */
	protected void locationChanged() {
		fContainer = null;
		if (fInstallLocation.getText().trim().length() == 0) {
			setErrorMessage(null);
		} else {
			File location = new File(fInstallLocation.getText());
			if (!location.isDirectory()) {
				setErrorMessage(Messages.AddDirectoryContainerPage_6);
			} else {
				if (fConfigLocation.isEnabled()) {
					File configLocation = new File(fConfigLocation.getText());
					if (!configLocation.isDirectory()) {
						setErrorMessage(Messages.AddProfileContainerPage_8);
					} else {
						try {
							fContainer = getTargetPlatformService().newProfileContainer(fInstallLocation.getText(), fConfigLocation.getText());
							setErrorMessage(null);
						} catch (CoreException ex) {
							setErrorMessage(ex.getMessage());
						}
					}
				} else {
					try {
						fContainer = getTargetPlatformService().newProfileContainer(fInstallLocation.getText(), null);
						setErrorMessage(null);
					} catch (CoreException ex) {
						setErrorMessage(ex.getMessage());
					}
				}
			}
		}

	}

}
