/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.osgi.wizards.project;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.core.osgi.bundle.IBundle;
import org.eclipse.pde.internal.PDE;
import org.eclipse.pde.internal.core.CoreUtility;
import org.eclipse.pde.internal.core.osgi.bundle.WorkspaceBundleModel;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.codegen.JavaCodeGenerator;
import org.eclipse.pde.internal.ui.elements.ElementList;
import org.eclipse.pde.internal.ui.wizards.*;
import org.eclipse.pde.internal.ui.wizards.project.ProjectStructurePage;
import org.eclipse.pde.ui.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;

public class BundleProjectCodeGeneratorsPage extends WizardListSelectionPage {
	private Button blankPageRadio;
	private Button templateRadio;
	private Control wizardList;
	private ControlEnableState wizardListEnableState;
	private boolean fragment;
	private IProjectProvider provider;
	private boolean firstTime = true;
	private static final String KEY_TITLE =
		"NewBundleProjectWizard.ProjectCodeGeneratorsPage.title";
	private static final String KEY_BLANK_LABEL =
		"NewBundleProjectWizard.ProjectCodeGeneratorsPage.blankLabel";
	private static final String KEY_BLANK_FLABEL =
		"NewBundleProjectWizard.ProjectCodeGeneratorsPage.blankFLabel";
	private static final String KEY_TEMPLATE_LABEL =
		"NewBundleProjectWizard.ProjectCodeGeneratorsPage.templateLabel";
	private static final String KEY_TEMPLATE_FLABEL =
		"NewBundleProjectWizard.ProjectCodeGeneratorsPage.templateFLabel";
	private static final String KEY_DESC =
		"NewBundleProjectWizard.ProjectCodeGeneratorsPage.desc";
	private static final String KEY_FTITLE =
		"NewBundleProjectWizard.ProjectCodeGeneratorsPage.ftitle";
	private static final String KEY_FDESC =
		"NewBundleProjectWizard.ProjectCodeGeneratorsPage.fdesc";
	private BundleProjectStructurePage projectStructurePage;
	private IConfigurationElement config;

	public BundleProjectCodeGeneratorsPage(
		IProjectProvider provider,
		BundleProjectStructurePage projectStructurePage,
		ElementList wizardElements,
		String message,
		boolean fragment,
		IConfigurationElement config) {
		super(wizardElements, message);
		this.fragment = fragment;
		this.provider = provider;
		this.projectStructurePage = projectStructurePage;
		this.config = config;

		setTitle(
			PDEPlugin.getResourceString(fragment ? KEY_FTITLE : KEY_TITLE));
		setDescription(
			PDEPlugin.getResourceString(fragment ? KEY_FDESC : KEY_DESC));
	}

