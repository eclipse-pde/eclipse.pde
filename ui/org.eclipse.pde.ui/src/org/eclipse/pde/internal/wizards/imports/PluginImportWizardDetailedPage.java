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
import org.eclipse.pde.internal.wizards.*;
import org.eclipse.swt.layout.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.*;
import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.events.*;
import org.eclipse.pde.internal.elements.*;
import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.IDialogConstants;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ViewerSorter;

public class PluginImportWizardDetailedPage extends StatusWizardPage {
	private static final String KEY_TITLE = "ImportWizard.DetailedPage.title";
	private static final String KEY_DESC = "ImportWizard.DetailedPage.desc";
	private PluginImportWizardFirstPage firstPage;
	private IPath dropLocation;
	private Button showNamesCheck;
	private CheckboxTableViewer pluginListViewer;
	private Button deselectAllButton;
	private Button selectAllButton;
	private Button existingButton;
	private Button existingBinaryButton;
	private Button existingExternalButton;
	private Button addRequiredButton;
	private Label counterLabel;
	private static final String SETTINGS_SHOW_IDS = "showIds";
	private static final String KEY_SHOW_NAMES =
		"ImportWizard.DetailedPage.showNames";
	private static final String KEY_PLUGIN_LIST =
		"ImportWizard.DetailedPage.pluginList";
	private static final String KEY_SELECT_ALL =
		"ImportWizard.DetailedPage.selectAll";
	private static final String KEY_DESELECT_ALL =
		"ImportWizard.DetailedPage.deselectAll";
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
	private static final String KEY_SELECTED = "ImportWizard.DetailedPage.selected";

	private static final String KEY_NO_PLUGINS = "ImportWizard.messages.noPlugins";
	private static final String KEY_NO_SELECTED =
		"ImportWizard.errors.noPluginSelected";
	private int counter;
	private Image externalPluginImage;
	private Image externalFragmentImage;
	private Vector selected;
	private IPluginModelBase[] models;
	private boolean loadFromRegistry;
	private boolean block;
	private IPluginModelBase launchingModel;

