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
package org.eclipse.pde.internal.ui.feature;

import java.lang.reflect.*;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.*;
import org.eclipse.jdt.launching.*;
import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.operation.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.core.build.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.*;
import org.eclipse.pde.internal.build.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.build.*;
import org.eclipse.pde.internal.core.feature.*;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.feature.NewFeatureProjectWizard.*;
import org.eclipse.pde.ui.*;
import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.actions.*;
import org.eclipse.ui.ide.*;
import org.eclipse.ui.part.*;
import org.eclipse.ui.wizards.newresource.*;

public class FeatureCustomHandlerPage extends WizardPage {
	private static final String KEY_TITLE = "FeatureCustomHandlerPage.title";
	private static final String KEY_OUTPUT = "ProjectStructurePage.output";
	private static final String KEY_LIBRARY = "FeatureCustomHandlerPage.library";
	private static final String KEY_SOURCE = "ProjectStructurePage.source";
	private static final String KEY_DESC = "FeatureCustomHandlerPage.desc";
	public static final String CREATING_PROJECT =
		"NewFeatureWizard.creatingProject";
	public static final String OVERWRITE_FEATURE = "NewFeatureWizard.overwriteFeature";
	public static final String CREATING_FOLDERS =
		"NewFeatureWizard.creatingFolders";
	public static final String CREATING_MANIFEST =
		"NewFeatureWizard.creatingManifest";
	private static final String KEY_CUSTOM_INSTALL_HANDLER =
		"FeatureCustomHandlerPage.customProject";
	public static final String KEY_WTITLE = "NewFeatureWizard.wtitle";
	public static final String FEATURE_LIBRARY_ERR = "FeatureCustomHandlerPage.error.library";
	public static final String FEATURE_SOURCE_ERR = "FeatureCustomHandlerPage.error.source";
	public static final String FEATURE_OUTPUT_ERR = "FeatureCustomHandlerPage.error.output";

	private IProjectProvider provider;
	private Text buildOutputText;
	private Text sourceText;
	private Text libraryText;
	private Button customChoice;
	private StructureData data;
	private Label libraryLabel;
	private Label sourceLabel;
	private Label buildOutputLabel;
	private boolean isInitialized = false;

	class StructureData {
		String buildOutput;
		String library;
		String source;

		public String getJavaBuildFolderName() {
			return buildOutput;
		}
		public String getSourceFolderName() {
			return source;
		}
		public String getRuntimeLibraryName() {
			if (library != null && !library.endsWith(".jar"))
				library += ".jar";
			return library;
		}
	}

	public FeatureCustomHandlerPage(IProjectProvider provider) {
		super("projectStructure");
		this.provider = provider;
		setTitle(PDEPlugin.getResourceString(KEY_TITLE));
		setDescription(PDEPlugin.getResourceString(KEY_DESC));
	}
	
	protected static void addSourceFolder(String name, IProject project, IProgressMonitor monitor)
	throws CoreException {
		IPath path = project.getFullPath().append(name);
		ensureFolderExists(project, path, monitor);
		monitor.worked(1);
	}
	
	private static void ensureFolderExists(IProject project, IPath folderPath, IProgressMonitor monitor)
	throws CoreException {
		IWorkspace workspace = project.getWorkspace();

		for (int i = 1; i <= folderPath.segmentCount(); i++) {
			IPath partialPath = folderPath.uptoSegment(i);
			if (!workspace.getRoot().exists(partialPath)) {
				IFolder folder = workspace.getRoot().getFolder(partialPath);
				folder.create(true, true, null);
			}
			monitor.worked(1);
		}
		
	}
	private void createBuildProperties(IProject project)
	throws CoreException {
		StructureData structureData = getStructureData();
		String fileName = "build.properties";
		IPath path = project.getFullPath().append(fileName);
		IFile file = project.getWorkspace().getRoot().getFile(path);
		if (!file.exists()) {
			WorkspaceBuildModel model = new WorkspaceBuildModel(file);
			IBuildEntry ientry =
				model.getFactory().createEntry("bin.includes");
			ientry.addToken("feature.xml");
			String library = structureData.getRuntimeLibraryName();
			if (library != null){
				String source = structureData.getSourceFolderName();
				if (source != null) {
					IBuildEntry entry =
						model.getFactory().createEntry(
							IBuildEntry.JAR_PREFIX + library);
					if (!source.endsWith("/"))
						source += "/";
					entry.addToken(source);
					ientry.addToken(library);
					model.getBuild().add(entry);
				}
				String output = structureData.getJavaBuildFolderName();
				if (output!=null){
					IBuildEntry entry = model.getFactory().createEntry(IXMLConstants.PROPERTY_OUTPUT_PREFIX + library);
					if (!output.endsWith("/"))
						output+="/";
					entry.addToken(output);
					model.getBuild().add(entry);
				}
			}

			model.getBuild().add(ientry);
			model.save();
		}
		IDE.setDefaultEditor(file, PDEPlugin.BUILD_EDITOR_ID);
	}
	
