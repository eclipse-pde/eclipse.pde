/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.provisioner.update;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

public class UpdateSitePage extends WizardPage {

	private List fElements = new ArrayList();
	private Button fAddButton;
	private Button fRemoveButton;
	private TreeViewer fTreeViewer;

	protected UpdateSitePage(String pageName) {
		super(pageName);
		setTitle(PDEUIMessages.UpdateSiteWizardPage_title);
		setDescription(PDEUIMessages.UpdateSiteWizardPage_description);
		setPageComplete(false);
	}

	public void createControl(Composite parent) {
		Composite client = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 2;
		layout.numColumns = 2;
		client.setLayout(layout);
		client.setLayoutData(new GridData(GridData.FILL_BOTH));

		Label label = new Label(client, SWT.None);
		label.setText(PDEUIMessages.UpdateSiteWizardPage_label);
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);

		fTreeViewer = new TreeViewer(client, SWT.VIRTUAL);
		fTreeViewer.setInput(fElements);


		// TODO Auto-generated method stub

	}

	protected void createButtons(Composite parent) {
		fAddButton = new Button(parent, SWT.PUSH);
		fAddButton.setText(PDEUIMessages.UpdateSiteWizardPage_add);
		fAddButton.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
		SWTUtil.setButtonDimensionHint(fAddButton);
		fAddButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleAdd();
			}
		});

		fRemoveButton = new Button(parent, SWT.PUSH);
		fRemoveButton.setText(PDEUIMessages.UpdateSiteWizardPage_remove);
		fRemoveButton.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
		SWTUtil.setButtonDimensionHint(fRemoveButton);
		fRemoveButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleRemove();
			}
		});
		updateButtons();		
	}

	protected void handleRemove() {
		// TODO Auto-generated method stub

	}

	protected void handleAdd() {
		// TODO Auto-generated method stub

	}

	protected void updateButtons() {
		int num = fTreeViewer.getTree().getSelectionCount();
		fRemoveButton.setEnabled(num > 0);
	}

	public File[] getLocations() {
//		Preferences pref = PDECore.getDefault().getPluginPreferences();
//		pref.setValue(LAST_LOCATION, fLastLocation);
		return (File[]) fElements.toArray(new File[fElements.size()]);
	}

}
