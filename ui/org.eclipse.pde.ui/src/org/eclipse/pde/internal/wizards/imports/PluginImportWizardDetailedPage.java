/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.wizards.imports;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;
import org.eclipse.pde.internal.parts.WizardCheckboxTablePart;
import org.eclipse.pde.internal.wizards.*;
import org.eclipse.swt.layout.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.*;
import org.eclipse.pde.model.plugin.*;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.events.*;
import org.eclipse.pde.internal.elements.*;
import org.eclipse.pde.model.plugin.*;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.IDialogConstants;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.pde.internal.parts.WizardCheckboxTablePart;

public class PluginImportWizardDetailedPage extends StatusWizardPage {
	private static final String KEY_TITLE = "ImportWizard.DetailedPage.title";
	private static final String KEY_DESC = "ImportWizard.DetailedPage.desc";
	private PluginImportWizardFirstPage firstPage;
	private IPath dropLocation;
	private CheckboxTableViewer pluginListViewer;
	private TablePart tablePart;
	private static final String SETTINGS_SHOW_IDS = "showIds";
	private static final String KEY_SHOW_NAMES =
		"ImportWizard.DetailedPage.showNames";
	private static final String KEY_PLUGIN_LIST =
		"ImportWizard.DetailedPage.pluginList";
	private static final String KEY_INVERT_SELECTION =
		"ImportWizard.DetailedPage.invertSelection";
	private static final String KEY_EXISTING = "ImportWizard.DetailedPage.existing";
	private static final String KEY_EXISTING_BINARY =
		"ImportWizard.DetailedPage.existingBinary";
	private static final String KEY_EXISTING_EXTERNAL =
		"ImportWizard.DetailedPage.existingExternal";
	private static final String KEY_ADD_REQUIRED =
		"ImportWizard.DetailedPage.addRequired";

	private static final String KEY_LOADING_RUNTIME =
		"ImportWizard.messages.loadingRuntime";
	private static final String KEY_LOADING_FILE =
		"ImportWizard.messages.loadingFile";
	private static final String KEY_NO_PLUGINS = "ImportWizard.messages.noPlugins";
	private static final String KEY_NO_SELECTED =
		"ImportWizard.errors.noPluginSelected";
	private IPluginModelBase[] models;
	private boolean loadFromRegistry;
	private boolean block;