	private IFile createFeatureManifest(
			IProject project,
			FeatureData data,
			IPluginBase[] plugins)
	throws CoreException {
		IFile file = project.getFile("feature.xml");
		WorkspaceFeatureModel model = new WorkspaceFeatureModel();
		model.setFile(file);
		IFeature feature = model.getFeature();
		String name = data.name;
		feature.setLabel(name);
		feature.setId(data.id);
		feature.setVersion(data.version);
		feature.setProviderName(data.provider);

		IFeaturePlugin[] added = new IFeaturePlugin[plugins.length];

		for (int i = 0; i < plugins.length; i++) {
			IPluginBase plugin = plugins[i];
			FeaturePlugin fplugin = (FeaturePlugin) model.getFactory().createPlugin();
			fplugin.loadFrom(plugin);
			added[i] = fplugin;
		}
		feature.addPlugins(added);
		feature.computeImports();
		IFeatureInstallHandler handler = feature.getInstallHandler();
		if (handler == null){
			handler = feature.getModel().getFactory().createInstallHandler();
			feature.setInstallHandler(handler);
		}
		StructureData structureData = getStructureData();
		handler.setLibrary(structureData.getRuntimeLibraryName());
			
		// Save the model
		model.save();
		model.dispose();
		IDE.setDefaultEditor(file, PDEPlugin.FEATURE_EDITOR_ID);
		return file;
	}
	
	private void createFeatureProject(
			IProject project,
			IPath location,
			FeatureData data,
			IPluginBase[] plugins,
			IProgressMonitor monitor)
	throws CoreException {
		
		monitor.beginTask(PDEPlugin.getResourceString(CREATING_PROJECT), 3);
		StructureData structureData = getStructureData();
		boolean overwrite = true;
		if (location.append(project.getName()).toFile().exists()) {
			overwrite =
				MessageDialog.openQuestion(
						PDEPlugin.getActiveWorkbenchShell(),
						PDEPlugin.getResourceString(KEY_WTITLE),
						PDEPlugin.getResourceString(OVERWRITE_FEATURE));
		}
		if (overwrite) {
			CoreUtility.createProject(project, location, monitor);
			project.open(monitor);
			IProjectDescription desc =
				project.getWorkspace().newProjectDescription(project.getName());
			desc.setLocation(provider.getLocationPath());
			if (!project.hasNature(PDE.PLUGIN_NATURE))
				CoreUtility.addNatureToProject(project, PDE.PLUGIN_NATURE, monitor);

			if (!project.hasNature(JavaCore.NATURE_ID)
					&& customChoice.getSelection()) {
				CoreUtility.addNatureToProject(
						project,
						JavaCore.NATURE_ID,
						monitor);
				JavaCore.create(project).setOutputLocation(
						project.getFullPath().append(structureData.getJavaBuildFolderName()),
						monitor);
				JavaCore.create(project).setRawClasspath(
						new IClasspathEntry[] {
							JavaCore.newContainerEntry(new Path(JavaRuntime.JRE_CONTAINER)),
							JavaCore.newSourceEntry(
									project.getFullPath().append(structureData.getSourceFolderName()))},
									monitor);
				addSourceFolder(structureData.getSourceFolderName(), project, monitor);
			}

			monitor.subTask(PDEPlugin.getResourceString(CREATING_MANIFEST));
			monitor.worked(1);
			createBuildProperties(project);
			monitor.worked(1);
			// create feature.xml
			IFile file = createFeatureManifest(project, data, plugins);
			monitor.worked(1);
			// open manifest for editing
			openFeatureManifest(file);
		} else {
			project.create(monitor);
			project.open(monitor);
			IFile featureFile = project.getFile("feature.xml");
			if (featureFile.exists())
				openFeatureManifest(featureFile);
			monitor.worked(3);
		}

	}