	public void createControl(Composite parent) {
		Composite outerContainer = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.verticalSpacing = 9;
		outerContainer.setLayout(layout);

		blankPageRadio = new Button(outerContainer, SWT.RADIO | SWT.LEFT);
		blankPageRadio.setText(
			PDEPlugin.getResourceString(
				fragment ? KEY_BLANK_FLABEL : KEY_BLANK_LABEL));
		blankPageRadio.setSelection(false);
		GridData gd = new GridData();
		gd.horizontalAlignment = GridData.FILL;
		gd.verticalAlignment = GridData.BEGINNING;
		gd.grabExcessHorizontalSpace = true;
		blankPageRadio.setLayoutData(gd);
		blankPageRadio.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				setWizardListEnabled(!blankPageRadio.getSelection());
				getContainer().updateButtons();
			}
		});

		templateRadio = new Button(outerContainer, SWT.RADIO | SWT.LEFT);
		templateRadio.setText(
			PDEPlugin.getResourceString(
				fragment ? KEY_TEMPLATE_FLABEL : KEY_TEMPLATE_LABEL));
		templateRadio.setSelection(true);
		gd = new GridData();
		gd.horizontalAlignment = GridData.FILL;
		gd.verticalAlignment = GridData.BEGINNING;
		gd.grabExcessHorizontalSpace = true;
		templateRadio.setLayoutData(gd);

		super.createControl(outerContainer);
		wizardList = super.wizardSelectionViewer.getControl();
		gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 450;
		getControl().setLayoutData(gd);
		setControl(outerContainer);
		Dialog.applyDialogFont(outerContainer);
		if (fragment)
			WorkbenchHelp.setHelp(
				outerContainer,
				IHelpContextIds.NEW_FRAGMENT_CODE_GEN_PAGE);
		else
			WorkbenchHelp.setHelp(
				outerContainer,
				IHelpContextIds.NEW_PROJECT_CODE_GEN_PAGE);
	}

	public void setVisible(boolean visible) {
		super.setVisible(visible);

		if (visible && firstTime) {
			if (blankPageRadio.getSelection())
				blankPageRadio.setFocus();
			else {
				focusAndSelectFirst();
			}
			firstTime = false;
		}
	}

	protected IWizardNode createWizardNode(WizardElement element) {
		return new WizardNode(this, element) {
			public IBasePluginWizard createWizard() throws CoreException {
				IPluginContentWizard wizard =
					(IPluginContentWizard) wizardElement
						.createExecutableExtension();
				wizard.init(
					provider,
					projectStructurePage.getStructureData(),
					fragment,
					config);
				return wizard;
			}
		};
	}
	public boolean finish() {
		if (blankPageRadio.getSelection()) {
			if (projectStructurePage.getStructureData().getRuntimeLibraryName()
				!= null) {
				// we must set the Java settings here
				// because there are no wizards to run
				runJavaSettingsOperation();
			} else {
				runSimpleManifestOperation();
			}
		}
		return true;
	}

	public boolean canFlipToNextPage() {
		return !blankPageRadio.getSelection();
	}

	public IWizardPage getNextPage() {
		return (
			blankPageRadio.getSelection()
				? null
				: super.getNextPage(
					projectStructurePage.isStructureDataChanged()));
	}

	public boolean isPageComplete() {
		if (blankPageRadio != null && blankPageRadio.getSelection())
			return true;
		return super.isPageComplete();
	}

	public void runSimpleManifestOperation() {
		final IPluginStructureData structureData =
			projectStructurePage.getStructureData();
		final IProject project = provider.getProject();
		IRunnableWithProgress operation = new WorkspaceModifyOperation() {
			public void execute(IProgressMonitor monitor) {
				try {
					createBlankManifest(project, structureData, monitor);
					ProjectStructurePage.createBuildProperties(
						project,
						structureData,
						fragment,
						monitor);
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				} finally {
					monitor.done();
				}
			}
		};
		try {
			getContainer().run(false, true, operation);
		} catch (InvocationTargetException e) {
			PDEPlugin.logException(e);
		} catch (InterruptedException e) {
		}
	}

	private void runJavaSettingsOperation() {
		final IPluginStructureData structureData =
			projectStructurePage.getStructureData();
		final IProject project = provider.getProject();

		IRunnableWithProgress operation = new WorkspaceModifyOperation() {
			public void execute(IProgressMonitor monitor) {
				try {
					boolean exists =
						createBlankManifest(project, structureData, monitor);
					setJavaSettings(project, structureData, !exists, monitor);
					BasicNewProjectResourceWizard.updatePerspective(config);
				} catch (JavaModelException e) {
					PDEPlugin.logException(e);
				} catch (CoreException e) {
					PDEPlugin.logException(e);
				} finally {
					monitor.done();
				}
			}
		};
		try {
			getContainer().run(false, true, operation);
		} catch (InvocationTargetException e) {
			PDEPlugin.logException(e);
		} catch (InterruptedException e) {
		}
	}
	private void setJavaSettings(
		IProject project,
		IPluginStructureData structureData,
		boolean setBuildpath,
		IProgressMonitor monitor)
		throws JavaModelException, CoreException {
		if (project.exists() == false) {
			IProjectDescription desc =
				project.getWorkspace().newProjectDescription(project.getName());
			desc.setLocation(provider.getLocationPath());
			project.create(desc, monitor);
			project.open(monitor);
		}
		if (!project.hasNature(JavaCore.NATURE_ID))
			CoreUtility.addNatureToProject(
				project,
				JavaCore.NATURE_ID,
				monitor);
		if (!project.hasNature(PDE.PLUGIN_NATURE))
			CoreUtility.addNatureToProject(project, PDE.PLUGIN_NATURE, monitor);
		JavaCore.create(project);
		if (setBuildpath)
			ClasspathUtil.setClasspath(
				project,
				structureData,
				new IClasspathEntry[0],
				true,
				monitor);
	}

	private boolean createBlankManifest(
		IProject project,
		IPluginStructureData structureData,
		IProgressMonitor monitor)
		throws CoreException {

		JavaCodeGenerator.ensureFoldersExist(project, "META-INF", "/");
		IFile file = project.getFile("META-INF/MANIFEST.MF");

		if (!file.exists()) {
			WorkspaceBundleModel model = new WorkspaceBundleModel(file);
			IBundle bundle = model.getBundle();
			bundle.setHeader(IBundle.KEY_NAME, structureData.getPluginId());
			bundle.setHeader(IBundle.KEY_UNIQUEID, structureData.getPluginId());
			bundle.setHeader(IBundle.KEY_VERSION, "1.0.0");
			bundle.setHeader(IBundle.KEY_DESC, structureData.getPluginId());
			if (structureData.getRuntimeLibraryName() != null) {
				String libName = structureData.getRuntimeLibraryName();
				bundle.setHeader(IBundle.KEY_CLASSPATH, libName);
			}
			model.save();
		}
		return !file.exists();
	}

	private void setWizardListEnabled(boolean enabled) {
		if (!enabled) {
			wizardListEnableState = ControlEnableState.disable(wizardList);
		} else {
			if (wizardListEnableState != null)
				wizardListEnableState.restore();
			wizardSelectionViewer.getControl().setEnabled(true);
			wizardList.setFocus();
		}
	}
}
