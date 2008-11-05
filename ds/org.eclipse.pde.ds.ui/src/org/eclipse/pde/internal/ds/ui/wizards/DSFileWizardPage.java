/*******************************************************************************
 * Copyright (c) 2008 Code 9 Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Code 9 Corporation - initial API and implementation
 *     Chris Aniszczyk <caniszczyk@gmail.com>
 *     Rafael Oliveira Nobrega <rafael.oliveira@gmail.com> - bug 242028
 *     Simon Archer <sarcher@us.ibm.com> - bug 248519
 *******************************************************************************/
package org.eclipse.pde.internal.ds.ui.wizards;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.internal.core.JavaElement;
import org.eclipse.jdt.internal.ui.packageview.ClassPathContainer;
import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.pde.internal.core.bundle.WorkspaceBundlePluginModel;
import org.eclipse.pde.internal.core.natures.PDE;
import org.eclipse.pde.internal.ds.ui.Activator;
import org.eclipse.pde.internal.ds.ui.Messages;
import org.eclipse.pde.internal.ui.util.PDEModelUtility;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
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
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;
import org.osgi.framework.Constants;

public class DSFileWizardPage extends WizardNewFileCreationPage {

	public static final String F_PAGE_NAME = "ds"; //$NON-NLS-1$

	private static final String F_FILE_EXTENSION = "xml"; //$NON-NLS-1$

	private static final String F_COMPONENT_NAME = "component.xml"; //$NON-NLS-1$

	private Group fGroup;

	private Text fDSComponentNameText;
	private Label fDSComponentNameLabel;

	private Text fDSImplementationClassText;
	private Label fDSImplementationClassLabel;
	private Button fDSImplementationClassButton;

	private IStructuredSelection fSelection;

	public DSFileWizardPage(IStructuredSelection selection) {
		super(F_PAGE_NAME, selection);
		this.fSelection = selection;
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
		setFileName(F_COMPONENT_NAME);
	}

	private void setComponentName() {
		Object element = fSelection.getFirstElement();
		if (element != null) {
			IProject project = null;
			if (element instanceof IResource) {
				project = ((IResource) element).getProject();
			} else if (element instanceof JavaElement) {
				project = ((JavaElement) element).getJavaProject()
						.getProject();
			} else if (element instanceof ClassPathContainer) {
				project = ((ClassPathContainer) element).getJavaProject()
						.getProject();
			}
			if (project != null)
				setComponentNameText(project);
		}
	}

	private void setComponentNameText(IProject project) {
		try {
			if (project.hasNature(PDE.PLUGIN_NATURE)) {
				WorkspaceBundlePluginModel model = new WorkspaceBundlePluginModel(
						project.getFile(PDEModelUtility.F_MANIFEST_FP), null);
				model.load();
				String header = model.getBundleModel().getBundle().getHeader(
						Constants.BUNDLE_SYMBOLICNAME);
				fDSComponentNameText.setText(header);
			}
		} catch (CoreException e) {
		}
	}

	protected void createAdvancedControls(Composite parent) {
		// Controls Group
		fGroup = new Group(parent, SWT.NONE);
		fGroup.setText(Messages.DSFileWizardPage_group);
		fGroup.setLayout(new GridLayout(3, false));
		fGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		GridData nameTextGridData = new GridData(GridData.FILL_HORIZONTAL);
		nameTextGridData.horizontalSpan = 2;
		nameTextGridData.horizontalIndent = 3;

		fDSComponentNameLabel = new Label(fGroup, SWT.None);
		fDSComponentNameLabel.setText(Messages.DSFileWizardPage_component_name);

		fDSComponentNameText = new Text(fGroup, SWT.SINGLE | SWT.BORDER);
		fDSComponentNameText.setLayoutData(nameTextGridData);
		fDSComponentNameText.setText(""); //$NON-NLS-1$
		fDSComponentNameText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				setPageComplete(checkPageComplete());
			}
		});
		setComponentName();

		// Implementation Class Label
		fDSImplementationClassLabel = new Label(fGroup, SWT.NONE);
		fDSImplementationClassLabel
				.setText(Messages.DSFileWizardPage_implementation_class);

		// Implementation Class Text
		fDSImplementationClassText = new Text(fGroup, SWT.SINGLE | SWT.BORDER);
		GridData classTextGridData = new GridData(GridData.FILL_HORIZONTAL);
		classTextGridData.horizontalSpan = 1;
		classTextGridData.horizontalIndent = 3;
		fDSImplementationClassText.setLayoutData(classTextGridData);
		fDSImplementationClassText.setText(""); //$NON-NLS-1$
		fDSImplementationClassText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				setPageComplete(checkPageComplete());
			}
		});

		// Implementation Class Browse Button
		fDSImplementationClassButton = new Button(fGroup, SWT.NONE);
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

	private boolean checkPageComplete() {
		if (fDSComponentNameText == null || fDSImplementationClassText == null) {
			return false;
		}
		return fDSComponentNameText.getText().length() > 0
				&& fDSImplementationClassText.getText().length() > 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.dialogs.WizardNewFileCreationPage#validateLinkedResource()
	 */
	protected IStatus validateLinkedResource() {
		return new Status(IStatus.OK, Activator.PLUGIN_ID, IStatus.OK, "", null); //$NON-NLS-1$
	}

	protected void createLinkTarget() {
		// NO-OP
	}

	public boolean isPageComplete() {
		return checkPageComplete();
	}

}
