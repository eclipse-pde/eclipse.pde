package org.eclipse.pde.internal.wizards.project;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.model.build.*;
import org.eclipse.pde.internal.model.build.*;
import org.eclipse.ui.*;
import org.eclipse.pde.model.plugin.*;

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
import org.eclipse.pde.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.swt.*;
import org.eclipse.pde.internal.util.*;
import org.eclipse.pde.internal.wizards.*;
import org.eclipse.pde.internal.*;
import org.eclipse.jdt.launching.*;
import org.eclipse.swt.events.*;

public class ProjectStructurePage extends WizardPage {
	private static final String KEY_TITLE = "ProjectStructurePage.title";
	private static final String KEY_OUTPUT = "ProjectStructurePage.output";
	private static final String KEY_FID = "ProjectStructurePage.fid";
	private static final String KEY_ID = "ProjectStructurePage.id";
	private static final String KEY_INVALID_ID =
		"WizardIdProjectCreationPage.invalidId";

	private static final String KEY_FLIBRARY = "ProjectStructurePage.flibrary";
	private static final String KEY_LIBRARY = "ProjectStructurePage.library";
	private static final String KEY_CREATING = "ProjectStructurePage.creating";
	private static final String KEY_SOURCE = "ProjectStructurePage.source";
	private static final String KEY_SOURCE_NAME = "ProjectStructurePage.sourceName";
	private static final String KEY_FSOURCE_NAME =
		"ProjectStructurePage.fsourceName";

	private static final String KEY_FTITLE = "ProjectStructurePage.ftitle";
	private static final String KEY_DESC = "ProjectStructurePage.desc";
	private static final String KEY_FDESC = "ProjectStructurePage.fdesc";

	private IProjectProvider provider;
	private boolean fragment;
	private Text idText;
	private Text buildOutputText;
	private Text sourceText;
	private Text libraryText;

	class StructureData implements IPluginStructureData {
		String pluginId;
		String buildOutput;
		String library;
		String source;
		IPath jrePath;
		IPath[] jreSourceAnnotation;

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

	public static void createProject(IProject project, IProjectProvider provider, IProgressMonitor monitor)
		throws CoreException {
		if (project.exists() == false) {
			CoreUtility.createProject(project, provider.getLocationPath(), monitor);
			project.open(monitor);
		}
		if (!project.hasNature(JavaCore.NATURE_ID))
			CoreUtility.addNatureToProject(project, JavaCore.NATURE_ID, monitor);
		if (!project.hasNature(PDEPlugin.PLUGIN_NATURE))
			CoreUtility.addNatureToProject(project, PDEPlugin.PLUGIN_NATURE, monitor);

		setDefaultVM(project);
		PDEPlugin.registerPlatformLaunchers(project);
	}

	static void setDefaultVM(IProject project) throws CoreException {
		IVMInstall install = JavaRuntime.getDefaultVMInstall();
		if (install != null) {
			IJavaProject javaProject = JavaCore.create(project);
			JavaRuntime.setVM(javaProject, install);
		}
	}
	
	public static void createBuildProperties(
		IProject project,
		IPluginStructureData data,
		IProgressMonitor monitor) throws CoreException {
		createBuildProperties(project, data.getRuntimeLibraryName(), data.getSourceFolderName());
	}

	public static void createBuildProperties(
		IProject project,
		String library,
		String source)
		throws CoreException {
		String fileName = "build.properties";
		IPath path = project.getFullPath().append(fileName);
		IFile file = project.getWorkspace().getRoot().getFile(path);
		boolean exists = file.exists();
		if (!exists) {
			WorkspaceBuildModel model = new WorkspaceBuildModel(file);
			String libKey = IBuildEntry.JAR_PREFIX + library;
			IBuildEntry entry = model.getFactory().createEntry(libKey);
			if (!source.endsWith("/"))
				source = source + "/";
			entry.addToken(source);
			model.getBuild().add(entry);
			model.save();
		}
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
		if (fragment)
			label.setText(PDEPlugin.getResourceString(KEY_FID));
		else
			label.setText(PDEPlugin.getResourceString(KEY_ID));
		idText = new Text(container, SWT.SINGLE | SWT.BORDER);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		idText.setLayoutData(gd);
		idText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				verifyId(idText.getText());
			}
		});

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

	private void verifyId(String id) {
		String error = verifyIdRules(id);
		setErrorMessage(error);
		setPageComplete(error == null);
	}

	private String verifyIdRules(String id) {
		String problemText = PDEPlugin.getResourceString(KEY_INVALID_ID);
		StringTokenizer stok = new StringTokenizer(id, ".");
		while (stok.hasMoreTokens()) {
			String token = stok.nextToken();
			for (int i = 0; i < token.length(); i++) {
				if (Character.isLetterOrDigit(token.charAt(i)) == false)
					return problemText;
			}
		}
		return null;
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
		final IPluginStructureData data = getStructureData();

		IRunnableWithProgress operation = new WorkspaceModifyOperation() {
			public void execute(IProgressMonitor monitor) {
				try {
					String message = PDEPlugin.getResourceString(KEY_CREATING);
					monitor.beginTask(message, 1);
					createProject(project, provider, monitor);
					createBuildProperties(project, data, monitor);
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
	private IPath getJREPath() {
		return PluginPathUpdater.getJREPath();
	}
	private IPath[] getJRESourceAnnotation() {
		return PluginPathUpdater.getJRESourceAnnotation();
	}
	public IPluginStructureData getStructureData() {
		StructureData data = new StructureData();
		data.pluginId = idText.getText();
		data.buildOutput = buildOutputText.getText();
		data.library = libraryText.getText();
		data.source = sourceText.getText();
		data.jrePath = getJREPath();
		data.jreSourceAnnotation = getJRESourceAnnotation();
		return data;
	}
	private void initialize() {
		String projectName = provider.getProjectName();
		idText.setText(projectName);
		String lastSegment = projectName;
		int loc = projectName.lastIndexOf('.');
		if (loc != -1) {
			lastSegment = projectName.substring(loc + 1);
		}
		buildOutputText.setText("bin");
		libraryText.setText(lastSegment + ".jar");
		sourceText.setText("src");

	}
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			initialize();
			idText.setFocus();
		}
	}
}