package org.eclipse.pde.internal.ui.wizards.project;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.plugin.*;
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
import org.eclipse.pde.internal.ui.elements.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.core.resources.*;
import org.eclipse.pde.internal.ui.util.*;
import org.eclipse.pde.internal.ui.wizards.*;
import java.util.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.jdt.core.*;
import org.eclipse.pde.internal.ui.preferences.*;
import org.eclipse.pde.internal.ui.parts.WizardCheckboxTablePart;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.PDE;

public class ConvertedProjectsPage extends WizardPage {
	private Button updateBuildPathButton;
	private CheckboxTableViewer projectViewer;
	public static final String KEY_TITLE = "ConvertedProjectWizard.title";
	public static final String KEY_UPDATE_BUILD_PATH =
		"ConvertedProjectWizard.updateBuildPath";
	public static final String KEY_CONVERTING = "ConvertedProjectWizard.converting";
	public static final String KEY_UPDATING = "ConvertedProjectWizard.updating";
	public static final String KEY_DESC = "ConvertedProjectWizard.desc";
	public static final String KEY_PROJECT_LIST =
		"ConvertedProjectWizard.projectList";
	private Vector initialSelection;
	private TablePart tablePart;

	public class ProjectContentProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			IWorkspace workspace = PDEPlugin.getWorkspace();
			return workspace.getRoot().getProjects();
		}
	}

	public class ProjectLabelProvider
		extends LabelProvider
		implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			if (index == 0) {
				return ((IProject) obj).getName();
			}
			return "";
		}
		public Image getColumnImage(Object obj, int index) {
			return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_PROJECT);
		}
	}

	class TablePart extends WizardCheckboxTablePart {
		public TablePart(String mainLabel) {
			super(mainLabel);
		}
		public void updateCounter(int count) {
			super.updateCounter(count);
			setPageComplete(count > 0);
		}
	}

	public ConvertedProjectsPage(Vector initialSelection) {
		super("convertedProjects");
		setTitle(PDEPlugin.getResourceString(KEY_TITLE));
		setDescription(PDEPlugin.getResourceString(KEY_DESC));
		this.initialSelection = initialSelection;
		tablePart = new TablePart(PDEPlugin.getResourceString(KEY_PROJECT_LIST));
	}

	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 0;
		layout.marginWidth = 5;
		container.setLayout(layout);

		tablePart.createControl(container);

		projectViewer = tablePart.getTableViewer();
		projectViewer.setContentProvider(new ProjectContentProvider());
		projectViewer.setLabelProvider(new ProjectLabelProvider());
		projectViewer.addFilter(new ViewerFilter () {
			public boolean select(Viewer viewer, Object parent, Object object) {
				return isCandidate((IProject)object);
			}
		});
		
		GridData gd = (GridData) tablePart.getControl().getLayoutData();
		gd.heightHint = 200;

		updateBuildPathButton = new Button(container, SWT.CHECK);
		updateBuildPathButton.setText(
			PDEPlugin.getResourceString(KEY_UPDATE_BUILD_PATH));
		boolean value = BuildpathPreferencePage.isConversionUpdate();
		updateBuildPathButton.setSelection(value);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		updateBuildPathButton.setLayoutData(gd);

		projectViewer.setInput(PDEPlugin.getWorkspace());
		if (initialSelection != null)
			tablePart.setSelection(initialSelection.toArray());
		else 
			//defect 17757
			tablePart.updateCounter(0);
		setControl(container);
	}

	private boolean isCandidate(IProject project) {
		if (project.isOpen()) {
			try {
				if (project.hasNature(JavaCore.NATURE_ID)
					&& !PDE.hasPluginNature(project))
					return true;
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
		}
		return false;
	}

	private static String createInitialName(String id) {
		int loc = id.lastIndexOf('.');
		if (loc == -1)
			return id;
		String name = id.substring(loc + 1);
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
		final Object [] selected = tablePart.getSelection();
		IRunnableWithProgress operation = new WorkspaceModifyOperation() {
			public void execute(IProgressMonitor monitor) {
				try {
					convertProjects(selected, updateBuildPath, monitor);
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

		for (int i = 0; i < newEntries.length; i++) {
			IClasspathEntry entry = newEntries[i];
			if (PluginPathUpdater.isAlreadyPresent(oldEntries, entry) == false)
				uniqueEntries.add(entry);
		}
		if (uniqueEntries.size() == 0)
			return oldEntries;
		IClasspathEntry[] uniqueArray = new IClasspathEntry[uniqueEntries.size()];
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
			IPlugin plugin = PDECore.getDefault().findPlugin(iimport.getId());
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

	public static void convertProject(IProject project, IProgressMonitor monitor)
		throws CoreException {
		CoreUtility.addNatureToProject(project, PDE.PLUGIN_NATURE, monitor);
		IPath manifestPath = project.getFullPath().append("plugin.xml");
		IFile file = project.getWorkspace().getRoot().getFile(manifestPath);
		if (file.exists()) {
			IWorkbench workbench = PlatformUI.getWorkbench();
			workbench.getEditorRegistry().setDefaultEditor(
				file,
				PDEPlugin.MANIFEST_EDITOR_ID);
		} else {
			createManifestFile(file, monitor);
		}
		IPath buildPath = project.getFullPath().append("build.properties");
		IFile buildFile = project.getWorkspace().getRoot().getFile(buildPath);
		if (buildFile.exists()) {
			IWorkbench workbench = PlatformUI.getWorkbench();
			workbench.getEditorRegistry().setDefaultEditor(
				buildFile,
				PDEPlugin.BUILD_EDITOR_ID);
		}
	}
	private void convertProjects(Object[] selected, boolean updateBuildPath, IProgressMonitor monitor)
		throws CoreException {
		int totalCount =
			updateBuildPath
				? selected.length
				: (2 * selected.length);
		monitor.beginTask(PDEPlugin.getResourceString(KEY_CONVERTING), totalCount);
		for (int i = 0; i < selected.length; i++) {
			IProject project = (IProject)selected[i];
			convertProject(project, monitor);
			monitor.worked(1);
		}
		WorkspaceModelManager manager =
			PDECore.getDefault().getWorkspaceModelManager();
		manager.reset();

		if (updateBuildPath) {
			monitor.subTask(PDEPlugin.getResourceString(KEY_UPDATING));
			for (int i = 0; i < selected.length; i++) {
				IProject project = (IProject)selected[i];
				updateBuildPath(project, monitor);
				monitor.worked(1);
			}
		}
		monitor.done();
	}
}