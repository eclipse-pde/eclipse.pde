/*******************************************************************************
 * Copyright (c) 2008, 2011 Code 9 Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Code 9 Corporation - initial API and implementation
 *     Rafael Oliveira Nobrega <rafael.oliveira@gmail.com> - bug 242028
 *     Simon Archer <sarcher@us.ibm.com> - bug 248519
 *     IBM - ongoing maintenance
 *******************************************************************************/
package org.eclipse.pde.internal.ds.ui.wizards;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.search.SearchEngine;
import org.eclipse.jdt.internal.ui.packageview.ClassPathContainer;
import org.eclipse.jdt.ui.IJavaElementSearchConstants;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.internal.core.bundle.WorkspaceBundlePluginModel;
import org.eclipse.pde.internal.core.natures.PDE;
import org.eclipse.pde.internal.core.project.PDEProject;
import org.eclipse.pde.internal.ds.ui.Activator;
import org.eclipse.pde.internal.ds.ui.Messages;
import org.eclipse.pde.internal.ds.ui.SWTUtil;
import org.eclipse.pde.internal.ds.ui.SharedImages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.ui.dialogs.WizardNewFileCreationPage;
import org.osgi.framework.Constants;

public class DSFileWizardPage extends WizardNewFileCreationPage {

	public static final String F_PAGE_NAME = "ds"; //$NON-NLS-1$

	private static final String F_FILE_EXTENSION = "xml"; //$NON-NLS-1$

	private static final String F_DEFAULT_COMPONENT_NAME = "component.xml"; //$NON-NLS-1$

	private static final String S_COMPONENT_NAME = "component"; //$NON-NLS-1$

	private Group fGroup;

	private Text fDSComponentNameText;
	private Label fDSComponentNameLabel;

