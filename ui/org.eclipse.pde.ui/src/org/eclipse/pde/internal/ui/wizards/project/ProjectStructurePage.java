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
package org.eclipse.pde.internal.ui.wizards.project;

import java.lang.reflect.InvocationTargetException;
import java.util.StringTokenizer;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.core.build.IBuildEntry;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.internal.PDE;
import org.eclipse.pde.internal.core.CoreUtility;
import org.eclipse.pde.internal.core.build.WorkspaceBuildModel;
import org.eclipse.pde.internal.core.plugin.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.ui.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.ide.*;

public class ProjectStructurePage extends WizardPage {
	private static final String KEY_TITLE = "ProjectStructurePage.title";
	private static final String KEY_OUTPUT = "ProjectStructurePage.output";
	private static final String KEY_FID = "ProjectStructurePage.fid";
	private static final String KEY_ID = "ProjectStructurePage.id";
	private static final String KEY_INVALID_ID =
		"WizardIdProjectCreationPage.invalidId";
	private static final String KEY_EMPTY_ID =
		"WizardIdProjectCreationPage.emptyId";
	private static final String KEY_FLIBRARY = "ProjectStructurePage.flibrary";
	private static final String KEY_LIBRARY = "ProjectStructurePage.library";
	private static final String KEY_CREATING = "ProjectStructurePage.creating";
	private static final String KEY_SOURCE = "ProjectStructurePage.source";
	private static final String KEY_R21 = "ProjectStructurePage.r21";

	private static final String KEY_FTITLE = "ProjectStructurePage.ftitle";
	private static final String KEY_DESC = "ProjectStructurePage.desc";
	private static final String KEY_FDESC = "ProjectStructurePage.fdesc";

	private static final String KEY_JAVA_PROJECT =
		"ProjectStructurePage.javaProject";
	private static final String KEY_SIMPLE_PROJECT =
		"ProjectStructurePage.simpleProject";

	private IProjectProvider provider;
	private boolean fragment;
	private Text idText;
	private Text buildOutputText;
	private Text sourceText;
	private Text libraryText;
	private Composite bottomContainer;
	private Button simpleChoice;
	private Button r21Check;
	private StructureData data;
	private String projectName;
	private Label libraryLabel;
	private	Label sourceLabel;
	private Label buildOutputLabel;

	class StructureData implements IPluginStructureData {
		String pluginId;
		String buildOutput;
		String library;
		String source;
		IPath jrePath;
		IPath[] jreSourceAnnotation;
		boolean r3Compatible;

		public String getPluginId() {
			return pluginId;
		}

		public IPath getJREPath() {
			return jrePath;
		}
		public IPath[] getJRESourceAnnotation() {
			return jreSourceAnnotation;
		}
		public String getJavaBuildFolderName() {
			return buildOutput;
		}
		public String getSourceFolderName() {
			return source;
		}
		public String getRuntimeLibraryName() {
			return library;
		}
		public boolean isR3Compatible() {
			return r3Compatible;
		}
	}

	public ProjectStructurePage(IProjectProvider provider, boolean fragment) {
		super("projectStructure");
		this.fragment = fragment;
		this.provider = provider;
		setTitle(
			PDEPlugin.getResourceString(fragment ? KEY_FTITLE : KEY_TITLE));
		setDescription(
			PDEPlugin.getResourceString(fragment ? KEY_FDESC : KEY_DESC));
	}

	public static void createProject(
		IProject project,
		IProjectProvider provider,
		IPluginStructureData data,
		IProgressMonitor monitor)
		throws CoreException {

		if (project.exists() == false) {
			CoreUtility.createProject(
				project,
				provider.getLocationPath(),
				monitor);
			project.open(monitor);
		}
		if (!project.hasNature(PDE.PLUGIN_NATURE))
			CoreUtility.addNatureToProject(project, PDE.PLUGIN_NATURE, monitor);

		if (!project.hasNature(JavaCore.NATURE_ID)
			&& data.getRuntimeLibraryName() != null) {
			CoreUtility.addNatureToProject(
				project,
				JavaCore.NATURE_ID,
				monitor);
			JavaCore.create(project).setOutputLocation(
				project.getFullPath().append(data.getJavaBuildFolderName()),
				monitor);
		}

	}

	public static void createBuildProperties(
		IProject project,
		IPluginStructureData data,
		boolean fragment,
		IProgressMonitor monitor)
		throws CoreException {
		createBuildProperties(
			project,
			data.getRuntimeLibraryName(),
			data.getSourceFolderName(),
			fragment);
	}

