package org.eclipse.pde.internal.wizards.project;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.internal.base.model.build.*;
import org.eclipse.pde.internal.model.build.*;
import org.eclipse.ui.*;
import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.pde.internal.model.jars.*;
import java.lang.reflect.*;
import org.eclipse.ui.actions.*;
import org.eclipse.jface.operation.*;
import org.eclipse.core.resources.*;
import org.eclipse.ui.plugin.*;
import org.eclipse.jface.preference.*;
import org.eclipse.core.runtime.*;
import java.util.*;
import org.eclipse.jdt.core.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.pde.internal.base.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.swt.*;
import org.eclipse.pde.internal.wizards.*;
import org.eclipse.pde.internal.*;

public class ProjectStructurePage extends WizardPage {
	public static final String PROP_JDK = "org.eclipse.jdt.ui.build.jdk.library";
	private IProjectProvider provider;
	private boolean fragment;
	private Text buildOutputText;
	private static final String KEY_TITLE = "ProjectStructurePage.title";
	private static final String KEY_OUTPUT = "ProjectStructurePage.output";

	private static final String KEY_FLIBRARY = "ProjectStructurePage.flibrary";
	private static final String KEY_LIBRARY = "ProjectStructurePage.library";
	private static final String KEY_CREATING = "ProjectStructurePage.creating";
	private static final String KEY_SOURCE = "ProjectStructurePage.source";
	private static final String KEY_SOURCE_NAME = "ProjectStructurePage.sourceName";
	private static final String KEY_FSOURCE_NAME = "ProjectStructurePage.fsourceName";

	private static final String KEY_FTITLE = "ProjectStructurePage.ftitle";
	private static final String KEY_DESC = "ProjectStructurePage.desc";
	private static final String KEY_FDESC = "ProjectStructurePage.fdesc";
	private Text sourceText;
	private Text libraryText;

	class StructureData implements IPluginStructureData {
		String buildOutput;
		String library;
		String source;
		IPath jdkPath;
		IPath[] jdkSourceAnnotation;

		public IPath getJDKPath() {
			return jdkPath;
		}
		public IPath[] getJDKSourceAnnotation() {
			return jdkSourceAnnotation;
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
	}

public ProjectStructurePage(IProjectProvider provider, boolean fragment) {
	super("projectStructure");
	this.fragment = fragment;
	this.provider = provider;
	if (fragment) {
		setTitle(PDEPlugin.getResourceString(KEY_FTITLE));
		setDescription(PDEPlugin.getResourceString(KEY_FDESC));
	} else {
		setTitle(PDEPlugin.getResourceString(KEY_TITLE));
		setDescription(PDEPlugin.getResourceString(KEY_DESC));
	}
}

private void createProject(IProject project, IProgressMonitor monitor) 
                      throws CoreException {
	if (project.exists() == false) {
		CoreUtility.createProject(project, provider.getLocationPath(),
											monitor);
		project.open(monitor);
	}
	if (!project.hasNature(JavaCore.NATURE_ID))
		CoreUtility.addNatureToProject(project, JavaCore.NATURE_ID, monitor);
	if (!project.hasNature(PDEPlugin.PLUGIN_NATURE))
		CoreUtility.addNatureToProject(project, PDEPlugin.PLUGIN_NATURE, monitor);
	PDEPlugin.registerPlatformLaunchers(project);
}

private void createBuildProperties(IProject project, String library, String source) throws CoreException {
	String fileName = "build.properties";
	IPath path = project.getFullPath().append(fileName);
	IFile file = project.getWorkspace().getRoot().getFile(path);
	WorkspaceBuildModel model = new WorkspaceBuildModel(file);
	String libKey = IBuildEntry.JAR_PREFIX + library;
	IBuildEntry entry = model.getFactory().createEntry(libKey);
	entry.addToken(source);
	model.getBuild().add(entry);
	model.save();
	IWorkbench workbench = PlatformUI.getWorkbench();
	workbench.getEditorRegistry().setDefaultEditor(file, PDEPlugin.BUILD_EDITOR_ID);
}
public void createControl(Composite parent) {
	Composite container = new Composite(parent, SWT.NULL);
	GridLayout layout = new GridLayout();
	layout.numColumns = 2;
	layout.verticalSpacing = 9;
	container.setLayout(layout);
	Label label;
	GridData gd;

	label = new Label(container, SWT.NULL);
	label.setText(PDEPlugin.getResourceString(KEY_OUTPUT));
	buildOutputText = new Text(container, SWT.SINGLE | SWT.BORDER);
	gd = new GridData(GridData.FILL_HORIZONTAL);
	buildOutputText.setLayoutData(gd);
	buildOutputText.setEditable(false);

	label = new Label(container, SWT.NULL);
	if (fragment)
		label.setText(PDEPlugin.getResourceString(KEY_FLIBRARY));
	else
		label.setText(PDEPlugin.getResourceString(KEY_LIBRARY));
	libraryText = new Text(container, SWT.SINGLE | SWT.BORDER);
	gd = new GridData(GridData.FILL_HORIZONTAL);
	libraryText.setLayoutData(gd);

	label = new Label(container, SWT.NULL);
	label.setText(PDEPlugin.getResourceString(KEY_SOURCE));
	sourceText = new Text(container, SWT.SINGLE | SWT.BORDER);
	gd = new GridData(GridData.FILL_HORIZONTAL);
	sourceText.setLayoutData(gd);
	initialize();
	setControl(container);
}
private String createSourceFolderName(String lastSegment) {
	if (fragment)
		return PDEPlugin.getFormattedMessage(
			KEY_FSOURCE_NAME,
			lastSegment.toUpperCase());
	else
		return PDEPlugin.getFormattedMessage(
			KEY_SOURCE_NAME,
			lastSegment.toUpperCase());

}
public boolean finish() {
	final IProject project = provider.getProject();
	final String library = libraryText.getText();
	final String source = sourceText.getText();

	IRunnableWithProgress operation = new WorkspaceModifyOperation() {
		public void execute(IProgressMonitor monitor) {
			try {
				String message = PDEPlugin.getResourceString(KEY_CREATING);
				monitor.beginTask(message, 1);
				createProject(project, monitor);
				createBuildProperties(project, library, source);
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
	}
	catch (InvocationTargetException e) {
		PDEPlugin.logException(e);
		return false;
	}
	catch (InterruptedException e) {
		return false;
	}
	return true;
}
private IPath getJDKPath() {
	return PluginPathUpdater.getJDKPath();
}
private IPath [] getJDKSourceAnnotation() {
	return PluginPathUpdater.getJDKSourceAnnotation();
}
public IPluginStructureData getStructureData() {
	StructureData data = new StructureData();
	data.buildOutput = buildOutputText.getText();
	data.library = libraryText.getText();
	data.source = sourceText.getText();
	data.jdkPath = getJDKPath();
	data.jdkSourceAnnotation = getJDKSourceAnnotation();
	return data;
}
private void initialize() {
	String projectName = provider.getProjectName();
	String lastSegment = projectName;
	int loc = projectName.lastIndexOf('.');
	if (loc!= -1) {
		lastSegment = projectName.substring(loc+1);
	}
	buildOutputText.setText("bin");
	libraryText.setText(lastSegment+".jar");

	sourceText.setText(createSourceFolderName(lastSegment));
}
public void setVisible(boolean visible) {
	super.setVisible(visible);
	if (visible) {
		initialize();
	}
}
}
