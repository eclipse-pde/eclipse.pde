package org.eclipse.pde.internal.ui.wizards.project;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.core.build.*;
import org.eclipse.pde.internal.core.build.*;
import org.eclipse.pde.internal.core.plugin.WorkspaceFragmentModel;
import org.eclipse.pde.internal.core.plugin.WorkspacePluginModel;
import org.eclipse.pde.internal.core.plugin.WorkspacePluginModelBase;
import org.eclipse.ui.*;
import org.eclipse.pde.core.plugin.*;

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
import org.eclipse.pde.ui.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.swt.*;
import org.eclipse.pde.internal.ui.util.*;
import org.eclipse.pde.internal.ui.wizards.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.jdt.launching.*;
import org.eclipse.swt.events.*;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.PDE;

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
	
	private static final String KEY_JAVA_PROJECT = "ProjectStructurePage.javaProject";
	private static final String KEY_SIMPLE_PROJECT = "ProjectStructurePage.simpleProject";

	private IProjectProvider provider;
	private boolean fragment;
	private Text idText;
	private Text buildOutputText;
	private Text sourceText;
	private Text libraryText;
	private Composite bottomContainer;
	private Button simpleChoice;

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

	public static void createProject(IProject project, IProjectProvider provider, IPluginStructureData data, IProgressMonitor monitor)
		throws CoreException {
		if (project.exists() == false) {
			CoreUtility.createProject(project, provider.getLocationPath(), monitor);
			project.open(monitor);
		}
		if (!project.hasNature(JavaCore.NATURE_ID) && data.getRuntimeLibraryName() != null)
			CoreUtility.addNatureToProject(project, JavaCore.NATURE_ID, monitor);
		if (!project.hasNature(PDE.PLUGIN_NATURE))
			CoreUtility.addNatureToProject(project, PDE.PLUGIN_NATURE, monitor);

		setDefaultVM(project);
	}

	static void setDefaultVM(IProject project) throws CoreException {
		IVMInstall install = JavaRuntime.getDefaultVMInstall();
		if (install != null) {
			IJavaProject javaProject = JavaCore.create(project);
			// method is deprecated and does nothing.
			//JavaRuntime.setVM(javaProject, install);
		}
	}
	
	public static void createBuildProperties(
		IProject project,
		IPluginStructureData data,
		IProgressMonitor monitor)
		throws CoreException {
		createBuildProperties(
			project,
			data.getRuntimeLibraryName(),
			data.getSourceFolderName());
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
			if (library != null && source != null) {
				String libKey = IBuildEntry.JAR_PREFIX + library;
				IBuildEntry entry = model.getFactory().createEntry(libKey);
				if (!source.endsWith("/"))
					source = source + "/";
				entry.addToken(source);
				model.getBuild().add(entry);
			}
			model.save();
		}
		IWorkbench workbench = PlatformUI.getWorkbench();
		workbench.getEditorRegistry().setDefaultEditor(
			file,
			PDEPlugin.BUILD_EDITOR_ID);
	}
	
	private Composite addTopSection(Composite container) {
		Composite topContainer = new Composite(container, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		topContainer.setLayout(layout);
		topContainer.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Label label = new Label(topContainer, SWT.NULL);
		if (fragment)
			label.setText(PDEPlugin.getResourceString(KEY_FID));
		else
			label.setText(PDEPlugin.getResourceString(KEY_ID));
		idText = new Text(topContainer, SWT.SINGLE | SWT.BORDER);
		idText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		idText.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				verifyId(idText.getText());
			}
		});
		return topContainer;
	} 
	
	private Composite addMiddleSection(Composite container) {
		Composite middleContainer = new Composite(container, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		middleContainer.setLayout(layout);
		middleContainer.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		
		simpleChoice = new Button(middleContainer, SWT.RADIO);
		simpleChoice.setText(PDEPlugin.getResourceString(KEY_SIMPLE_PROJECT));
		simpleChoice.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				bottomContainer.setVisible(!((Button)e.widget).getSelection());
				getContainer().updateButtons();
			}
		});
		
		Button button = new Button(middleContainer, SWT.RADIO);
		button.setText(PDEPlugin.getResourceString(KEY_JAVA_PROJECT));
		button.setSelection(true);
		
		return middleContainer;
	}
	
	private Composite addBottomSection(Composite container) {
		bottomContainer = new Composite(container, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.verticalSpacing = 9;
		bottomContainer.setLayout(layout);		
		bottomContainer.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Label buildOutputLabel = new Label(bottomContainer, SWT.NULL);
		buildOutputLabel.setText(PDEPlugin.getResourceString(KEY_OUTPUT));
		buildOutputText = new Text(bottomContainer, SWT.SINGLE | SWT.BORDER);
		buildOutputText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		buildOutputText.setEditable(false);

		Label libraryLabel = new Label(bottomContainer, SWT.NULL);
		libraryLabel.setText(PDEPlugin.getResourceString(fragment ? KEY_FLIBRARY : KEY_LIBRARY));
		libraryText = new Text(bottomContainer, SWT.SINGLE | SWT.BORDER);
		libraryText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Label sourceLabel = new Label(bottomContainer, SWT.NULL);
		sourceLabel.setText(PDEPlugin.getResourceString(KEY_SOURCE));
		sourceText = new Text(bottomContainer, SWT.SINGLE | SWT.BORDER);
		sourceText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		return bottomContainer;
	}
	
	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.verticalSpacing = 9;
		container.setLayout(layout);
		
		addTopSection(container);
		addMiddleSection(container);
		addBottomSection(container);
	
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

	private void createBlankManifest(
		IProject project,
		IProgressMonitor monitor)
		throws CoreException {
		IPath path = project.getFullPath().append(fragment ? "fragment.xml" : "plugin.xml");
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
			model.save();
		}
	}
	
	private String createSourceFolderName(String lastSegment) {
		return PDEPlugin.getFormattedMessage(
			fragment ? KEY_FSOURCE_NAME : KEY_SOURCE_NAME,
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
					createProject(project, provider, data, monitor);
					createBuildProperties(project, data, monitor);
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
	
	private IPath getJREPath() {
		return PluginPathUpdater.getJREPath();
	}
	
	private IPath[] getJRESourceAnnotation() {
		return PluginPathUpdater.getJRESourceAnnotation();
	}
	
	public IPluginStructureData getStructureData() {
		StructureData data = new StructureData();
		data.pluginId = idText.getText();
		data.buildOutput = (simpleChoice.getSelection()) ? null : buildOutputText.getText();
		data.library = (simpleChoice.getSelection()) ? null : libraryText.getText();
		data.source = (simpleChoice.getSelection()) ? null : sourceText.getText();
		data.jrePath = (simpleChoice.getSelection()) ? null : getJREPath();
		data.jreSourceAnnotation = (simpleChoice.getSelection()) ? null : getJRESourceAnnotation();
		return data;
	}
	
	private void initialize() {
		String projectName = setInitialId(provider.getProjectName());
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
	
	private String setInitialId(String projectName) {
		StringBuffer buffer = new StringBuffer();
		StringTokenizer stok = new StringTokenizer(projectName,".");
		while (stok.hasMoreTokens()) {
			String token = stok.nextToken();
			for (int i=0; i<token.length(); i++) {
				if (Character.isLetterOrDigit(token.charAt(i)))
				   buffer.append(token.charAt(i));
			}
			if (stok.hasMoreTokens() && buffer.charAt(buffer.length()-1) != '.')
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

	public IWizardPage getNextPage() {
		return (!fragment && simpleChoice.getSelection() ? null : super.getNextPage());
	}

}