	private void addCustomInstallHandlerSection(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);
		container.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		customChoice = new Button(container, SWT.CHECK);
		customChoice.setText(PDEPlugin.getResourceString(KEY_CUSTOM_INSTALL_HANDLER));
		customChoice.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				boolean isSelected = ((Button) e.widget).getSelection();
				libraryText.setEnabled(isSelected);
				sourceText.setEnabled(isSelected);
				buildOutputText.setEnabled(isSelected);
				libraryLabel.setEnabled(isSelected);
				sourceLabel.setEnabled(isSelected);
				buildOutputLabel.setEnabled(isSelected);
				getContainer().updateButtons();
			}
		});
	}

	private void addCustomInstallHandlerPropertiesSection(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.verticalSpacing = 9;
		container.setLayout(layout);
		container.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		libraryLabel = new Label(container, SWT.NULL);
		libraryLabel.setText(
			PDEPlugin.getResourceString(KEY_LIBRARY));
		GridData gd = new GridData();
		gd.horizontalIndent = 25;
		libraryLabel.setLayoutData(gd);
		libraryText = new Text(container, SWT.SINGLE | SWT.BORDER);
		libraryText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		libraryText.addModifyListener(new ModifyListener(){
			public void modifyText(ModifyEvent e) {
				if (libraryText.getText().length() == 0)
					setPageComplete(false);
				evalErrMsg();
			}	
		});

		sourceLabel = new Label(container, SWT.NULL);
		sourceLabel.setText(PDEPlugin.getResourceString(KEY_SOURCE));
		gd = new GridData();
		gd.horizontalIndent = 25;
		sourceLabel.setLayoutData(gd);
		sourceText = new Text(container, SWT.SINGLE | SWT.BORDER);
		sourceText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		sourceText.addModifyListener(new ModifyListener(){
			public void modifyText(ModifyEvent e){
				if (sourceText.getText().length() == 0)
					setPageComplete(false);
				evalErrMsg();
			}
		});
		
		buildOutputLabel = new Label(container, SWT.NULL);
		buildOutputLabel.setText(PDEPlugin.getResourceString(KEY_OUTPUT));
		gd = new GridData();
		gd.horizontalIndent = 25;
		buildOutputLabel.setLayoutData(gd);
		buildOutputText = new Text(container, SWT.SINGLE | SWT.BORDER);
		buildOutputText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		buildOutputText.addModifyListener(new ModifyListener(){
			public void modifyText(ModifyEvent e){
				if (buildOutputText.getText().length() == 0)
					setPageComplete(false);
				evalErrMsg();
			}
		});
	}
	
	private void evalErrMsg(){
		if (!customChoice.getSelection())
			return;
		if (libraryText.getText().length() == 0)
			setErrorMessage(PDEPlugin.getResourceString(FEATURE_LIBRARY_ERR));
		else if (sourceText.getText().length() == 0)
			setErrorMessage(PDEPlugin.getResourceString(FEATURE_SOURCE_ERR));
		else if (buildOutputText.getText().length() == 0)
			setErrorMessage(PDEPlugin.getResourceString(FEATURE_OUTPUT_ERR));
		else{
			setErrorMessage(null);
			setPageComplete(true);
		}
	}
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.verticalSpacing = 0;
		container.setLayout(layout);

		addCustomInstallHandlerSection(container);
		addCustomInstallHandlerPropertiesSection(container);

		initialize();
		setControl(container);
		Dialog.applyDialogFont(container);
	}

	public boolean finish() {
		final IProject project = ((FeatureProjectProvider)provider).getProject();
		final IPath location = ((FeatureProjectProvider)provider).getLocationPath();
		final FeatureData data = ((FeatureProjectProvider)provider).getFeatureData();
		final IPluginBase[] plugins =
			((FeatureProjectProvider)provider).getPluginListSelection() != null
			? ((FeatureProjectProvider)provider).getPluginListSelection()
			: (new IPluginBase[0]);
			IRunnableWithProgress operation = new WorkspaceModifyOperation() {
				public void execute(IProgressMonitor monitor) {
					try {
						createFeatureProject(project, location, data, plugins, monitor);
					} catch (CoreException e) {
						PDEPlugin.logException(e);
					} finally {
						monitor.done();
					}
				}
			};
			try {
				getContainer().run(false, true, operation);
				BasicNewProjectResourceWizard.updatePerspective(((FeatureProjectProvider)provider).getConfigElement());
			} catch (InvocationTargetException e) {
				PDEPlugin.logException(e);
				return false;
			} catch (InterruptedException e) {
				return false;
			}
			return true;
	}
	public StructureData getStructureData() {
		data = new StructureData();
		data.buildOutput =
			(!customChoice.getSelection()) ? null : buildOutputText.getText();
		data.library =
			(!customChoice.getSelection()) ? null : libraryText.getText();
		data.source =
			(!customChoice.getSelection()) ? null : sourceText.getText();
		return data;
	}

	public boolean isInitialized(){
		return isInitialized;
	}
	private void initialize() {
		if (isInitialized)
			return;
		customChoice.setSelection(false);
		if (buildOutputText.getText().equals(""))
			buildOutputText.setText("bin");
		if (libraryText.getText().equals("")
			|| libraryText.getText().equals(".jar")) {
			String lastSegment = setInitialId(provider.getProjectName());
			int loc = lastSegment.lastIndexOf('.');
			if (loc != -1) {
				lastSegment = lastSegment.substring(loc + 1);
			}
			libraryText.setText(lastSegment + ".jar");
		}
		if (sourceText.getText().equals(""))
			sourceText.setText("src");
		if (customChoice != null){
			libraryText.setEnabled(customChoice.getSelection());
			sourceText.setEnabled(customChoice.getSelection());
			buildOutputText.setEnabled(customChoice.getSelection());
			libraryLabel.setEnabled(customChoice.getSelection());
			sourceLabel.setEnabled(customChoice.getSelection());
			buildOutputLabel.setEnabled(customChoice.getSelection());
		}

	}
	public boolean isPageComplete() {
		if (!customChoice.getSelection())
			return true;
		// java choice selected
		return (libraryText.getText().length() > 0);
	}
	private void openFeatureManifest(IFile manifestFile) {
		IWorkbenchPage page = PDEPlugin.getActivePage();
		// Reveal the file first
		final ISelection selection = new StructuredSelection(manifestFile);
		final IWorkbenchPart activePart = page.getActivePart();

		if (activePart instanceof ISetSelectionTarget) {
			getShell().getDisplay().asyncExec(new Runnable() {
				public void run() {
					((ISetSelectionTarget) activePart).selectReveal(selection);
				}
			});
		}
		// Open the editor

		FileEditorInput input = new FileEditorInput(manifestFile);
		String id = PDEPlugin.FEATURE_EDITOR_ID;
		try {
			page.openEditor(input, id);
		} catch (PartInitException e) {
			PDEPlugin.logException(e);
		}
	}
	private String setInitialId(String projectName) {
		StringBuffer buffer = new StringBuffer();
		StringTokenizer stok = new StringTokenizer(projectName, ".");
		while (stok.hasMoreTokens()) {
			String token = stok.nextToken();
			for (int i = 0; i < token.length(); i++) {
				if (Character.isLetterOrDigit(token.charAt(i)))
					buffer.append(token.charAt(i));
			}
			if (stok.hasMoreTokens()
				&& buffer.charAt(buffer.length() - 1) != '.')
				buffer.append(".");
		}
		return buffer.toString();
	}

	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			initialize();
			isInitialized=true;
		}
	}
}
