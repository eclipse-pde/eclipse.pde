package org.eclipse.pde.internal.wizards.project;

import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.pde.internal.model.*;
import org.eclipse.ui.*;
import java.lang.reflect.*;
import org.eclipse.ui.actions.*;
import org.eclipse.jface.operation.*;
import org.eclipse.core.runtime.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.swt.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.elements.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.core.resources.*;
import org.eclipse.pde.internal.wizards.*;
import java.util.*;
import org.eclipse.pde.internal.*;
import org.eclipse.jdt.core.*;

public class ConvertedProjectsPage extends WizardPage implements ICheckStateListener {
	private Label selectedLabel;
	private Button selectAllButton;
	private Button deselectAllButton;
	private Button updateBuildPathButton;
	private CheckboxTableViewer projectViewer;
	public static final String KEY_TITLE = "ConvertedProjectWizard.title";
	public static final String KEY_SELECT_ALL = "ConvertedProjectWizard.selectAll";
	public static final String KEY_SELECTED = "ConvertedProjectWizard.selected";
	public static final String KEY_DESELECT_ALL = "ConvertedProjectWizard.deselectAll";
	public static final String KEY_UPDATE_BUILD_PATH = "ConvertedProjectWizard.updateBuildPath";
	public static final String KEY_CONVERTING = "ConvertedProjectWizard.converting";
	public static final String KEY_UPDATING = "ConvertedProjectWizard.updating";
	public static final String KEY_DESC = "ConvertedProjectWizard.desc";
	private int selectedCount;
	private Vector candidates;

	public class CheckedProject {
		IProject project;
		boolean checked;
		public CheckedProject(IProject project) {
			this.project = project;
		}
	}

	public class ProjectContentProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			if (candidates == null)
				createConversionCandidates();
			Object [] elements = new Object[candidates.size()];
			candidates.copyInto(elements);
			return elements;
		}
	}

	public class ProjectLabelProvider
		extends LabelProvider
		implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			if (index == 0) {
				CheckedProject checkedProject = (CheckedProject) obj;
				return checkedProject.project.getName();
			}
			return "";
		}
		public String getColumnText(Viewer v, Object obj, int index) {
			return getColumnText(obj, index);
		}
		public Image getColumnImage(Object obj, int index) {
			return null;
		}
		public Image getColumnImage(Viewer v, Object obj, int index) {
			return getColumnImage(obj, index);
		}
	}

