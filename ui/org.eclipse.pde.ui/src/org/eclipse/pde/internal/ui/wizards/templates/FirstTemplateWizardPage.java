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
package org.eclipse.pde.internal.ui.wizards.templates;

import java.util.ArrayList;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.JavaConventions;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.eclipse.pde.internal.core.plugin.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.pde.internal.ui.wizards.project.*;
import org.eclipse.pde.ui.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.help.WorkbenchHelp;

public class FirstTemplateWizardPage extends WizardPage {
	private static final String KEY_TITLE = "DefaultCodeGenerationPage.title";
	private static final String KEY_FTITLE = "DefaultCodeGenerationPage.ftitle";
	private static final String KEY_ID_NOT_SET =
		"DefaultCodeGenerationPage.idNotSet";
	private static final String KEY_VERSION_FORMAT =
		"DefaultCodeGenerationPage.versionFormat";
	private static final String KEY_INVALID_ID =
		"DefaultCodeGenerationPage.invalidId";
	private static final String KEY_DESC = "DefaultCodeGenerationPage.desc";
	private static final String KEY_FDESC = "DefaultCodeGenerationPage.fdesc";
	private static final String KEY_FNAME = "DefaultCodeGenerationPage.fname";
	private static final String KEY_NAME = "DefaultCodeGenerationPage.name";
	private static final String KEY_VERSION = "DefaultCodeGenerationPage.version";
	private static final String KEY_PROVIDER =
		"DefaultCodeGenerationPage.providerName";
	private static final String KEY_PLUGIN_ID =
		"DefaultCodeGenerationPage.pluginId";
	private static final String KEY_BROWSE =
		"DefaultCodeGenerationPage.pluginId.browse";
	private static final String KEY_PLUGIN_VERSION =
		"DefaultCodeGenerationPage.pluginVersion";

	private static final String KEY_MATCH =
		"ManifestEditor.PluginSpecSection.versionMatch";
	private static final String KEY_MATCH_PERFECT =
		"ManifestEditor.MatchSection.perfect";
	private static final String KEY_MATCH_EQUIVALENT =
		"ManifestEditor.MatchSection.equivalent";
	private static final String KEY_MATCH_COMPATIBLE =
		"ManifestEditor.MatchSection.compatible";
	private static final String KEY_MATCH_GREATER =
		"ManifestEditor.MatchSection.greater";

	private static final String KEY_CLASS = "DefaultCodeGenerationPage.class";
	private static final String KEY_GENERATE = "DefaultCodeGenerationPage.generate";
	private static final String KEY_INITIAL_NAME =
		"DefaultCodeGenerationPage.initialName";
	private static final String KEY_INITIAL_FNAME =
		"DefaultCodeGenerationPage.initialFName";
	private static final String KEY_CREATING = "DefaultCodeGenerationPage.creating";
	private static final String KEY_OPTIONS = "DefaultCodeGenerationPage.options";
	private static final String KEY_OPTIONS_THIS =
		"DefaultCodeGenerationPage.options.this";
	private static final String KEY_OPTIONS_BUNDLE =
		"DefaultCodeGenerationPage.options.bundle";
	private static final String KEY_OPTIONS_WORKSPACE =
		"DefaultCodeGenerationPage.options.workspace";
	private static final String KEY_BROWSE_TITLE = "DefaultCodeGenerationPage.pluginId.browse.title";
		
	private IProjectProvider projectProvider;
	private IPluginStructureData structureData;
	private boolean fragment;
	private Text nameField;
	private Text pluginVersionField;
	private Text pluginIdField;
	private Button thisCheck;
	private Button bundleCheck;
	private Button workspaceCheck;
	private Text versionField;
	private Combo matchCombo;
	private Text providerField;
	private Text classField;
	private Button generateMainClass;
	
	private boolean pluginFieldsStatus;
	private boolean versionStatus;
	private boolean classStatus;
	
	private String versionError;
	private String classError;

