/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.internal.samples;

import java.util.HashSet;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;

/**
 *
 */
public class ProjectNamesPage extends WizardPage {
	private SampleWizard wizard;
	private Composite container;
	/**
	 * @param pageName
	 */
	public ProjectNamesPage(SampleWizard wizard) {
		super("projects"); //$NON-NLS-1$
		this.wizard = wizard;
		setTitle(PDEPlugin.getResourceString("ProjectNamesPage.title")); //$NON-NLS-1$
		setDescription(PDEPlugin.getResourceString("ProjectNamesPage.desc")); //$NON-NLS-1$
	}
	public void setVisible(boolean visible) {
		setPageComplete(wizard.getSelection()!=null);
		if (container!=null) updateEntries();
		super.setVisible(visible);
	}
	
	private void updateEntries() {
		IConfigurationElement selection = wizard.getSelection();
		if (selection!=null) {
			setMessage(null);
			IConfigurationElement [] projects = selection.getChildren("project"); //$NON-NLS-1$
			Control [] children = container.getChildren();
			if (projects.length==1 && children.length==2) {
				Text text = (Text)children[1];
				text.setText(projects[0].getAttribute("name")); //$NON-NLS-1$
				validateEntries();
				return;
			}
			// dispose all
			for (int i=0; i<children.length; i++) {
				children[i].dispose();
			}
			// create entries
			if (projects.length==1) {
				createEntry(PDEPlugin.getResourceString("ProjectNamesPage.projectName"), projects[0].getAttribute("name")); //$NON-NLS-1$ //$NON-NLS-2$
			}
			else {
				for (int i=0; i<projects.length; i++) {
					String label = PDEPlugin.getFormattedMessage("ProjectNamesPage.multiProjectName", ""+(i+1)); //$NON-NLS-1$ //$NON-NLS-2$
					createEntry(label, projects[i].getAttribute("name")); //$NON-NLS-1$
				}
			}
			container.layout();
			validateEntries();
		}
		else {
			setMessage(PDEPlugin.getResourceString("ProjectNamesPage.noSampleFound"), WizardPage.WARNING); //$NON-NLS-1$
		}
	}
	public String [] getProjectNames() {
		Control [] children = container.getChildren();
		String [] names = new String[children.length/2];

		int index=0;
		for (int i=0; i<children.length; i++) {
			if (children[i] instanceof Text) {
				String name = ((Text)children[i]).getText();
				names[index++] = name;
			}
		}
		return names;
	}
	private void createEntry(String labelName, String projectName) {
		Label label = new Label(container, SWT.NULL);
		label.setText(labelName);
		label.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_CENTER));
		final Text text = new Text(container, SWT.SINGLE|SWT.BORDER);
		text.setText(projectName);
		text.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				validateEntries();
			}
		});
		text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
	}
	private void validateEntries() {
		Control [] children = container.getChildren();
		boolean empty=false;
		
		HashSet set = new HashSet();
		for (int i=0; i<children.length; i++) {
			if (children[i] instanceof Text) {
				String name = ((Text)children[i]).getText();
				if (name.length()==0) {
					empty=true;
					break;
				}
				else {
					IStatus nameStatus = PDEPlugin.getWorkspace().validateName(name, IResource.PROJECT);
					if (!nameStatus.isOK()) {
						setErrorMessage(nameStatus.getMessage());
						setPageComplete(false);
						return;
					}
					set.add(name);
				}
			}
		}
		if (empty) {
			setErrorMessage(PDEPlugin.getResourceString("ProjectNamesPage.emptyName")); //$NON-NLS-1$
			setPageComplete(false);
		}
		else {
			int nnames = set.size();
			int nfields = children.length/2;
			if (nfields>nnames) {
				setErrorMessage(PDEPlugin.getResourceString("ProjectNamesPage.duplicateNames")); //$NON-NLS-1$
				setPageComplete(false);
			}
			else {
				setPageComplete(true);
				setErrorMessage(null);
			}
		}
	}
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		container.setLayout(layout);
		setControl(container);
		updateEntries();
	}
}
