/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.ui.wizards.imports;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.parts.WizardCheckboxTablePart;
import org.eclipse.pde.internal.ui.wizards.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.update.ui.forms.internal.FormWidgetFactory;

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
	private static final String KEY_EXISTING =
		"ImportWizard.DetailedPage.existing";
	private static final String KEY_EXISTING_BINARY =
		"ImportWizard.DetailedPage.existingBinary";
	private static final String KEY_EXISTING_EXTERNAL =
		"ImportWizard.DetailedPage.existingExternal";
	private static final String KEY_ADD_REQUIRED =
		"ImportWizard.DetailedPage.addRequired";

	private static final String KEY_LOADING_RUNTIME =
		"ImportWizard.messages.loadingRuntime";
	private static final String KEY_UPDATING = "ImportWizard.messages.updating";
	private static final String KEY_LOADING_FILE =
		"ImportWizard.messages.loadingFile";
	private static final String KEY_NO_PLUGINS =
		"ImportWizard.messages.noPlugins";
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
		protected StructuredViewer createStructuredViewer(
			Composite parent,
			int style,
			FormWidgetFactory factory) {
			StructuredViewer viewer =
				super.createStructuredViewer(parent, style, factory);
			viewer.setSorter(ListUtil.PLUGIN_SORTER);
			return viewer;
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
				PDEPlugin.getResourceString(
					WizardCheckboxTablePart.KEY_SELECT_ALL),
				PDEPlugin.getResourceString(
					WizardCheckboxTablePart.KEY_DESELECT_ALL),
				PDEPlugin.getResourceString(KEY_INVERT_SELECTION),
				null,
				PDEPlugin.getResourceString(KEY_EXISTING),
				PDEPlugin.getResourceString(KEY_EXISTING_BINARY),
			//PDEPlugin.getResourceString(KEY_EXISTING_EXTERNAL),
			null, PDEPlugin.getResourceString(KEY_ADD_REQUIRED)};

		tablePart =
			new TablePart(
				PDEPlugin.getResourceString(KEY_PLUGIN_LIST),
				buttonLabels);
		PDEPlugin.getDefault().getLabelProvider().connect(this);
	}

	private void initializeFields(IPath dropLocation) {
		boolean oldLoadFromRegistry = loadFromRegistry;

		loadFromRegistry = !firstPage.isOtherLocation();

		if (loadFromRegistry) {
			if (!oldLoadFromRegistry)
				models = null;
			this.dropLocation = null;
			updateStatus(createStatus(IStatus.OK, ""));
		} else {
			if (!dropLocation.equals(this.dropLocation)) {
				updateStatus(createStatus(IStatus.OK, ""));
				this.dropLocation = dropLocation;
				models = null;
			}
		}
		if (models == null) {
			getModels(); // force loading
			IRunnableWithProgress op = new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) {
					monitor.beginTask(
						PDEPlugin.getResourceString(KEY_UPDATING),
						IProgressMonitor.UNKNOWN);
					pluginListViewer
						.getControl()
						.getDisplay()
						.asyncExec(new Runnable() {
						public void run() {
							pluginListViewer.setInput(PDEPlugin.getDefault());
						}
					});

					monitor.done();
				}
			};
			try {
				getContainer().run(true, false, op);
			} catch (InterruptedException e) {
			} catch (InvocationTargetException e) {
				PDEPlugin.logException(e);
			}
			tablePart.updateCounter(0);
		}
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
		pluginListViewer.setLabelProvider(
			PDEPlugin.getDefault().getLabelProvider());
		GridData gd = (GridData) tablePart.getControl().getLayoutData();
		gd.heightHint = 300;
		gd.widthHint = 300;
		setControl(container);
		WorkbenchHelp.setHelp(container, IHelpContextIds.PLUGIN_IMPORT_SECOND_PAGE);
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
				PDECore.getDefault().getExternalModelManager();
			IRunnableWithProgress op = new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) {
					monitor.beginTask(
						PDEPlugin.getResourceString(KEY_LOADING_RUNTIME),
						IProgressMonitor.UNKNOWN);
					models = registry.getAllModels();
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

						MultiStatus errors =
							RegistryLoader.loadFromDirectories(
								result,
								fresult,
								createPath(dropLocation),
								false,
								false,
								monitor);
						if (errors != null
							&& errors.getChildren().length > 0) {
							PDEPlugin.log(errors);
						}
						models = new IPluginModelBase[result.size() + fresult.size()];
						System.arraycopy(
							result.toArray(new IPluginModel[result.size()]),
							0,
							models,
							0,
							result.size());
						System.arraycopy(
							fresult.toArray(new IFragmentModel[fresult.size()]),
							0,
							models,
							result.size(),
							fresult.size());
						monitor.done();
					}
				};
				try {
					getContainer().run(true, false, op);
				} catch (Throwable e) {
					PDEPlugin.logException(e);
				}
			}
		}
		return models;
	}

	private String[] createPath(IPath dropLocation) {
		File pluginsDir = new File(dropLocation.toFile(), "plugins");
		if (pluginsDir.exists()) 
			return new String[] {pluginsDir.getAbsolutePath()};
		return new String[0];
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
			return createStatus(
				IStatus.ERROR,
				PDEPlugin.getResourceString(KEY_NO_PLUGINS));
		}
		if (tablePart.getSelectionCount() == 0) {
			return createStatus(
				IStatus.INFO,
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
				/*
						case 6 : // existing external
							checked = selectExternalProjects();
							break;
						case 8 : // select dependent
				*/
			case 7 :
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
			if (proj != null && proj.isOpen()) {
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
			try {
				if (proj != null && proj.isOpen()) {
					String property =
						proj.getPersistentProperty(
							PDECore.EXTERNAL_PROJECT_PROPERTY);
					if (property != null && (property.equals(PDECore.BINARY_PROJECT_VALUE) || property.equals(PDECore.EXTERNAL_PROJECT_VALUE))) {
						selected.add(curr);
					}
				}
			} catch (CoreException e) {
				PDEPlugin.logException(e);
			}
		}
		return selected;
	}

	/*
	
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
	
	*/

	/*private boolean hasSourceFolder(IProject project) throws CoreException {
		IClasspathEntry[] entries = JavaCore.create(project).getRawClasspath();
		for (int i = 0; i < entries.length; i++) {
			if (entries[i].getEntryKind() == IClasspathEntry.CPE_SOURCE) {
				return true;
			}
		}
		return false;
	}*/

	private ArrayList selectDependentPlugins() {
		HashSet checked = new HashSet();
		Object[] selected = tablePart.getSelection();
		if (selected.length == 1) {
			IPluginModelBase model = (IPluginModelBase)selected[0];
			if (model.getPluginBase().getId().equals("org.eclipse.core.boot")) {
				checked.add(model);
				return new ArrayList(checked);
			}
		}
		
		if (selected.length > 0) {
			boolean noImplicitPlugins = true;
			for (int i = 0; i < selected.length; i++) {
				IPluginModelBase model = (IPluginModelBase) selected[i];
				if (noImplicitPlugins)
					noImplicitPlugins =
						model.getPluginBase().getLibraries().length == 0
							&& model.getPluginBase().getImports().length == 0;
				addPluginAndDependent(model, checked);
			}

			if (!noImplicitPlugins)
				addImplicitDependencies(checked);

		}

		return new ArrayList(checked);
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
	}

	private void addImplicitDependencies(HashSet checked) {
		IPluginModelBase implicit = findModel("org.eclipse.core.boot");
		if (implicit != null) {
			checked.add(implicit);
		}
		implicit = findModel("org.eclipse.core.runtime");
		if (implicit != null) {
			addPluginAndDependent(implicit,checked);
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

	private IFragmentModel[] findFragments(IPlugin plugin) {
		String pluginId = plugin.getId();
		ArrayList result = new ArrayList();
		for (int i = 0; i < models.length; i++) {
			IPluginModelBase model = (IPluginModelBase) models[i];
			if (model instanceof IFragmentModel) {
				IFragment fragment = ((IFragmentModel) model).getFragment();
				String refId = fragment.getPluginId();
				if (pluginId.equalsIgnoreCase(refId)) {
					result.add(model);
				}
			}
		}
		return (IFragmentModel[]) result.toArray(
			new IFragmentModel[result.size()]);
	}

	private void addPluginAndDependent(
		IPluginModelBase model,
		HashSet checked) {
		addPluginAndDependent(model, checked, true);
	}

	private void addPluginAndDependent(
		IPluginModelBase model,
		HashSet checked,
		boolean addFragmentPlugin) {
		if (!checked.add(model))
			return;
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
			IFragmentModel[] fragments = findFragments(plugin);
			for (int i = 0; i < fragments.length; i++) {
				addPluginAndDependent(fragments[i], checked, false);
			}
		}
		if (addFragmentPlugin && model instanceof IFragmentModel) {
			IFragment fragment = ((IFragmentModel) model).getFragment();
			String id = fragment.getPluginId();
			IPluginModelBase found = findModel(id);
			if (found != null) {
				addPluginAndDependent(found, checked);
			}
		}
	}
}