	public static void createBuildProperties(
		IProject project,
		String library,
		String source,
		boolean fragment)
		throws CoreException {
		String fileName = "build.properties";
		IPath path = project.getFullPath().append(fileName);
		IFile file = project.getWorkspace().getRoot().getFile(path);
		if (!file.exists()) {
			WorkspaceBuildModel model = new WorkspaceBuildModel(file);
			IBuildEntry ientry = model.getFactory().createEntry("bin.includes");
			if (fragment)
				ientry.addToken("fragment.xml");
			else
				ientry.addToken("plugin.xml");

			if (library != null && source != null) {
				IBuildEntry entry =
					model.getFactory().createEntry(
						IBuildEntry.JAR_PREFIX + library);
				if (!source.endsWith("/"))
					source += "/";
				entry.addToken(source);
				ientry.addToken("*.jar");
				ientry.addToken(library);
				model.getBuild().add(entry);
			}
			model.getBuild().add(ientry);
			model.save();
		}
		IDE.setDefaultEditor(file, PDEPlugin.BUILD_EDITOR_ID);
	}

	private void addTopSection(Composite container) {
		Composite topContainer = new Composite(container, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		topContainer.setLayout(layout);
		topContainer.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Label label = new Label(topContainer, SWT.NULL);
		label.setText(PDEPlugin.getResourceString(fragment ? KEY_FID : KEY_ID));
		idText = new Text(topContainer, SWT.SINGLE | SWT.BORDER);
		idText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		idText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				verifyId(idText.getText());
			}
		});
		
		Label spacer = new Label(topContainer, SWT.NULL);
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		spacer.setLayoutData(gd);
	}

	private void addMiddleSection(Composite container) {
		Composite middleContainer = new Composite(container, SWT.NONE);
		GridLayout layout = new GridLayout();
		//layout.numColumns = 2;
		middleContainer.setLayout(layout);
		middleContainer.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		simpleChoice = new Button(middleContainer, SWT.RADIO);
		simpleChoice.setText(PDEPlugin.getResourceString(KEY_SIMPLE_PROJECT));
		simpleChoice.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				boolean isSelected = ((Button) e.widget).getSelection();
				libraryText.setEnabled(!isSelected);
				sourceText.setEnabled(!isSelected);
				buildOutputText.setEnabled(!isSelected);
				libraryLabel.setEnabled(!isSelected);
				sourceLabel.setEnabled(!isSelected);
				buildOutputLabel.setEnabled(!isSelected);
				getContainer().updateButtons();
			}
		});

		Button button = new Button(middleContainer, SWT.RADIO);
		button.setText(PDEPlugin.getResourceString(KEY_JAVA_PROJECT));
		button.setSelection(true);
	}

	private void addBottomSection(Composite container) {
		bottomContainer = new Composite(container, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.verticalSpacing = 9;
		bottomContainer.setLayout(layout);
		bottomContainer.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		libraryLabel = new Label(bottomContainer, SWT.NULL);
		libraryLabel.setText(
			PDEPlugin.getResourceString(fragment ? KEY_FLIBRARY : KEY_LIBRARY));
		GridData gd = new GridData();
		gd.horizontalIndent = 25;
		libraryLabel.setLayoutData(gd);
		libraryText = new Text(bottomContainer, SWT.SINGLE | SWT.BORDER);
		libraryText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		sourceLabel = new Label(bottomContainer, SWT.NULL);
		sourceLabel.setText(PDEPlugin.getResourceString(KEY_SOURCE));
		gd = new GridData();
		gd.horizontalIndent = 25;
		sourceLabel.setLayoutData(gd);
		sourceText = new Text(bottomContainer, SWT.SINGLE | SWT.BORDER);
		sourceText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		buildOutputLabel = new Label(bottomContainer, SWT.NULL);
		buildOutputLabel.setText(PDEPlugin.getResourceString(KEY_OUTPUT));
		gd = new GridData();
		gd.horizontalIndent = 25;
		buildOutputLabel.setLayoutData(gd);
		buildOutputText = new Text(bottomContainer, SWT.SINGLE | SWT.BORDER);
		buildOutputText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		Label spacer = new Label(bottomContainer, SWT.NULL);
		gd = new GridData();
		gd.horizontalSpan = 2;
		spacer.setLayoutData(gd);
		
		r21Check = new Button(bottomContainer, SWT.CHECK);
		r21Check.setText(PDEPlugin.getResourceString(KEY_R21));
		gd = new GridData();
		gd.horizontalSpan = 2;
		r21Check.setLayoutData(gd);
	}

	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.verticalSpacing = 0;
		container.setLayout(layout);

		addTopSection(container);
		addMiddleSection(container);
		addBottomSection(container);

		initialize();
		setControl(container);
		Dialog.applyDialogFont(container);
		if (fragment)
			WorkbenchHelp.setHelp(
				container,
				IHelpContextIds.NEW_FRAGMENT_STRUCTURE_PAGE);
		else
			WorkbenchHelp.setHelp(
				container,
				IHelpContextIds.NEW_PROJECT_STRUCTURE_PAGE);
	}

	private boolean verifyId(String id) {
		String error =
			(id.length() == 0)
				? PDEPlugin.getResourceString(KEY_EMPTY_ID)
				: verifyIdRules(id);
		setErrorMessage(error);
		setPageComplete(error == null);
		return (error == null);
	}

	private String verifyIdRules(String id) {
		String problemText = PDEPlugin.getResourceString(KEY_INVALID_ID);
		StringTokenizer stok = new StringTokenizer(id, ".");
		while (stok.hasMoreTokens()) {
			String token = stok.nextToken();
			for (int i = 0; i < token.length(); i++) {
				if (!Character.isLetterOrDigit(token.charAt(i)))
					return problemText;
			}
		}
		return null;
	}

	private void createBlankManifest(
		IProject project,
		IProgressMonitor monitor)
		throws CoreException {
		IPath path =
			project.getFullPath().append(
				fragment ? "fragment.xml" : "plugin.xml");
		IFile file = project.getWorkspace().getRoot().getFile(path);

		WorkspacePluginModelBase model = null;
		if (fragment)
			model = new WorkspaceFragmentModel(file);
		else
			model = new WorkspacePluginModel(file);
		model.load();

		if (!file.exists()) {
			IPluginBase pluginBase = model.getPluginBase();
			pluginBase.setId(idText.getText());
			pluginBase.setVersion("1.0.0");
			pluginBase.setName(idText.getText());
			if (!r21Check.getSelection())
				pluginBase.setSchemaVersion("3.0");
			model.save();
		}
	}

	public boolean finish() {
		final IProject project = provider.getProject();
		final IPluginStructureData data = getStructureData();

		IRunnableWithProgress operation = new WorkspaceModifyOperation() {
			public void execute(IProgressMonitor monitor) {
				try {
					String message = PDEPlugin.getResourceString(KEY_CREATING);
					monitor.beginTask(message, 1);
					createProject(project, provider, data, monitor);
					createBuildProperties(project, data, fragment, monitor);
					if (simpleChoice.getSelection())
						createBlankManifest(project, monitor);
					monitor.worked(1);
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
			return false;
		} catch (InterruptedException e) {
			return false;
		}
		return true;
	}

	private IPath[] getJRESourceAnnotation() {
		IPath source = JavaCore.getClasspathVariable("JRE_SRC");
		IPath prefix = JavaCore.getClasspathVariable("JRE_SRCROOT");
		return new IPath[] { source, prefix };
	}

	public IPluginStructureData getStructureData() {
		data = new StructureData();
		data.pluginId = idText.getText();
		data.buildOutput =
			(simpleChoice.getSelection()) ? null : buildOutputText.getText();
		data.library =
			(simpleChoice.getSelection()) ? null : libraryText.getText();
		data.source =
			(simpleChoice.getSelection()) ? null : sourceText.getText();
		data.jrePath =
			(simpleChoice.getSelection())
				? null
				: JavaCore.getClasspathVariable("JRE_SRC");
		data.jreSourceAnnotation =
			(simpleChoice.getSelection()) ? null : getJRESourceAnnotation();
		data.r3Compatible = !r21Check.getSelection();
		return data;
	}

	private void initialize() {
		if (idText.getText().equals("")) {
			projectName = provider.getProjectName();
			idText.setText(setInitialId(provider.getProjectName()));
		}
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
		r21Check.setSelection(false);
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
			idText.setFocus();
		}
	}

	public boolean isStructureDataChanged() {
		if (data == null || projectName == null)
			return false;
		StructureData oldData = data;
		StructureData newData = (StructureData) getStructureData();
		boolean structureChange =
				!oldData.pluginId.equals(newData.pluginId)
				|| !oldData.buildOutput.equals(newData.buildOutput)
				|| !oldData.library.equals(newData.library)
				|| !oldData.source.equals(newData.source)
				|| oldData.r3Compatible!=newData.r3Compatible
				|| !projectName.equals(provider.getProjectName());
		projectName = provider.getProjectName();
		return (structureChange);
	}

	public IWizardPage getNextPage() {
		if (!fragment && simpleChoice.getSelection())
			return null;
		return super.getNextPage();
	}
	
	public String getSchemaVersion() {
		return r21Check.getSelection() ? null : "3.0";
	}

}
