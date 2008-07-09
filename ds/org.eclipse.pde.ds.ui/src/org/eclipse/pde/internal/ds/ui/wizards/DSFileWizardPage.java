/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Chris Aniszczyk <caniszczyk@gmail.com>
 *     Rafael Oliveira Nóbrega <rafael.oliveira@gmail.com> - bug 223738
 *******************************************************************************/
package org.eclipse.pde.internal.ds.ui.wizards;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.internal.ds.ui.Activator;
import org.eclipse.pde.internal.ds.ui.Messages;
import org.eclipse.pde.internal.ui.wizards.PDEWizardNewFileCreationPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SelectionDialog;

public class DSFileWizardPage extends PDEWizardNewFileCreationPage {

	public static final String F_PAGE_NAME = "ds"; //$NON-NLS-1$

	private static final String F_FILE_EXTENSION = "xml"; //$NON-NLS-1$

	private Group fGroup;

	private Text fDSComponentNameText;
	private Label fDSComponentNameLabel;

	private Text fDSImplementationClassText;
	private Label fDSImplementationClassLabel;
	private Button fDSImplementationClassButton;
	
	private Composite fImplementationComposite;

	public DSFileWizardPage(IStructuredSelection selection) {
		super(F_PAGE_NAME, selection);
		initialize();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.wizards.cheatsheet.CheatSheetFileWizardPage#initialize()
	 */
	protected void initialize() {
		setTitle(Messages.DSFileWizardPage_title);
		setDescription(Messages.DSFileWizardPage_description);
		// Force the file extension to be 'xml'
		setFileExtension(F_FILE_EXTENSION);
	}

	protected void createAdvancedControls(Composite parent) {
		// Controls Group
		fGroup = new Group(parent, SWT.NONE);
		fGroup.setText(Messages.DSFileWizardPage_group);
		fGroup.setLayout(new GridLayout(2, false));
		fGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		
		GridData textGridData = new GridData(GridData.FILL_HORIZONTAL);
		textGridData.widthHint = 20;
		textGridData.horizontalSpan = 1;
		textGridData.horizontalIndent = 3;
		
		fDSComponentNameLabel = new Label(fGroup, SWT.None);
		fDSComponentNameLabel.setText(Messages.DSFileWizardPage_component_name);

		fDSComponentNameText = new Text(fGroup, SWT.NONE);
		fDSComponentNameText.setLayoutData(textGridData);
		fDSComponentNameText.addFocusListener(new FocusListener() {
			public void focusGained(FocusEvent e) {
				if (!fDSComponentNameText.getText().equals("")) { //$NON-NLS-1$
					return;
				}
				String text = DSFileWizardPage.this.getFileName();
				if (text != null && text != "") { //$NON-NLS-1$
					int index = text.lastIndexOf("."); //$NON-NLS-1$
					if (index > 0) {
						fDSComponentNameText.setText(text.substring(0, index));
					} else {
						fDSComponentNameText.setText(text);
					}
				}
			}

			public void focusLost(FocusEvent e) {
			}
		});

		// Implementation Class Composite
		fImplementationComposite = new Composite(fGroup, SWT.NONE);
		fImplementationComposite.setLayout(new GridLayout(3, false));
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 3;
		fImplementationComposite.setLayoutData(data);

		// Implementation Class Label
		fDSImplementationClassLabel = new Label(fImplementationComposite,
				SWT.NONE);
		fDSImplementationClassLabel
				.setText(Messages.DSFileWizardPage_implementation_class);

		// Implementation Class Text
		fDSImplementationClassText = new Text(fImplementationComposite,
				SWT.NONE);
		fDSImplementationClassText.setLayoutData(textGridData);

		// Implementation Class Browse Button
		fDSImplementationClassButton = new Button(fImplementationComposite,
				SWT.NONE);
		fDSImplementationClassButton.setText(Messages.DSFileWizardPage_browse);
		fDSImplementationClassButton.addMouseListener(new MouseListener() {

			public void mouseDoubleClick(MouseEvent e) {
				// TODO Auto-generated method stub

			}

			public void mouseDown(MouseEvent e) {
				// TODO Auto-generated method stub

			}

			public void mouseUp(MouseEvent e) {
				doOpenSelectionDialog(
						IJavaElementSearchConstants.CONSIDER_CLASSES,
						fDSImplementationClassText);
			}

			private void doOpenSelectionDialog(int scopeType, Text entry) {
				try {
					String filter = entry.getText();
					filter = filter.substring(filter.lastIndexOf(".") + 1); //$NON-NLS-1$
					SelectionDialog dialog = JavaUI.createTypeDialog(Activator
							.getActiveWorkbenchShell(), PlatformUI
							.getWorkbench().getProgressService(), SearchEngine
							.createWorkspaceScope(), scopeType, false, filter);
					dialog.setTitle(Messages.DSFileWizardPage_selectType);
					if (dialog.open() == Window.OK) {
						IType type = (IType) dialog.getResult()[0];
						entry.setText(type.getFullyQualifiedName('$'));
					}
				} catch (CoreException e) {
				}
			}

		});
			}

	public String getDSComponentNameValue() {
		return fDSComponentNameText.getText();
	}

	public String getDSImplementationClassValue() {
		return fDSImplementationClassText.getText();
	}

}
