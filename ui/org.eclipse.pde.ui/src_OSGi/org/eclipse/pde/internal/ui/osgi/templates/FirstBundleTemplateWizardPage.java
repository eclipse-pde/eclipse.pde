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
package org.eclipse.pde.internal.ui.osgi.templates;

import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.build.*;
import org.eclipse.pde.internal.core.osgi.bundle.*;
import org.eclipse.pde.internal.core.plugin.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.wizards.*;
import org.eclipse.pde.internal.ui.wizards.templates.*;
import org.eclipse.pde.internal.ui.wizards.templates.PluginReference;
import org.eclipse.pde.ui.*;
import org.eclipse.pde.ui.templates.*;
import org.eclipse.swt.*;
import org.eclipse.swt.custom.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.help.*;

public class FirstBundleTemplateWizardPage extends WizardPage implements IFirstWizardPage {
	private static final String KEY_TITLE = "DefaultBundleCodeGenerationPage.title";
	private static final String KEY_FTITLE = "DefaultBundleCodeGenerationPage.ftitle";
	private static final String KEY_ID_NOT_SET =
		"DefaultCodeGenerationPage.idNotSet";
	private static final String KEY_VERSION_FORMAT =
		"DefaultCodeGenerationPage.versionFormat";
	private static final String KEY_INVALID_ID =
		"DefaultCodeGenerationPage.invalidId";
	private static final String KEY_DESC = "DefaultBundleCodeGenerationPage.desc";
	private static final String KEY_FDESC = "DefaultBundleCodeGenerationPage.fdesc";
	private static final String KEY_FNAME = "DefaultBundleCodeGenerationPage.fname";
	private static final String KEY_NAME = "DefaultBundleCodeGenerationPage.name";
	private static final String KEY_VERSION = "DefaultBundleCodeGenerationPage.version";
	private static final String KEY_PROVIDER =
		"DefaultBundleCodeGenerationPage.providerName";
	private static final String KEY_PLUGIN_ID =
		"DefaultBundleCodeGenerationPage.pluginId";
	private static final String KEY_BROWSE =
		"DefaultBundleCodeGenerationPage.pluginId.browse";
	private static final String KEY_PLUGIN_VERSION =
		"DefaultBundleCodeGenerationPage.pluginVersion";

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

	private static final String KEY_CLASS = "DefaultBundleCodeGenerationPage.class";
	private static final String KEY_GENERATE = "DefaultBundleCodeGenerationPage.generate";
	private static final String KEY_INITIAL_NAME =
		"DefaultBundleCodeGenerationPage.initialName";
	private static final String KEY_INITIAL_FNAME =
		"DefaultBundleCodeGenerationPage.initialFName";
	private static final String KEY_CREATING = "DefaultBundleCodeGenerationPage.creating";
	private static final String KEY_OPTIONS = "DefaultBundleCodeGenerationPage.options";
	private static final String KEY_OPTIONS_THIS =
		"DefaultBundleCodeGenerationPage.options.this";
	private static final String KEY_OPTIONS_BUNDLE =
		"DefaultBundleCodeGenerationPage.options.bundle";
	private static final String KEY_OPTIONS_WORKSPACE =
		"DefaultBundleCodeGenerationPage.options.workspace";
		
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