	public class PluginContentProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			return getModels();
		}
	}

	public class PluginLabelProvider
		extends LabelProvider
		implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			if (index == 0) {
				IPluginModelBase model = (IPluginModelBase) obj;
				IPluginBase plugin = model.getPluginBase();
				String name;
				if (showNamesCheck.getSelection())
					name = plugin.getTranslatedName();
				else
					name = plugin.getId();

				String version = plugin.getVersion();
				return name + " (" + version + ")";
			}
			return "";
		}
		public String getText(Object obj) {
			return getColumnText(obj, 0);
		}
		public Image getColumnImage(Object obj, int index) {
			if (index == 0) {
				if (obj instanceof IFragmentModel)
					return externalFragmentImage;
				else
					return externalPluginImage;
			}
			return null;
		}
	}

	public PluginImportWizardDetailedPage(
		PluginImportWizardFirstPage firstPage,
		IPluginModelBase launchingModel) {
		super("PluginImportWizardDetailedPage", false);
		setTitle(PDEPlugin.getResourceString(KEY_TITLE));
		setDescription(PDEPlugin.getResourceString(KEY_DESC));

		externalPluginImage = PDEPluginImages.DESC_PLUGIN_OBJ.createImage();
		externalFragmentImage = PDEPluginImages.DESC_FRAGMENT_OBJ.createImage();
		this.firstPage = firstPage;
		this.launchingModel = launchingModel;
		dropLocation = null;
		selected = new Vector();
		updateStatus(createStatus(IStatus.ERROR, ""));
	}

	private void initializeFields(IPath dropLocation) {
		boolean showIds = false;
		IDialogSettings settings = getDialogSettings();

		if (settings != null) {
			showIds = settings.getBoolean(SETTINGS_SHOW_IDS);
		}
		block = true;
		showNamesCheck.setSelection(!showIds);
		block = false;
		loadFromRegistry = !firstPage.isOtherLocation();
		if (!dropLocation.equals(this.dropLocation)) {
			updateStatus(createStatus(IStatus.OK, ""));
			this.dropLocation = dropLocation;
			models = null;
			selected.clear();
		}
		pluginListViewer.setInput(PDEPlugin.getDefault());
		if (launchingModel != null) {
			checkLaunchingImports();
		}
		dialogChanged();
	}

	private void checkLaunchingImports() {
		IPlugin plugin = ((IPluginModel) launchingModel).getPlugin();
		IPluginImport[] imports = plugin.getImports();

		for (int i = 0; i < imports.length; i++) {
			String id = imports[i].getId();
			IPluginModelBase model = findModel(id);
			if (model != null)
				selected.add(model);
		}
		counter = selected.size();
		pluginListViewer.setCheckedElements(selected.toArray());
	}

	public void storeSettings(boolean finishPressed) {
		IDialogSettings settings = getDialogSettings();
		boolean showIds = !showNamesCheck.getSelection();
		if (finishPressed) {
			settings.put(SETTINGS_SHOW_IDS, showIds);
		}
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

		showNamesCheck = new Button(container, SWT.CHECK);
		showNamesCheck.setText(PDEPlugin.getResourceString(KEY_SHOW_NAMES));
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		showNamesCheck.setLayoutData(gd);
		showNamesCheck.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (!block)
					pluginListViewer.refresh();
			}
		});

		Label label = new Label(container, SWT.NULL);
		label.setText(PDEPlugin.getResourceString(KEY_PLUGIN_LIST));
		gd = new GridData();
		gd.horizontalSpan = 2;
		label.setLayoutData(gd);

		pluginListViewer = CheckboxTableViewer.newCheckList(container, SWT.BORDER);
		pluginListViewer.setContentProvider(new PluginContentProvider());
		pluginListViewer.setLabelProvider(new PluginLabelProvider());
		pluginListViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				modelChecked((IPluginModelBase) event.getElement(), event.getChecked());
			}
		});
		pluginListViewer.setSorter(ListUtil.NAME_SORTER);

		gd =
			new GridData(
				GridData.FILL_BOTH | GridData.GRAB_HORIZONTAL | GridData.GRAB_VERTICAL);
		gd.heightHint = 300;
		gd.widthHint = 300;
		gd.verticalSpan = 3;

		pluginListViewer.getTable().setLayoutData(gd);

		Composite buttons1 = createButtonContainer(container, 0);
		Composite buttons2 = createButtonContainer(container, 10);
		Composite buttons3 = createButtonContainer(container, 0);

		counterLabel = new Label(container, SWT.NONE);
		gd = new GridData();
		gd.horizontalAlignment = GridData.FILL;
		gd.horizontalSpan = 2;
		counterLabel.setLayoutData(gd);

		SelectionListener buttonListener = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				buttonSelected((Button) e.widget);
			}
		};

		selectAllButton =
			createButton(
				buttons1,
				PDEPlugin.getResourceString(KEY_SELECT_ALL),
				buttonListener);
		deselectAllButton =
			createButton(
				buttons1,
				PDEPlugin.getResourceString(KEY_DESELECT_ALL),
				buttonListener);

		existingButton =
			createButton(
				buttons2,
				PDEPlugin.getResourceString(KEY_EXISTING),
				buttonListener);
		existingBinaryButton =
			createButton(
				buttons2,
				PDEPlugin.getResourceString(KEY_EXISTING_BINARY),
				buttonListener);
		existingExternalButton =
			createButton(
				buttons2,
				PDEPlugin.getResourceString(KEY_EXISTING_EXTERNAL),
				buttonListener);

		addRequiredButton =
			createButton(
				buttons3,
				PDEPlugin.getResourceString(KEY_ADD_REQUIRED),
				buttonListener);

		counterLabel = new Label(container, SWT.NONE);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		counterLabel.setLayoutData(gd);
		setControl(container);
	}

	private Composite createButtonContainer(Composite container, int vmargin) {
		Composite buttonContainer = new Composite(container, SWT.NULL);
		GridData gd =
			new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL);
		buttonContainer.setLayoutData(gd);
		GridLayout buttonLayout = new GridLayout();
		buttonLayout.marginWidth = 0;
		buttonLayout.marginHeight = vmargin;
		buttonContainer.setLayout(buttonLayout);
		return buttonContainer;
	}

	private Button createButton(
		Composite container,
		String text,
		SelectionListener listener) {
		Button button = new Button(container, SWT.PUSH);
		button.setText(text);
		GridData gd =
			new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_BEGINNING);
		gd.heightHint = convertVerticalDLUsToPixels(IDialogConstants.BUTTON_HEIGHT);
		int widthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
		gd.widthHint =
			Math.max(
				widthHint,
				button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
		button.setLayoutData(gd);
		button.addSelectionListener(listener);
		return button;
	}

	public void dispose() {
		externalPluginImage.dispose();
		externalFragmentImage.dispose();
		super.dispose();
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
					models = (IPluginModelBase[]) registry.getModels();
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
			if (dropLocation != null) {
				IRunnableWithProgress op = new IRunnableWithProgress() {
					public void run(IProgressMonitor monitor) {
						monitor.beginTask(
							PDEPlugin.getResourceString(KEY_LOADING_FILE),
							IProgressMonitor.UNKNOWN);
						String[] paths = createPaths(dropLocation);

						MultiStatus errors =
							ExternalModelManager.processPluginDirectories(result, paths, false, monitor);
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
			models =
				(IPluginModelBase[]) result.toArray(new IPluginModelBase[result.size()]);
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
		return (IPluginModelBase[]) selected.toArray(
			new IPluginModelBase[selected.size()]);
	}

	private IStatus validatePlugins() {
		IPluginModelBase[] allModels = getModels();
		if (allModels == null || allModels.length == 0) {
			return createStatus(IStatus.ERROR, PDEPlugin.getResourceString(KEY_NO_PLUGINS));
		}
		if (selected.size() == 0) {
			return createStatus(
				IStatus.ERROR,
				PDEPlugin.getResourceString(KEY_NO_SELECTED));
		}
		return createStatus(IStatus.OK, "");
	}

	private void modelChecked(IPluginModelBase model, boolean checked) {
		if (checked) {
			selected.add(model);
			counter++;
		} else {
			selected.remove(model);
			counter--;
		}
		dialogChanged();
	}

	private void dialogChanged() {
		IStatus genStatus = validatePlugins();
		updateStatus(genStatus);
		updateCounterLabel();
	}

	private void buttonSelected(Button button) {
		if (button.equals(selectAllButton)) {
			selectAll();
			return;
		}
		if (button.equals(deselectAllButton)) {
			deselectAll();
			return;
		}
		ArrayList checked = null;
		if (button.equals(existingButton))
			checked = selectExistingProjects();
		else if (button.equals(existingBinaryButton))
			checked = selectLibraryProjects();
		else if (button.equals(existingExternalButton))
			checked = selectExternalProjects();
		else if (button.equals(addRequiredButton))
			checked = selectDependentPlugins();
		else
			return;
		for (Iterator iter = checked.iterator(); iter.hasNext();) {
			IPluginModelBase candidate = (IPluginModelBase) iter.next();
			if (!selected.contains(candidate)) {
				pluginListViewer.setChecked(candidate, true);
				selected.add(candidate);
				counter++;
			}
		}
		dialogChanged();
	}

	private void selectAll() {
		IPluginModelBase[] models = getModels();
		selected.clear();

		pluginListViewer.setAllChecked(true);
		for (int i = 0; i < models.length; i++) {
			selected.add(models[i]);
		}
		counter = models.length;
		dialogChanged();
	}

	private void deselectAll() {
		pluginListViewer.setAllChecked(false);
		selected.clear();
		counter = 0;
		dialogChanged();
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
		if (selected.size() > 0) {
			if (selected.size() > 1
				|| !((IPluginModelBase) selected.get(0)).getPluginBase().getId().equals(
					"org.eclipse.core.boot")) {
				addImplicitDependencies(checked);
			}
			for (int i = 0; i < selected.size(); i++) {
				addPluginAndDependent((IPluginModelBase) selected.get(i), checked);
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

	protected void updateCounterLabel() {
		String[] args = { "" + counter };
		String selectedLabelText = PDEPlugin.getFormattedMessage(KEY_SELECTED, args);
		counterLabel.setText(selectedLabelText);
	}
}