	public FirstTemplateWizardPage(
		IProjectProvider projectProvider,
		IPluginStructureData structureData,
		boolean fragment) {
		super("DefaultCodeGenerationPage");
		this.fragment = fragment;
		if (fragment) {
			setTitle(PDEPlugin.getResourceString(KEY_FTITLE));
			setDescription(PDEPlugin.getResourceString(KEY_FDESC));
		} else {
			setTitle(PDEPlugin.getResourceString(KEY_TITLE));
			setDescription(PDEPlugin.getResourceString(KEY_DESC));
		}
		this.projectProvider = projectProvider;
		this.structureData = structureData;
		pluginFieldsStatus=true;
		versionStatus=true;
		classStatus=true;
	}

	public void becomesVisible(int event) {
		nameField.setFocus();
	}
	
	private void browsePluginId() {
		BusyIndicator.showWhile(pluginIdField.getDisplay(), new Runnable() {
			public void run() {
				PluginSelectionDialog dialog =
					new PluginSelectionDialog(pluginIdField.getShell());
				dialog.create();
				SWTUtil.setDialogSize(dialog, 300, 400);
				dialog.getShell().setText(PDEPlugin.getResourceString(KEY_BROWSE_TITLE));
				if (dialog.open() == PluginSelectionDialog.OK) {
					IPluginModel model = (IPluginModel) dialog.getFirstResult();
					IPlugin plugin = model.getPlugin();
					pluginIdField.setText(plugin.getId());
					pluginVersionField.setText(plugin.getVersion());
				}
			}
		});
	}
	
	private Button createCheck(Composite parent, String label, boolean state) {
		Button check = new Button(parent, SWT.CHECK);
		check.setText(label);
		GridData gd = new GridData();
		gd.horizontalAlignment = GridData.FILL;
		check.setLayoutData(gd);
		check.setSelection(state);
		return check;
	}
	
