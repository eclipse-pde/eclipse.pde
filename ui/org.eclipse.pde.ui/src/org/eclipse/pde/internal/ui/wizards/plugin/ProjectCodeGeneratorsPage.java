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
package org.eclipse.pde.internal.ui.wizards.plugin;

import java.lang.reflect.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.operation.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.plugin.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.elements.*;
import org.eclipse.pde.internal.ui.wizards.*;
import org.eclipse.pde.ui.*;
import org.eclipse.pde.ui.templates.*;
import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.actions.*;
import org.eclipse.ui.help.*;
import org.eclipse.ui.wizards.newresource.*;

public class ProjectCodeGeneratorsPage extends WizardListSelectionPage {
	private Button blankPageRadio;
	private Button noUIRadio;
	private Button templateRadio;
	private Control wizardList;
	private ControlEnableState wizardListEnableState;
	private IProjectProvider provider;
	private boolean firstTime = true;
	private static final String KEY_TITLE =
		"NewProjectWizard.ProjectCodeGeneratorsPage.title";
	private static final String KEY_BLANK_LABEL =
		"NewProjectWizard.ProjectCodeGeneratorsPage.blankLabel";
	private static final String KEY_TEMPLATE_LABEL =
		"NewProjectWizard.ProjectCodeGeneratorsPage.templateLabel";
	private static final String KEY_NOUI_LABEL =
		"NewProjectWizard.ProjectCodeGeneratorsPage.noUILabel";
	private static final String KEY_DESC =
		"NewProjectWizard.ProjectCodeGeneratorsPage.desc";
	private ProjectStructurePage projectStructurePage;
	private IConfigurationElement config;
	private byte oldSelection;
	private byte BLANK_SELECTION = 0x00;
	private byte NO_UI_SELECTION = 0x01;
	private byte TEMPLATE_SELECTION = 0x002;
	private boolean hasSelectionChanged;

	public ProjectCodeGeneratorsPage(
		IProjectProvider provider,
		ProjectStructurePage projectStructurePage,
		ElementList wizardElements,
		String message,
		IConfigurationElement config) {
		super(wizardElements, message);
		this.provider = provider;
		this.projectStructurePage = projectStructurePage;
		this.config = config;

		setTitle(
			PDEPlugin.getResourceString(KEY_TITLE));
		setDescription(
			PDEPlugin.getResourceString(KEY_DESC));
		hasSelectionChanged = false;
	}

	public void createControl(Composite parent) {
		Composite outerContainer = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.verticalSpacing = 9;
		outerContainer.setLayout(layout);

		blankPageRadio = new Button(outerContainer, SWT.RADIO | SWT.LEFT);
		blankPageRadio.setText(
			PDEPlugin.getResourceString(KEY_BLANK_LABEL));
		blankPageRadio.setSelection(false);
		GridData gd = new GridData();
		gd.horizontalAlignment = GridData.FILL;
		gd.verticalAlignment = GridData.BEGINNING;
		gd.grabExcessHorizontalSpace = true;
		blankPageRadio.setLayoutData(gd);
		blankPageRadio.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				setWizardListEnabled(!blankPageRadio.getSelection());
				if (blankPageRadio.getSelection() && oldSelection != BLANK_SELECTION)
					hasSelectionChanged = true;
				getContainer().updateButtons();
			}
		});

		noUIRadio = new Button(outerContainer, SWT.RADIO | SWT.LEFT);
		noUIRadio.setText(PDEPlugin.getResourceString(KEY_NOUI_LABEL));
		noUIRadio.setSelection(false);
		gd = new GridData();
		gd.horizontalAlignment = GridData.FILL;
		gd.verticalAlignment = GridData.BEGINNING;
		gd.grabExcessHorizontalSpace = true;
		noUIRadio.setLayoutData(gd);
		noUIRadio.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e){
				setWizardListEnabled(!noUIRadio.getSelection());
				if (noUIRadio.getSelection() && oldSelection != NO_UI_SELECTION)
					hasSelectionChanged = true;
				getContainer().updateButtons();
			}
		});
		
		templateRadio = new Button(outerContainer, SWT.RADIO | SWT.LEFT);
		templateRadio.setText(
			PDEPlugin.getResourceString(KEY_TEMPLATE_LABEL));
		templateRadio.setSelection(true);
		gd = new GridData();
		gd.horizontalAlignment = GridData.FILL;
		gd.verticalAlignment = GridData.BEGINNING;
		gd.grabExcessHorizontalSpace = true;
		templateRadio.setLayoutData(gd);
		templateRadio.addSelectionListener(new SelectionAdapter(){
			public void widgetSelected(SelectionEvent e){
				if (templateRadio.getSelection() && oldSelection != TEMPLATE_SELECTION)
					hasSelectionChanged = true;
			}
		});

		super.createControl(outerContainer);
		wizardList = super.wizardSelectionViewer.getControl();
		gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 450;
		getControl().setLayoutData(gd);
		setControl(outerContainer);
		Dialog.applyDialogFont(outerContainer);
		WorkbenchHelp.setHelp(
			outerContainer,
			IHelpContextIds.NEW_PROJECT_CODE_GEN_PAGE);
	}

	public void setVisible(boolean visible) {
		super.setVisible(visible);

		if (visible){
			setOldSelection();
			hasSelectionChanged = false;
			if (firstTime) {
				if (blankPageRadio.getSelection())
					blankPageRadio.setFocus();
				else {
					focusAndSelectFirst();
				}
				firstTime = false;
			}
		}
	}

	public void setOldSelection(){
		if (blankPageRadio.getSelection())
			oldSelection = BLANK_SELECTION;
		else if (templateRadio.getSelection())
			oldSelection = TEMPLATE_SELECTION;
		else
			oldSelection = NO_UI_SELECTION;
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
					false,
					config);
				((AbstractNewPluginTemplateWizard)wizard).setShowTemplatePages(templateRadio.getSelection());
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
					projectStructurePage.isStructureDataChanged() || hasSelectionChanged));
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
						false,
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
					createBlankManifest(project, structureData, monitor);
					setJavaSettings(project, structureData, monitor);
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
			CoreUtility.addNatureToProject(project, JavaCore.NATURE_ID, monitor);
		if (!project.hasNature(PDE.PLUGIN_NATURE))
			CoreUtility.addNatureToProject(project, PDE.PLUGIN_NATURE, monitor);
		ClasspathUtil.setClasspath(project, structureData, monitor);
	}

	private void createBlankManifest(
		IProject project,
		IPluginStructureData structureData,
		IProgressMonitor monitor)
		throws CoreException {

		IPath path =
			project.getFullPath().append("plugin.xml");
		IFile file = project.getWorkspace().getRoot().getFile(path);

		WorkspacePluginModelBase model = null;
		model = new WorkspacePluginModel(file);
		model.load();

		if (!file.exists()) {
			IPluginBase pluginBase = model.getPluginBase();
			pluginBase.setId(structureData.getPluginId());
			pluginBase.setVersion("1.0.0");
			pluginBase.setName(structureData.getPluginId());
			pluginBase.setSchemaVersion("3.0");
			if (structureData.getRuntimeLibraryName() != null) {
				IPluginLibrary library = model.getPluginFactory().createLibrary();
				library.setName(structureData.getRuntimeLibraryName());
				library.setExported(true);
				model.getPluginBase().add(library);
			}
			model.save();
		}
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