	public class PluginContentProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			return getModels();
		}
	}

	class TablePart extends WizardCheckboxTablePart {
		public TablePart(String mainLabel, String[] buttonLabels) {
			super(mainLabel, buttonLabels);
			setSelectAllIndex(0);
			setDeselectAllIndex(1);
		}
		public void updateCounter(int count) {
			super.updateCounter(count);
			dialogChanged();
		}
		public void buttonSelected(Button button, int index) {
			if (index == 0 || index == 1)
				super.buttonSelected(button, index);
			else
				PluginImportWizardDetailedPage.this.buttonSelected(index);
		}
	}

	public PluginImportWizardDetailedPage(PluginImportWizardFirstPage firstPage) {
		super("PluginImportWizardDetailedPage", false);
		setTitle(PDEPlugin.getResourceString(KEY_TITLE));
		setDescription(PDEPlugin.getResourceString(KEY_DESC));

		this.firstPage = firstPage;
		dropLocation = null;
		updateStatus(createStatus(IStatus.ERROR, ""));

		String[] buttonLabels =
			{
				PDEPlugin.getResourceString(WizardCheckboxTablePart.KEY_SELECT_ALL),
				PDEPlugin.getResourceString(WizardCheckboxTablePart.KEY_DESELECT_ALL),
				PDEPlugin.getResourceString(KEY_INVERT_SELECTION),
				null,
				PDEPlugin.getResourceString(KEY_EXISTING),
				PDEPlugin.getResourceString(KEY_EXISTING_BINARY),
				PDEPlugin.getResourceString(KEY_EXISTING_EXTERNAL),
				null,
				PDEPlugin.getResourceString(KEY_ADD_REQUIRED)};

		tablePart =
			new TablePart(PDEPlugin.getResourceString(KEY_PLUGIN_LIST), buttonLabels);
		PDEPlugin.getDefault().getLabelProvider().connect(this);
	}

	private void initializeFields(IPath dropLocation) {
		boolean oldLoadFromRegistry = loadFromRegistry;

		loadFromRegistry = !firstPage.isOtherLocation();
		
		if (loadFromRegistry) {
			if (!oldLoadFromRegistry) models = null;
			this.dropLocation = null;
			updateStatus(createStatus(IStatus.OK, ""));
		}
		else {
			if (!dropLocation.equals(this.dropLocation)) {
				updateStatus(createStatus(IStatus.OK, ""));
				this.dropLocation = dropLocation;
				models = null;
			}
		}
		pluginListViewer.setInput(PDEPlugin.getDefault());
		tablePart.updateCounter(0);
	}

	public void storeSettings(boolean finishPressed) {
	}

	/*
	 * @see DialogPage#setVisible(boolean)
	 */
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			initializeFields(firstPage.getDropLocation());
		}
	}

	/*
	 * @see IDialogPage#createControl(Composite)
	 */
	public void createControl(Composite parent) {
		initializeDialogUnits(parent);

		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 0;
		layout.marginWidth = 5;
		container.setLayout(layout);

		tablePart.createControl(container);
		pluginListViewer = tablePart.getTableViewer();
		pluginListViewer.setContentProvider(new PluginContentProvider());
		pluginListViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		GridData gd = (GridData) tablePart.getControl().getLayoutData();
		gd.heightHint = 300;
		gd.widthHint = 300;
		setControl(container);
	}

	public void dispose() {
		super.dispose();
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
	}

	public IPluginModelBase[] getModels() {
		if (models != null)
			return models;
		if (loadFromRegistry) {
			final ExternalModelManager registry =
				PDEPlugin.getDefault().getExternalModelManager();
			IRunnableWithProgress op = new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) {
					monitor.beginTask(
						PDEPlugin.getResourceString(KEY_LOADING_RUNTIME),
						IProgressMonitor.UNKNOWN);
					int size = registry.getPluginCount()+registry.getFragmentCount();
					models = new IPluginModelBase[size];
					for (int i=0; i<registry.getPluginCount(); i++) {
						models[i] = registry.getPlugin(i).getModel();
					}
					for (int i=registry.getPluginCount(); i<size; i++) {
						models[i] = registry.getFragment(i).getModel();
					}
					monitor.done();
				}
			};
			try {
				getContainer().run(true, false, op);
			} catch (Throwable e) {
				PDEPlugin.logException(e);
			}
		} else {
			final Vector result = new Vector();
			final Vector fresult = new Vector();
			if (dropLocation != null) {
				IRunnableWithProgress op = new IRunnableWithProgress() {
					public void run(IProgressMonitor monitor) {
						monitor.beginTask(
							PDEPlugin.getResourceString(KEY_LOADING_FILE),
							IProgressMonitor.UNKNOWN);
						String[] paths = createPaths(dropLocation);

						MultiStatus errors =
							ExternalModelManager.processPluginDirectories(result, fresult, paths, false, monitor);
						if (errors != null && errors.getChildren().length > 0) {
							PDEPlugin.log(errors);
						}
						monitor.done();
					}
				};
				try {
					getContainer().run(true, false, op);
				} catch (Throwable e) {
					PDEPlugin.logException(e);
				}
			}
			int size = result.size()+fresult.size();
			models = new IPluginModelBase[size];
			for (int i=0; i<result.size(); i++) {
				models[i] = (IPluginModelBase)result.get(i);
			}
			int offset = result.size();
			for (int i=0; i<fresult.size(); i++) {
				models[offset+i] = (IPluginModelBase)fresult.get(i);
			}
		}
		return models;
	}

	private String[] createPaths(IPath dropLocation) {
		File dropDir = dropLocation.toFile();
		Vector result = new Vector();

		File pluginsDir = new File(dropDir, "plugins");
		if (pluginsDir.exists()) {
			result.add(pluginsDir.getAbsolutePath());
		}
		File fragmentDir = new File(dropDir, "fragments");
		if (fragmentDir.exists()) {
			result.add(fragmentDir.getAbsolutePath());
		}
		result.add(dropDir.getAbsolutePath());
		return (String[]) result.toArray(new String[result.size()]);
	}

	public IPluginModelBase[] getSelectedModels() {
		Object[] selected = tablePart.getSelection();
		IPluginModelBase[] result = new IPluginModelBase[selected.length];
		System.arraycopy(selected, 0, result, 0, selected.length);
		return result;
	}

	private IStatus validatePlugins() {
		IPluginModelBase[] allModels = getModels();
		if (allModels == null || allModels.length == 0) {
			return createStatus(IStatus.ERROR, PDEPlugin.getResourceString(KEY_NO_PLUGINS));
		}
		if (tablePart.getSelectionCount() == 0) {
			return createStatus(
				IStatus.ERROR,
				PDEPlugin.getResourceString(KEY_NO_SELECTED));
		}
		return createStatus(IStatus.OK, "");
	}

	private void dialogChanged() {
		IStatus genStatus = validatePlugins();
		updateStatus(genStatus);
	}

	private void buttonSelected(int index) {
		if (index == 2) {
			invertSelection();
			return;
		}
		ArrayList checked = null;
		switch (index) {
			case 4 : // existing
				checked = selectExistingProjects();
				break;
			case 5 : // existing binary
				checked = selectLibraryProjects();
				break;
			case 6 : // existing external
				checked = selectExternalProjects();
				break;
			case 8 : // select dependent
				checked = selectDependentPlugins();
				break;
			default :
				return;
		}
		tablePart.setSelection(checked.toArray());
	}

	private void invertSelection() {
		IPluginModelBase[] models = getModels();

		Vector selected = new Vector();
		for (int i = 0; i < models.length; i++) {
			Object model = models[i];
			if (!pluginListViewer.getChecked(model))
				selected.add(model);
		}
		tablePart.setSelection(selected.toArray());
	}

	private ArrayList selectExistingProjects() {
		IWorkspaceRoot root = PDEPlugin.getWorkspace().getRoot();
		ArrayList selected = new ArrayList();
		IPluginModelBase[] models = getModels();
		for (int i = 0; i < models.length; i++) {
			IPluginModelBase curr = (IPluginModelBase) models[i];
			String id = curr.getPluginBase().getId();
			IProject proj = (IProject) root.findMember(id);
			if (proj != null) {
				selected.add(curr);
			}
		}
		return selected;
	}

	private ArrayList selectLibraryProjects() {
		IWorkspaceRoot root = PDEPlugin.getWorkspace().getRoot();
		ArrayList selected = new ArrayList();
		IPluginModelBase[] models = getModels();
		for (int i = 0; i < models.length; i++) {
			IPluginModelBase curr = (IPluginModelBase) models[i];
			String id = curr.getPluginBase().getId();
			IProject proj = (IProject) root.findMember(id);
			if (proj != null && !hasSourceFolder(proj)) {
				selected.add(curr);
			}
		}
		return selected;
	}

	private ArrayList selectExternalProjects() {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		ArrayList selected = new ArrayList();
		IPluginModelBase[] models = getModels();
		for (int i = 0; i < models.length; i++) {
			IPluginModelBase curr = (IPluginModelBase) models[i];
			String id = curr.getPluginBase().getId();
			IProject proj = (IProject) root.findMember(id);
			if (proj != null && !root.getLocation().isPrefixOf(proj.getLocation())) {
				selected.add(curr);
			}
		}
		return selected;
	}

	private boolean hasSourceFolder(IProject project) {
		try {
			if (project.hasNature(JavaCore.NATURE_ID)) {
				IClasspathEntry[] entries = JavaCore.create(project).getRawClasspath();
				for (int i = 0; i < entries.length; i++) {
					if (entries[i].getEntryKind() == IClasspathEntry.CPE_SOURCE) {
						return true;
					}
				}
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
		return false;
	}
	private ArrayList selectDependentPlugins() {
		HashSet checked = new HashSet();
		Object[] selected = tablePart.getSelection();
		if (selected.length > 0) {
			if (selected.length > 1
				|| !((IPluginModelBase) selected[0]).getPluginBase().getId().equals(
					"org.eclipse.core.boot")) {
				addImplicitDependencies(checked);
			}
			for (int i = 0; i < selected.length; i++) {
				addPluginAndDependent((IPluginModelBase) selected[i], checked);
			}
		}

		ArrayList result = new ArrayList(checked);
		/*
				if (findPlugin("org.eclipse.sdk") == null
					findPlugin("org.eclipse.ui") != null) {
					PluginModel sdkPlugin = PluginUtil.findPlugin("org.eclipse.sdk", plugins);
					if (sdkPlugin != null) {
						String title = "Plugin Selection";
						String message =
							"'org.eclipse.ui' implicitly requires 'org.eclipse.sdk'.\nOK to add 'org.eclipse.sdk' (recommended)?";
						if (MessageDialog.openQuestion(getShell(), title, message)) {
							result.add(sdkPlugin);
						}
					}
				}
		*/
		return result;
	}

	private void addImplicitDependencies(HashSet checked) {
		IPluginModelBase implicit = findModel("org.eclipse.core.boot");
		if (implicit != null) {
			checked.add(implicit);
		}
		implicit = findModel("org.eclipse.core.runtime");
		if (implicit != null) {
			checked.add(implicit);
		}
	}

	private IPluginModelBase findModel(String id) {
		IPluginModelBase[] models = getModels();
		for (int i = 0; i < models.length; i++) {
			IPluginModelBase model = (IPluginModelBase) models[i];
			if (model.getPluginBase().getId().equals(id))
				return model;
		}
		return null;
	}

	private void addPluginAndDependent(IPluginModelBase model, HashSet checked) {
		if (checked.contains(model)) {
			return;
		}
		checked.add(model);
		if (model instanceof IPluginModel) {
			IPlugin plugin = ((IPluginModel) model).getPlugin();
			IPluginImport[] required = plugin.getImports();
			if (required.length > 0) {
				for (int k = 0; k < required.length; k++) {
					String id = required[k].getId();
					IPluginModelBase found = findModel(id);
					if (found != null) {
						addPluginAndDependent(found, checked);
					}
				}
			}
		}
		if (model instanceof IFragmentModel) {
			IFragment fragment = ((IFragmentModel) model).getFragment();
			String id = fragment.getPluginId();
			IPluginModelBase found = findModel(id);
			if (found != null) {
				addPluginAndDependent(found, checked);
			}
		}
	}
}