	public FirstBundleTemplateWizardPage(
		IProjectProvider projectProvider,
		IPluginStructureData structureData,
		boolean fragment) {
		super(FirstTemplateWizardPage.PAGE_ID);
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
					new PluginSelectionDialog(pluginIdField.getShell(), false, false);
				dialog.create();
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
			thisCheck.addSelectionListener(new SelectionAdapter(){
				public void widgetSelected(SelectionEvent e){
					boolean value = thisCheck.getSelection();
					bundleCheck.setEnabled(value);
				}
			});
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
	public IFieldData createFieldData(ITemplateSection[] activeSections){
		FieldData data = (FieldData)createFieldData();
		data.setHasPreference(false);
		// only need to set this value to true if a preference page
		// will be generated and there is no default instance access
		if (thisCheck.getSelection())
			return data;
		
		for (int i =0 ; i<activeSections.length; i++){
			if (activeSections[i].getLabel().equals("Preference Page")){
				data.setHasPreference(true);
				continue;
			}
		}	
		return data;
	}
	public IFieldData createFieldData() {
		FieldData data = new FieldData();
		data.setName(nameField.getText());
		PluginVersionIdentifier pvi =
			new PluginVersionIdentifier(versionField.getText());
		data.setVersion(pvi.toString());
		data.setProvider(providerField.getText());
		data.setFragment(fragment);
		if (fragment) {
			data.setPluginId(pluginIdField.getText());
			try {
				PluginVersionIdentifier fvi =
					new PluginVersionIdentifier(pluginVersionField.getText());
				data.setPluginVersion(fvi.toString());
			} catch (NumberFormatException e) {
				data.setPluginVersion(pluginVersionField.getText());
			}
			data.setMatch(matchCombo.getSelectionIndex());
		} else {
			data.setDoMain(generateMainClass.getSelection());
			data.setClassName(classField.getText());
			data.setThisCheck(thisCheck.getSelection());
			data.setBundleCheck(bundleCheck.getSelection() && bundleCheck.isEnabled());
			data.setWorkspaceCheck(workspaceCheck.getSelection());
		}
		return data;
	}	
	
	public BundlePluginModelBase createPluginManifest(IProject project, IFieldData data, ArrayList dependencies, IProgressMonitor monitor) throws CoreException {
		BundlePluginModelBase model;
		IFile file = project.getFile("META-INF/MANIFEST.MF");
		WorkspaceBundleModel bmodel = new WorkspaceBundleModel(file);
		file = project.getFile("extensions.xml");
		WorkspaceExtensionsModel emodel = new WorkspaceExtensionsModel(file);
		emodel.load();
		
		if (fragment) model = new BundleFragmentModel();
		else model = new BundlePluginModel();
		model.setBundleModel(bmodel);
		model.setExtensionsModel(emodel);
		IPluginBase plugin = model.getPluginBase(true);
		plugin.setId(structureData.getPluginId());
		plugin.setName(data.getName());
		plugin.setProviderName(data.getProvider());
		plugin.setVersion(data.getVersion());
		if (fragment) {
			IFragment fragment = (IFragment)plugin;
			fragment.setPluginId(data.getPluginId());
			fragment.setPluginVersion(data.getPluginVersion());
			fragment.setRule(data.getMatch());
		}
		else {
			((IPlugin)plugin).setClassName(data.getClassName());
		}
		
		if (structureData.getRuntimeLibraryName() != null) {
			// add library
			IPluginLibrary library = model.getPluginFactory().createLibrary();
			library.setName(structureData.getRuntimeLibraryName());
			library.setExported(true);
			plugin.add(library);
		}
		
		boolean hasRuntime=false;
		for (int i=0; i<dependencies.size(); i++) {
			IPluginReference ref = (IPluginReference)dependencies.get(i);
			IPluginImport iimport = model.getPluginFactory().createImport();
			iimport.setId(ref.getId());
			iimport.setVersion(ref.getVersion());
			iimport.setMatch(ref.getMatch());
			plugin.add(iimport);
		}
		if (data.getClassName()!=null && !hasRuntime) {
			IPluginImport iimport = model.getPluginFactory().createImport();
			iimport.setId("org.eclipse.core.runtime");
			plugin.add(iimport);
		}
		IFile buildFile = project.getFile("build.properties");
		if (buildFile.exists()) {
			WorkspaceBuildModel buildModel = new WorkspaceBuildModel(buildFile);
			buildModel.load();
			model.setBuildModel(buildModel);
		}
		if (data.isDoMain())
			generatePluginClass(project, data, monitor);
		return model;
	}

	public void generatePluginClass(
		IProject project,
		IFieldData data,
		IProgressMonitor monitor)
		throws CoreException {
		String fullyQualifiedClassName = data.getClassName();
		if (fullyQualifiedClassName == null || fullyQualifiedClassName.length() == 0)
			return;
		int flags = 0;
		if (data.isThisCheck())
			flags |= BundleActivatorClassCodeGenerator.F_THIS;
		if (data.isWorkspaceCheck())
			flags |= BundleActivatorClassCodeGenerator.F_WORKSPACE;
		if (data.isBundleCheck())
			flags |= BundleActivatorClassCodeGenerator.F_BUNDLES;
		if (data.hasPreference())
			flags |= BundleActivatorClassCodeGenerator.F_PREF;
		String sourceFolder = structureData.getSourceFolderName();
		IPath folderPath = project.getFullPath().append(sourceFolder);
		IFolder folder = project.getWorkspace().getRoot().getFolder(folderPath);
		BundleActivatorClassCodeGenerator generator =
			new BundleActivatorClassCodeGenerator(folder, fullyQualifiedClassName, flags);

		monitor.subTask(
			PDEPlugin.getFormattedMessage(KEY_CREATING, fullyQualifiedClassName));
		generator.generate(monitor);
		monitor.worked(1);
	}
	
	public IPluginReference [] getDependencies() {
		boolean needActivator = generateMainClass.getSelection();
		IPluginReference [] dependencies = new IPluginReference[needActivator?3:2];
		dependencies[0] = new PluginReference("org.eclipse.core.resources", null, 0);
		dependencies[1] = new PluginReference("org.eclipse.ui", null, 0);
		if (needActivator)
			dependencies[2] = new PluginReference("org.eclipse.osgi", null, 0);
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