public ConvertedProjectsPage() {
	super("convertedProjects");
	setTitle(PDEPlugin.getResourceString(KEY_TITLE));
	setDescription(PDEPlugin.getResourceString(KEY_DESC));
}
public void checkStateChanged(CheckStateChangedEvent event) {
	CheckedProject checkedProject = (CheckedProject)event.getElement();
	checkedProject.checked = event.getChecked();
	updateSelectedCount();
}
public static void convertProject(IProject project, IProgressMonitor monitor)
	throws CoreException {
	CoreUtility.addNatureToProject(project, PDEPlugin.PLUGIN_NATURE, monitor);
	IPath manifestPath = project.getFullPath().append("plugin.xml");
	IFile file = project.getWorkspace().getRoot().getFile(manifestPath);
	if (file.exists()) {
		IWorkbench workbench = PlatformUI.getWorkbench();
		workbench.getEditorRegistry().setDefaultEditor(
			file,
			PDEPlugin.MANIFEST_EDITOR_ID);
	}
	else {
		createManifestFile(file, monitor);
	}
	IPath jarsPath = project.getFullPath().append("plugin.jars");
	IFile jarsFile = project.getWorkspace().getRoot().getFile(jarsPath);
	if (jarsFile.exists()) {
		IWorkbench workbench = PlatformUI.getWorkbench();
		workbench.getEditorRegistry().setDefaultEditor(
			jarsFile,
			PDEPlugin.JARS_EDITOR_ID);
	}
	PDEPlugin.registerPlatformLaunchers(project);
}
private void convertProjects(boolean updateBuildPath, IProgressMonitor monitor)
	throws CoreException {
	int totalCount = updateBuildPath ? selectedCount : (2 * selectedCount);
	monitor.beginTask(PDEPlugin.getResourceString(KEY_CONVERTING), totalCount);
	for (int i = 0; i < candidates.size(); i++) {
		CheckedProject checkedProject = (CheckedProject) candidates.elementAt(i);
		if (checkedProject.checked == false)
			continue;
		IProject project = checkedProject.project;
		convertProject(project, monitor);
		monitor.worked(1);
	}
	WorkspaceModelManager manager =
		PDEPlugin.getDefault().getWorkspaceModelManager();
	manager.reset();
	
	if (updateBuildPath) {
		monitor.subTask(PDEPlugin.getResourceString(KEY_UPDATING));
		for (int i = 0; i < candidates.size(); i++) {
			CheckedProject checkedProject = (CheckedProject) candidates.elementAt(i);
			if (checkedProject.checked == false)
				continue;
			IProject project = checkedProject.project;
			updateBuildPath(project, monitor);
			monitor.worked(1);
		}
	}
	monitor.done();
}
public void createControl(Composite parent) {
	Composite container = new Composite(parent, SWT.NONE);
	GridLayout layout = new GridLayout();
	layout.numColumns = 2;
	layout.marginHeight = 0;
	layout.marginWidth = 5;
	container.setLayout(layout);

	projectViewer = new CheckboxTableViewer(container, SWT.BORDER);
	projectViewer.setContentProvider(new ProjectContentProvider());
	projectViewer.setLabelProvider(new ProjectLabelProvider());
	projectViewer.setSorter(ListUtil.NAME_SORTER);
	GridData gd =
		new GridData(
			GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL);
	gd.heightHint = 200;
	projectViewer.getTable().setLayoutData(gd);

	Composite buttonContainer = new Composite(container, SWT.NONE);
	gd = new GridData(GridData.FILL_VERTICAL);
	buttonContainer.setLayoutData(gd);
	GridLayout buttonLayout = new GridLayout();
	buttonLayout.marginWidth =0;
	buttonLayout.marginHeight = 0;
	buttonContainer.setLayout(buttonLayout);

	selectedLabel = new Label(container, SWT.NONE);
	gd = new GridData();
	gd.horizontalAlignment = GridData.FILL;
	gd.horizontalSpan = 2;
	selectedLabel.setLayoutData(gd);

	selectAllButton = new Button(buttonContainer, SWT.PUSH);
	selectAllButton.setText(PDEPlugin.getResourceString(KEY_SELECT_ALL));
	gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
	selectAllButton.setLayoutData(gd);
	selectAllButton.addSelectionListener(new SelectionAdapter() {
		public void widgetSelected(SelectionEvent e) {
			projectViewer.setAllChecked(true);
			updateSelectedCount(1);
		}
	});

	deselectAllButton = new Button(buttonContainer, SWT.PUSH);
	deselectAllButton.setText(PDEPlugin.getResourceString(KEY_DESELECT_ALL));
	gd = new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
	deselectAllButton.setLayoutData(gd);
	deselectAllButton.addSelectionListener(new SelectionAdapter() {
		public void widgetSelected(SelectionEvent e) {
			projectViewer.setAllChecked(false);
			updateSelectedCount(-1);
		}
	});

	updateBuildPathButton = new Button(container, SWT.CHECK);
	updateBuildPathButton.setText(PDEPlugin.getResourceString(KEY_UPDATE_BUILD_PATH));
	updateBuildPathButton.setSelection(true);
	gd = new GridData(GridData.FILL_HORIZONTAL);
	updateBuildPathButton.setLayoutData(gd);
	
	projectViewer.setInput(PDEPlugin.getWorkspace());
	projectViewer.setAllChecked(true);
	projectViewer.addCheckStateListener(this);
	updateSelectedCount(1);
	setControl(container);
}
private void createConversionCandidates() {
	candidates = new Vector();
	IProject[] projects = PDEPlugin.getWorkspace().getRoot().getProjects();
	for (int i = 0; i < projects.length; i++) {
		IProject project = projects[i];
		try {
			if (project.hasNature(JavaCore.NATURE_ID)
				&& !project.hasNature(PDEPlugin.PLUGIN_NATURE)) {
				CheckedProject checkedProject = new CheckedProject(project);
				checkedProject.checked = true;
				candidates.add(checkedProject);
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}
}
private static String createInitialName(String id) {
	int loc = id.lastIndexOf('.');
	if (loc == -1) return id;
	String name = id.substring(loc+1);
	StringBuffer buf = new StringBuffer(name);
	buf.setCharAt(0, Character.toUpperCase(buf.charAt(0)));
	return buf.toString();
}
private static void createManifestFile(IFile file, IProgressMonitor monitor)
	throws CoreException {
	WorkspacePluginModel model = new WorkspacePluginModel(file);
	model.load();
	IProject project = file.getProject();
	IPlugin plugin = model.getPlugin();
	String id = project.getName();
	plugin.setId(id);
	String name = createInitialName(id);
	plugin.setName(name);
	plugin.setVersion("0.0.1");
	model.save();
}
public boolean finish() {
	final boolean updateBuildPath = updateBuildPathButton.getSelection();
	IRunnableWithProgress operation = new WorkspaceModifyOperation() {
		public void execute(IProgressMonitor monitor) {
			try {
				convertProjects(updateBuildPath, monitor);
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
		PDEPlugin.logException(e);
		return false;
	}
	return true;
}
private static IClasspathEntry[] mergeEntries(
	IClasspathEntry[] e1,
	IClasspathEntry[] e2) {
	int size = e1.length + e2.length;
	IClasspathEntry[] result = new IClasspathEntry[size];
	int cnt = 0;
	for (int i = 0; i < e1.length; i++) {
		result[cnt++] = e1[i];
	}
	for (int i = 0; i < e2.length; i++) {
		result[cnt++] = e2[i];
	}
	return result;
}
private static IClasspathEntry[] mergeEntriesWithoutDuplicates(
	IClasspathEntry[] oldEntries,
	IClasspathEntry[] newEntries) {
	Vector uniqueEntries = new Vector();

	for (int i=0; i<newEntries.length; i++) {
		IClasspathEntry entry = newEntries[i];
		if (PluginPathUpdater.isAlreadyPresent(oldEntries, entry)==false)
		   uniqueEntries.add(entry);
	}
	if (uniqueEntries.size()==0) return oldEntries;
	IClasspathEntry [] uniqueArray = new IClasspathEntry [uniqueEntries.size()];
	uniqueEntries.copyInto(uniqueArray);
	return mergeEntries(oldEntries, uniqueArray);
}
public static void updateBuildPath(IProject project, IProgressMonitor monitor)
	throws CoreException {
	IPath manifestPath = project.getFullPath().append("plugin.xml");
	IFile file = project.getWorkspace().getRoot().getFile(manifestPath);
	if (!file.exists())
		return;
	IJavaProject javaProject = JavaCore.create(project);
	WorkspacePluginModel model = new WorkspacePluginModel(file);
	model.load();
	if (!model.isLoaded())
		return;
	// Update the build path
	Vector required = new Vector();
	IPluginImport[] imports = model.getPlugin().getImports();

	for (int i = 0; i < imports.length; i++) {
		IPluginImport iimport = imports[i];
		IPlugin plugin = PDEPlugin.getDefault().findPlugin(iimport.getId());
		if (plugin != null) {
			required.add(new PluginPathUpdater.CheckedPlugin(plugin, true));
		}
	}
	PluginPathUpdater updater = new PluginPathUpdater(project, required.iterator());
	IClasspathEntry[] oldEntries = javaProject.getRawClasspath();
	IClasspathEntry[] sourceEntries = updater.getSourceClasspathEntries(model);
	IClasspathEntry[] entries = updater.getClasspathEntries();
	IClasspathEntry[] newEntries = mergeEntries(sourceEntries, entries);
	IClasspathEntry[] mergedEntries =
		mergeEntriesWithoutDuplicates(newEntries, oldEntries);
	/*
		mergedEntries =
			mergeEntriesWithoutDuplicates(
				mergedEntries,
				new IClasspathEntry[] { PluginPathUpdater.newStartupJarEntry()});
	*/
	try {
		javaProject.setRawClasspath(mergedEntries, monitor);
	} catch (JavaModelException e) {
		PDEPlugin.logException(e);
	}
}
private void updateSelectedCount() {
	updateSelectedCount(0);
	setPageComplete(selectedCount>0);
}
private void updateSelectedCount(int mode) {
	selectedCount = 0;

	if (mode != -1) {
		for (int i=0; i<candidates.size(); i++) {
			CheckedProject checkedProject = (CheckedProject)candidates.elementAt(i);
			if (mode == 1 || checkedProject.checked)
			selectedCount++;
		}
	}
	selectedLabel.setText(PDEPlugin.getFormattedMessage(KEY_SELECTED, selectedCount+""));
}
}