	private void addCommonControls(Composite parent) {
		nameField =
			createField(
				parent,
				PDEPlugin.getResourceString(fragment ? KEY_FNAME : KEY_NAME));
		versionField =
			createField(parent, PDEPlugin.getResourceString(KEY_VERSION));
		versionField.setText("1.0.0");
		versionField.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				if (!isVersionValid(versionField.getText())) {
					versionStatus=false;
					setPageComplete(versionStatus);
					setVersionError(PDEPlugin.getResourceString(KEY_VERSION_FORMAT));
				} else if (fragment) {
					verifyPluginFields();
				} else {
					versionStatus=true;
					evalPageComplete();
					setVersionError(null);
				}
			}
		});
		providerField =
			createField(parent, PDEPlugin.getResourceString(KEY_PROVIDER));
	}
	
	private void addFragmentSpecificControls(Composite parent) {
			pluginIdField =
				createField(parent, PDEPlugin.getResourceString(KEY_PLUGIN_ID), false);
			pluginIdField.addModifyListener(new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					verifyPluginFields();
				}
			});
			Button browsePluginButton = new Button(parent, SWT.PUSH);
			browsePluginButton.setText(PDEPlugin.getResourceString(KEY_BROWSE));
			browsePluginButton.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					browsePluginId();
				}
			});
			pluginVersionField =
				createField(parent, PDEPlugin.getResourceString(KEY_PLUGIN_VERSION));
			pluginVersionField.addModifyListener(new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					verifyPluginFields();
				}
			});
			matchCombo = createMatchCombo(parent);
	}
	
	private void addPluginSpecificControls(Composite parent) {  
			classField = createField(parent, PDEPlugin.getResourceString(KEY_CLASS));
			classField.addModifyListener(new ModifyListener() {
				public void modifyText(ModifyEvent arg0) {
					IStatus status = JavaConventions.validateJavaTypeName(classField.getText());
					if (classField.getText().equals("")){
						generateMainClass.setEnabled(false);
						thisCheck.setEnabled(false);
						bundleCheck.setEnabled(false);
						workspaceCheck.setEnabled(false);	
						setClassError(null);
						classStatus=true;
						evalPageComplete();
					} else if (status.getSeverity() == IStatus.ERROR) {
						generateMainClass.setEnabled(true);
						thisCheck.setEnabled(generateMainClass.getSelection());
						bundleCheck.setEnabled(generateMainClass.getSelection());
						workspaceCheck.setEnabled(generateMainClass.getSelection());	
						setClassError(status.getMessage());
						classStatus=false;
						setPageComplete(classStatus);
					} else {
						generateMainClass.setEnabled(true);
						thisCheck.setEnabled(generateMainClass.getSelection());
						bundleCheck.setEnabled(generateMainClass.getSelection());
						workspaceCheck.setEnabled(generateMainClass.getSelection());	
						setClassError(null);
						classStatus=true;
						evalPageComplete();
					}
				}
			});

			new Label(parent, SWT.NONE);
			generateMainClass = new Button(parent, SWT.CHECK);
			generateMainClass.setText(PDEPlugin.getResourceString(KEY_GENERATE));
			generateMainClass.setSelection(true);
			generateMainClass.setEnabled(true);
			GridData gd = new GridData();
			gd.horizontalSpan = 2;
			generateMainClass.setLayoutData(gd);
			generateMainClass.addSelectionListener(new SelectionAdapter() {
				public void widgetSelected(SelectionEvent e) {
					boolean value = generateMainClass.getSelection();
					thisCheck.setEnabled(value);
					bundleCheck.setEnabled(value);
					workspaceCheck.setEnabled(value);
				}
			});

			gd = new GridData();
			gd.horizontalSpan = 3;
			new Label(parent, SWT.NONE).setLayoutData(gd);
			Group checkGroup = new Group(parent, SWT.NONE);
			checkGroup.setText(PDEPlugin.getResourceString(KEY_OPTIONS));
			checkGroup.setLayout(new GridLayout());
			gd = new GridData();
			gd.horizontalSpan = 3;
			gd.horizontalAlignment = GridData.FILL;
			checkGroup.setLayoutData(gd);
			thisCheck =
				createCheck(checkGroup, PDEPlugin.getResourceString(KEY_OPTIONS_THIS), true);
			bundleCheck =
				createCheck(checkGroup, PDEPlugin.getResourceString(KEY_OPTIONS_BUNDLE), true);
			workspaceCheck =
				createCheck(
					checkGroup,
					PDEPlugin.getResourceString(KEY_OPTIONS_WORKSPACE),
					true);
	}
	
	public void createControl(Composite parent) {
		GridLayout layout = new GridLayout();
		Composite container = new Composite(parent, SWT.NONE);
		layout.numColumns = 3;
		layout.verticalSpacing = 9;
		container.setLayout(layout);

		addCommonControls(container);
		if (fragment) {
			addFragmentSpecificControls(container);
		} else {
			addPluginSpecificControls(container);
		}
		presetFields();
		setControl(container);
		Dialog.applyDialogFont(container);
		if (fragment) 
			WorkbenchHelp.setHelp(container, IHelpContextIds.NEW_FRAGMENT_REQUIRED_DATA);
		else
			WorkbenchHelp.setHelp(container, IHelpContextIds.NEW_PROJECT_REQUIRED_DATA);
	}
	
	private void verifyPluginFields() {
		if (pluginIdField.getText().length() == 0) {
			setErrorMessage(PDEPlugin.getResourceString(KEY_ID_NOT_SET));
			pluginFieldsStatus=false;
			setPageComplete(pluginFieldsStatus);
		} else {
			String id = pluginIdField.getText();
			String version = pluginVersionField.getText();

			if (version.length() == 0 || !isVersionValid(version)) {
				pluginFieldsStatus=false;
				setPageComplete(pluginFieldsStatus);
				setErrorMessage(PDEPlugin.getResourceString(KEY_VERSION_FORMAT));
				return;
			}
			int match = matchCombo.getSelectionIndex();
			if (isPluginValid(id, version, match)) {
				pluginFieldsStatus=true;
				evalPageComplete();
				setErrorMessage(null);
			} else {
				pluginFieldsStatus=false;
				setPageComplete(pluginFieldsStatus);
				setErrorMessage(PDEPlugin.getResourceString(KEY_INVALID_ID));
			}
		}
	}
	private boolean isPluginValid(
		String pluginId,
		String pluginVersion,
		int match) {
		IPlugin plugin =
			PDECore.getDefault().findPlugin(pluginId, pluginVersion, match);
		return plugin != null;
	}
	
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			nameField.setFocus();
		}
	}
	private Text createField(Composite parent, String label) {
		return createField(parent, label, true);
	}
	private Text createField(Composite parent, String label, boolean addFiller) {
		Label l = new Label(parent, SWT.NONE);
		l.setText(label);
		GridData gd = new GridData();
		gd.horizontalAlignment = GridData.BEGINNING;
		l.setLayoutData(gd);

		Text text = new Text(parent, SWT.SINGLE | SWT.BORDER);
		gd = new GridData();
		gd.horizontalAlignment = GridData.FILL;
		gd.grabExcessHorizontalSpace = true;
		text.setLayoutData(gd);

		if (addFiller) {
			Label filler = new Label(parent, SWT.NONE);
			gd = new GridData();
			gd.horizontalAlignment = GridData.FILL;
			filler.setLayoutData(gd);
		}
		return text;
	}

	private Combo createMatchCombo(Composite parent) {
		Label l = new Label(parent, SWT.NONE);
		l.setText(PDEPlugin.getResourceString(KEY_MATCH));
		GridData gd = new GridData();
		gd.horizontalAlignment = GridData.BEGINNING;
		l.setLayoutData(gd);

		Combo combo = new Combo(parent, SWT.BORDER | SWT.READ_ONLY);
		gd = new GridData();
		gd.horizontalAlignment = GridData.FILL;
		gd.grabExcessHorizontalSpace = true;
		combo.setLayoutData(gd);
		String[] items =
			new String[] {
				"",
				PDEPlugin.getResourceString(KEY_MATCH_EQUIVALENT),
				PDEPlugin.getResourceString(KEY_MATCH_COMPATIBLE),
				PDEPlugin.getResourceString(KEY_MATCH_PERFECT),
				PDEPlugin.getResourceString(KEY_MATCH_GREATER)};

		combo.setItems(items);
		combo.select(0);
		combo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent event) {
				verifyPluginFields();
			}
		});

		Label filler = new Label(parent, SWT.NONE);
		gd = new GridData();
		gd.horizontalAlignment = GridData.FILL;
		filler.setLayoutData(gd);
		return combo;
	}

	private boolean isVersionValid(String version) {
		try {
			new PluginVersionIdentifier(version);
		} catch (Exception e) {
			return false;
		}
		return true;
	}
	
	private void presetFields() {
		String name = projectProvider.getProjectName();
		String noSpaceName = removeSpaces(name);
		int loc = name.lastIndexOf('.');
		String lastSegment = name;
		if (loc != -1) {
			StringBuffer buf = new StringBuffer(name.substring(loc + 1));
			buf.setCharAt(0, Character.toUpperCase(buf.charAt(0)));
			lastSegment = buf.toString();
		}
		String noSpaceLastSegment = removeSpaces(lastSegment);

		if (!fragment) {
			classField.setText(noSpaceName + "." + noSpaceLastSegment.toString() + "Plugin");
		}
		versionField.setText("1.0.0");
		if (fragment)
			nameField.setText(
				PDEPlugin.getFormattedMessage(KEY_INITIAL_FNAME, lastSegment));
		else
			nameField.setText(PDEPlugin.getFormattedMessage(KEY_INITIAL_NAME, lastSegment));

		loc = name.indexOf('.');
		if (loc == -1)
			return;
		String firstSegment = name.substring(0, loc);
		if (firstSegment.toLowerCase().equals("com")) {
			// This is a reverse URL - use second segment
			// as the vendor name
			String provider = name.substring(loc + 1);
			loc = provider.indexOf('.');
			if (loc != -1) {
				provider = provider.substring(0, loc);
				providerField.setText(provider.toUpperCase());
			} else {
				provider = "";
			}
		}
		evalPageComplete();
	}
	
	private String removeSpaces(String name) {
		StringBuffer buf = new StringBuffer();
		for (int i=0; i<name.length(); i++) {
			char c = name.charAt(i);
			if (c!=' ')
				buf.append(c);
		}
		return buf.toString();
	}

	public FieldData createFieldData() {
		FieldData data = new FieldData();
		data.name = nameField.getText();
		PluginVersionIdentifier pvi =
			new PluginVersionIdentifier(versionField.getText());
		data.version = pvi.toString();
		data.provider = providerField.getText();
		data.fragment = fragment;
		if (fragment) {
			data.pluginId = pluginIdField.getText();
			try {
				PluginVersionIdentifier fvi =
					new PluginVersionIdentifier(pluginVersionField.getText());
				data.pluginVersion = fvi.toString();
			} catch (NumberFormatException e) {
				data.pluginVersion = pluginVersionField.getText();
			}
			data.match = matchCombo.getSelectionIndex();
		} else {
			data.doMain = generateMainClass.getSelection();
			data.className = classField.getText();
			data.thisCheck = thisCheck.getSelection();
			data.bundleCheck = bundleCheck.getSelection();
			data.workspaceCheck = workspaceCheck.getSelection();
		}
		return data;
	}	
	
	public WorkspacePluginModelBase createPluginManifest(IProject project, FieldData data, ArrayList dependencies, IProgressMonitor monitor) throws CoreException {
		WorkspacePluginModelBase model;
		IFile file = project.getFile(fragment?"fragment.xml":"plugin.xml");
		
		if (fragment) model = new WorkspaceFragmentModel(file);
		else model = new WorkspacePluginModel(file);
		IPluginBase plugin = model.getPluginBase(true);
		plugin.setId(structureData.getPluginId());
		plugin.setName(data.name);
		plugin.setProviderName(data.provider);
		plugin.setVersion(data.version);
		if (fragment) {
			IFragment fragment = (IFragment)plugin;
			fragment.setPluginId(data.pluginId);
			fragment.setPluginVersion(data.pluginVersion);
			fragment.setRule(data.match);
		}
		else {
			((IPlugin)plugin).setClassName(data.className);
		}
		
		if (structureData.getRuntimeLibraryName() != null) {
			// add library
			IPluginLibrary library = model.getPluginFactory().createLibrary();
			library.setName(structureData.getRuntimeLibraryName());
			library.setExported(true);
			plugin.add(library);
		}
		
		for (int i=0; i<dependencies.size(); i++) {
			IPluginReference ref = (IPluginReference)dependencies.get(i);
			IPluginImport iimport = model.getPluginFactory().createImport();
			iimport.setId(ref.getId());
			iimport.setVersion(ref.getVersion());
			iimport.setMatch(ref.getMatch());
			plugin.add(iimport);
		}
		IFile buildFile = project.getFile("build.properties");
		if (buildFile.exists()) {
			WorkspaceBuildModel buildModel = new WorkspaceBuildModel(buildFile);
			buildModel.load();
			model.setBuildModel(buildModel);
		}
		if (data.doMain)
			generatePluginClass(project, data, monitor);
		return model;
	}

	public void generatePluginClass(
		IProject project,
		FieldData data,
		IProgressMonitor monitor)
		throws CoreException {
		String fullyQualifiedClassName = data.className;
		if (fullyQualifiedClassName == null || fullyQualifiedClassName.length() == 0)
			return;
		int flags = 0;
		if (data.thisCheck)
			flags |= PluginClassCodeGenerator.F_THIS;
		if (data.workspaceCheck)
			flags |= PluginClassCodeGenerator.F_WORKSPACE;
		if (data.bundleCheck)
			flags |= PluginClassCodeGenerator.F_BUNDLES;
		String sourceFolder = structureData.getSourceFolderName();
		IPath folderPath = project.getFullPath().append(sourceFolder);
		IFolder folder = project.getWorkspace().getRoot().getFolder(folderPath);
		PluginClassCodeGenerator generator =
			new PluginClassCodeGenerator(folder, fullyQualifiedClassName, flags);

		monitor.subTask(
			PDEPlugin.getFormattedMessage(KEY_CREATING, fullyQualifiedClassName));
		generator.generate(monitor);
		monitor.worked(1);
	}
	
	public IPluginReference [] getDependencies() {
		IPluginReference [] dependencies = new IPluginReference[2];
		dependencies[0] = new PluginReference("org.eclipse.core.resources", null, 0);
		dependencies[1] = new PluginReference("org.eclipse.ui", null, 0);
		return dependencies;
	}
	
	public IPluginStructureData getStructureData() {
		return structureData;
	}

	public void evalPageComplete(){
		setPageComplete(pluginFieldsStatus && versionStatus && classStatus);
		
	}
	
	protected void setClassError(String err){
		classError = err;
		evalErrorMsg();
	}
	
	protected void setVersionError(String err){
		versionError = err;
		evalErrorMsg();
	}
	
	protected void evalErrorMsg(){
		if (versionError!=null)
			setErrorMessage(versionError);
		else if (classError!=null)
			setErrorMessage(classError);
		else
			setErrorMessage(null);
	}
	
}