	private Text fDSImplementationClassText;
	private Link fDSImplementationClassHyperlink;
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
		setImageDescriptor(SharedImages
				.getImageDescriptor(SharedImages.DESC_DS_WIZ));
		// Force the file extension to be 'xml'
		setFileExtension(F_FILE_EXTENSION);
	}

	private void setComponentName() {
		Object element = fSelection.getFirstElement();
		if (element != null) {
			IProject project = getProject(element);
			if (project != null)
				setComponentNameText(project);
		}
		if (fDSComponentNameText.getText().trim().length() == 0) {
			fDSComponentNameText.setText(Messages.DSFileWizardPage_ExampleComponentName);
		}
	}

	private IProject getProject(Object element) {
		IProject project = null;
		if (element instanceof IResource) {
			project = ((IResource) element).getProject();
		} else if (element instanceof IJavaElement) {
			project = ((IJavaElement) element).getJavaProject().getProject();
		} else if (element instanceof ClassPathContainer) {
			project = ((ClassPathContainer) element).getJavaProject()
					.getProject();
		}
		return project;
	}

	private void setComponentNameText(IProject project) {
		try {
			if (project.hasNature(PDE.PLUGIN_NATURE)) {
				WorkspaceBundlePluginModel model = new WorkspaceBundlePluginModel(
						PDEProject.getManifest(project),
						null);
				model.load();
				String header = model.getBundleModel().getBundle().getHeader(
						Constants.BUNDLE_SYMBOLICNAME);
				String[] h = header.split(";"); //$NON-NLS-1$
				fDSComponentNameText.setText(h[0]);
			}
		} catch (CoreException e) {
		}
	}

	protected void createAdvancedControls(Composite parent) {
		IDialogSettings settings = getDialogSettings();
		if (settings != null) {
			String component = settings.get(S_COMPONENT_NAME);
			if (component != null && !component.equals("")) { //$NON-NLS-1$
				setFileName(component);
			} else {
				setFileName(F_DEFAULT_COMPONENT_NAME);
			}
		} else {
			setFileName(F_DEFAULT_COMPONENT_NAME);
		}

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
				setPageComplete(isPageComplete());
			}
		});
		setComponentName();

		fDSImplementationClassHyperlink = new Link(fGroup, SWT.NONE);
		fDSImplementationClassHyperlink.setText("<a>" //$NON-NLS-1$
				+ Messages.DSFileWizardPage_implementation_class + "</a>"); //$NON-NLS-1$
		fDSImplementationClassHyperlink.setForeground(Display.getDefault()
				.getSystemColor(SWT.COLOR_BLUE));
		fDSImplementationClassHyperlink
				.addSelectionListener(new SelectionAdapter() {
					public void widgetSelected(SelectionEvent e) {
						String value = fDSImplementationClassText.getText();
						value = handleLinkActivated(value, false);
						if (value != null)
							fDSImplementationClassText.setText(value);

			}

			private String handleLinkActivated(String value,
							boolean isInter) {
						Object object = fSelection.getFirstElement();
						if (object != null) {
							IProject project = getProject(object);
							try {
								if (project != null
										&& project
												.hasNature(JavaCore.NATURE_ID)) {
									IJavaProject javaProject = JavaCore
											.create(project);
									IJavaElement element = javaProject
											.findType(value.replace('$', '.'));
									if (element != null)
										JavaUI.openInEditor(element);
									else {
										// TODO create our own wizard for reuse
										// here
										DSNewClassCreationWizard wizard = new DSNewClassCreationWizard(
												project, isInter, value);
										WizardDialog dialog = new WizardDialog(
												Activator
														.getActiveWorkbenchShell(),
												wizard);
										dialog.create();
										SWTUtil.setDialogSize(dialog, 400, 500);
										if (dialog.open() == Window.OK) {
											return wizard.getQualifiedName();
								}
							}
						}
							} catch (PartInitException e1) {
							} catch (CoreException e1) {
					}
						}
						return null;
					}

		});

		// Implementation Class Text
		fDSImplementationClassText = new Text(fGroup, SWT.SINGLE | SWT.BORDER);
		GridData classTextGridData = new GridData(GridData.FILL_HORIZONTAL);
		classTextGridData.horizontalSpan = 1;
		classTextGridData.horizontalIndent = 3;
		fDSImplementationClassText.setLayoutData(classTextGridData);
		fDSImplementationClassText.setText("Component"); //$NON-NLS-1$
		fDSImplementationClassText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				setPageComplete(isPageComplete());
			}
		});

		// Implementation Class Browse Button
		fDSImplementationClassButton = new Button(fGroup, SWT.NONE);
		fDSImplementationClassButton.setText(Messages.DSFileWizardPage_browse);
		fDSImplementationClassButton.addMouseListener(new MouseListener() {

			public void mouseDoubleClick(MouseEvent e) {
				// do nothing
			}

			public void mouseDown(MouseEvent e) {
				// do nothing
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
					Activator.logException(e);
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.ui.dialogs.WizardNewFileCreationPage#validateLinkedResource()
	 */
	protected IStatus validateLinkedResource() {
		return new Status(IStatus.OK, Activator.PLUGIN_ID, IStatus.OK, "", null); //$NON-NLS-1$
	}

	protected void createLinkTarget() {
		// NO-OP
	}

	public void createControl(Composite parent) {
		super.createControl(parent);
		Dialog.applyDialogFont(fGroup);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.jface.wizard.WizardPage#isPageComplete()
	 */
	public boolean isPageComplete() {
		return validatePage();
	}

	public void saveSettings(IDialogSettings settings) {
		settings.put(S_COMPONENT_NAME, getFileName());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.WizardNewFileCreationPage#validatePage()
	 */
	protected boolean validatePage() {
		if (fDSComponentNameText == null || fDSImplementationClassText == null) {
			return false;
		}
		
		if (getFileName() == null || getFileName().length() == 0) {
			setErrorMessage(Messages.DSFileWizardPage_ComponentNeedsFileName);
			return false;
		}

		if (fDSComponentNameText.getText().trim().length() == 0) {
			setErrorMessage(Messages.DSFileWizardPage_ComponentNeedsName);
			return false;
		}
		
		IStatus status = ResourcesPlugin.getWorkspace().validateName(fDSImplementationClassText.getText().trim(),IResource.FILE);
		if (!status.isOK()) {
			setErrorMessage(Messages.DSFileWizardPage_ComponentNeedsClass);
			return false;
		}
		
		return super.validatePage();
	}

}
