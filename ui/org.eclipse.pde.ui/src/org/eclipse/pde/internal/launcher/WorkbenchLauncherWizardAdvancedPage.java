/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.launcher;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.pde.internal.*;
import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.pde.internal.preferences.PDEBasePreferencePage;
import org.eclipse.pde.internal.wizards.StatusWizardPage;
import org.eclipse.swt.layout.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.elements.*;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.pde.internal.preferences.ExternalPluginsBlock;
import org.eclipse.jface.dialogs.IDialogConstants;

import org.eclipse.jface.dialogs.IDialogSettings;

public class WorkbenchLauncherWizardAdvancedPage extends StatusWizardPage {

	private static final String SETTINGS_USECUSTOM = "default";
	private static final String SETTINGS_SHOWNAMES = "showNames";
	private static final String SETTINGS_WSPROJECT = "wsproject";
	private static final String SETTINGS_EXTPLUGINS = "extplugins";
	public static final String KEY_WORKSPACE_PLUGINS =
		"Preferences.AdvancedTracingPage.workspacePlugins";
	public static final String KEY_EXTERNAL_PLUGINS =
		"Preferences.AdvancedTracingPage.externalPlugins";

	private static final String SETTINGS_PREVPATH = "prevpath";

	private Button useDefaultCheck;
	private Button showNamesCheck;
	private CheckboxTreeViewer pluginTreeViewer;
	private Label visibleLabel;
	private Label restoreLabel;
	private Image pluginImage;
	private Image fragmentImage;
	private Image pluginsImage;
	private NamedElement workspacePlugins;
	private NamedElement externalPlugins;
	private Vector externalList;
	private Vector workspaceList;
	private Button defaultsButton;

	class PluginLabelProvider extends LabelProvider {
		public String getText(Object obj) {
			if (obj instanceof IPluginModelBase) {
				IPluginModelBase model = (IPluginModelBase) obj;
				IPluginBase plugin = model.getPluginBase();
				String name = plugin.getId();
				if (showNamesCheck.getSelection())
					name = plugin.getTranslatedName();
				return name + " (" + plugin.getVersion() + ")";
			}
			return obj.toString();
		}
		public Image getImage(Object obj) {
			if (obj instanceof IPluginModel)
				return pluginImage;
			if (obj instanceof IFragmentModel)
				return fragmentImage;
			if (obj instanceof NamedElement)
				return ((NamedElement) obj).getImage();
			return null;
		}
	}

	class PluginContentProvider
		extends DefaultContentProvider
		implements ITreeContentProvider {
		public boolean hasChildren(Object parent) {
			if (parent instanceof IPluginModelBase)
				return false;
			return true;
		}
		public Object[] getChildren(Object parent) {
			if (parent == externalPlugins) {
				return getExternalPlugins();
			}
			if (parent == workspacePlugins) {
				return getWorkspacePlugins();
			}
			return new Object[0];
		}
		public Object getParent(Object child) {
			if (child instanceof IPluginModelBase) {
				IPluginModelBase model = (IPluginModelBase) child;
				if (model.getUnderlyingResource() != null)
					return workspacePlugins;
				else
					return externalPlugins;
			}
			return null;
		}
		public Object[] getElements(Object input) {
			return new Object[] { workspacePlugins, externalPlugins };
		}
	}

	public WorkbenchLauncherWizardAdvancedPage(String title) {
		super("WorkbenchLauncherWizardAdvancedPage", false);
		setTitle(title);
		setDescription("Plug-ins and fragments visible to the plug-in loader.");
		pluginImage = PDEPluginImages.DESC_PLUGIN_OBJ.createImage();
		fragmentImage = PDEPluginImages.DESC_FRAGMENT_OBJ.createImage();
		pluginsImage = PDEPluginImages.DESC_REQ_PLUGINS_OBJ.createImage();
	}

	public void dispose() {
		pluginImage.dispose();
		fragmentImage.dispose();
		pluginsImage.dispose();
		super.dispose();
	}

	public void createControl(Composite parent) {
		initializeDialogUnits(parent);

		Composite composite = new Composite(parent, SWT.NONE);

		GridLayout layout = new GridLayout();
		GridData gd;
		layout.numColumns = 2;
		composite.setLayout(layout);

		useDefaultCheck = new Button(composite, SWT.CHECK);
		useDefaultCheck.setText("&Use default");
		fillIntoGrid(useDefaultCheck, 2, false);

		Label label = new Label(composite, SWT.WRAP);
		label.setText(
			"If this option is checked, the workbench instance you are about to launch will 'see' all the plug-ins and fragments in the workspace, as well as all the external projects enabled in the Preferences.");
		gd = fillIntoGrid(label, 2, false);
		gd.widthHint = convertWidthInCharsToPixels(70);

		label = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
		fillIntoGrid(label, 2, false);

		showNamesCheck = new Button(composite, SWT.CHECK);
		showNamesCheck.setText("Show full plug-in and fragment names");
		fillIntoGrid(showNamesCheck, 2, false);

		visibleLabel = new Label(composite, SWT.NULL);
		visibleLabel.setText("Visible plug-ins and fragments:");
		fillIntoGrid(visibleLabel, 2, false);

		Control list = createPluginList(composite);
		gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = 2;
		//gd.verticalSpan = 2;
		gd.heightHint = 250;
		list.setLayoutData(gd);

		defaultsButton = new Button(composite, SWT.PUSH);
		defaultsButton.setText("Restore &Defaults");
		gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.heightHint = convertVerticalDLUsToPixels(IDialogConstants.BUTTON_HEIGHT);
		int widthHint = convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
		gd.widthHint =
			Math.max(
				widthHint,
				defaultsButton.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
		defaultsButton.setLayoutData(gd);

		restoreLabel = new Label(composite, SWT.NULL);
		restoreLabel.setText(
			"You can restore plug-in and fragment visibility to the default values.");
		gd = fillIntoGrid(restoreLabel, 1, false);
		//gd.verticalAlignment = GridData.BEGINNING;

		initializeFields();
		hookListeners();
		pluginTreeViewer.reveal(workspacePlugins);
		setControl(composite);
	}

	private void hookListeners() {
		useDefaultCheck.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				useDefaultChanged();
			}
		});
		showNamesCheck.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				pluginTreeViewer.refresh();
			}
		});
		defaultsButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				Vector checked = computeInitialCheckState();
				pluginTreeViewer.setCheckedElements(checked.toArray());
				IDialogSettings settings = getDialogSettings();
				settings.put(SETTINGS_WSPROJECT, (String) null);
				settings.put(SETTINGS_EXTPLUGINS, (String) null);
			}
		});
	}

	private void useDefaultChanged() {
		boolean useDefault = useDefaultCheck.getSelection();
		adjustCustomControlEnableState(!useDefault);
		updateStatus();
	}

	private void adjustCustomControlEnableState(boolean enable) {
		visibleLabel.setEnabled(enable);
		showNamesCheck.setEnabled(enable);
		pluginTreeViewer.getTree().setEnabled(enable);
		defaultsButton.setEnabled(enable);
		restoreLabel.setEnabled(enable);
	}

	private GridData fillIntoGrid(Control control, int hspan, boolean grab) {
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = hspan;
		gd.grabExcessHorizontalSpace = grab;
		control.setLayoutData(gd);
		return gd;
	}

	protected Control createPluginList(Composite parent) {
		pluginTreeViewer = new CheckboxTreeViewer(parent, SWT.BORDER);
		pluginTreeViewer.setContentProvider(new PluginContentProvider());
		pluginTreeViewer.setLabelProvider(new PluginLabelProvider());
		pluginTreeViewer.setAutoExpandLevel(2);
		pluginTreeViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				Object element = event.getElement();
				if (element instanceof IPluginModelBase) {
					IPluginModelBase model = (IPluginModelBase) event.getElement();
					handleCheckStateChanged(model, event.getChecked());
				} else {
					handleGroupStateChanged(element, event.getChecked());
				}
			}
		});
		workspacePlugins =
			new NamedElement(
				PDEPlugin.getResourceString(KEY_WORKSPACE_PLUGINS),
				pluginsImage);
		externalPlugins =
			new NamedElement(
				PDEPlugin.getResourceString(KEY_EXTERNAL_PLUGINS),
				pluginsImage);
		return pluginTreeViewer.getTree();
	}

	private static IPluginModelBase[] getExternalPlugins() {
		return PDEPlugin.getDefault().getExternalModelManager().getModels();
	}

	private static IPluginModelBase[] getWorkspacePlugins() {
		IPluginModelBase[] plugins =
			PDEPlugin.getDefault().getWorkspaceModelManager().getWorkspacePluginModels();
		IPluginModelBase[] fragments =
			PDEPlugin.getDefault().getWorkspaceModelManager().getWorkspaceFragmentModels();
		IPluginModelBase[] all = new IPluginModelBase[plugins.length + fragments.length];
		System.arraycopy(plugins, 0, all, 0, plugins.length);
		System.arraycopy(fragments, 0, all, plugins.length, fragments.length);
		return all;
	}

	private void initializeFields() {
		IDialogSettings initialSettings = getDialogSettings();
		boolean useDefault = true;
		boolean showNames = true;

		if (initialSettings != null) {
			useDefault = !initialSettings.getBoolean(SETTINGS_USECUSTOM);
			showNames = !initialSettings.getBoolean(SETTINGS_SHOWNAMES);
		}
		// Need to set these before we refresh the viewer
		useDefaultCheck.setSelection(useDefault);
		showNamesCheck.setSelection(showNames);
		pluginTreeViewer.setInput(PDEPlugin.getDefault());
		Vector checked = computeInitialCheckState();

		// Deselected workspace plug-ins
		String deselectedPluginIDs = initialSettings.get(SETTINGS_WSPROJECT);
		if (deselectedPluginIDs != null) {
			ArrayList deselected = new ArrayList();
			StringTokenizer tok =
				new StringTokenizer(deselectedPluginIDs, File.pathSeparator);
			while (tok.hasMoreTokens()) {
				deselected.add(tok.nextToken());
			}
			for (int i = checked.size() - 1; i >= 0; i--) {
				Object curr = checked.get(i);
				if (!(curr instanceof IPluginModelBase))
					continue;
				IPluginModelBase desc = (IPluginModelBase) curr;
				if (deselected.contains(desc.getPluginBase().getId())) {
					checked.remove(i);
				}
			}
		}
		// Deselected external plug-ins
		deselectedPluginIDs = initialSettings.get(SETTINGS_EXTPLUGINS);
		if (deselectedPluginIDs != null) {
			ArrayList deselected = new ArrayList();
			StringTokenizer tok =
				new StringTokenizer(deselectedPluginIDs, File.pathSeparator);
			while (tok.hasMoreTokens()) {
				deselected.add(tok.nextToken());
			}
			boolean mixed = false;
			for (int i = checked.size() - 1; i >= 0; i--) {
				Object curr = checked.get(i);
				if (!(curr instanceof IPluginModelBase))
					continue;
				IPluginModelBase desc = (IPluginModelBase) curr;
				if (deselected.contains(desc.getPluginBase().getId())) {
					checked.remove(i);
					mixed = true;
				}
			}
			if (mixed) {
				pluginTreeViewer.setGrayed(externalPlugins, true);
			}
		}
		pluginTreeViewer.setCheckedElements(checked.toArray());
		adjustCustomControlEnableState(!useDefault);
	}

	private Vector computeInitialCheckState() {
		IPluginModelBase[] models = (IPluginModelBase[]) getWorkspacePlugins();
		Vector checked = new Vector();

		for (int i = 0; i < models.length; i++) {
			checked.add(models[i]);
		}
		checked.add(workspacePlugins);
		if (pluginTreeViewer.getGrayed(workspacePlugins))
			pluginTreeViewer.setGrayed(workspacePlugins, false);

		models = (IPluginModelBase[]) getExternalPlugins();
		for (int i = 0; i < models.length; i++) {
			IPluginModelBase model = models[i];
			if (model.isEnabled())
				checked.add(model);
		}
		IPreferenceStore pstore = PDEPlugin.getDefault().getPreferenceStore();
		String exMode = pstore.getString(ExternalPluginsBlock.CHECKED_PLUGINS);
		boolean externalMixed = false;
		if (exMode.length() == 0 || exMode.equals(ExternalPluginsBlock.SAVED_ALL)) {
			checked.add(externalPlugins);
		} else if (!exMode.equals(ExternalPluginsBlock.SAVED_NONE)) {
			checked.add(externalPlugins);
			externalMixed = true;
		}
		if (pluginTreeViewer.getGrayed(externalPlugins) != externalMixed)
			pluginTreeViewer.setGrayed(externalPlugins, externalMixed);
		return checked;
	}

	private void handleCheckStateChanged(IPluginModelBase model, boolean checked) {
		boolean external = model.getUnderlyingResource() == null;
		NamedElement parent = external ? externalPlugins : workspacePlugins;
		IPluginModelBase[] siblings;

		if (external) {
			siblings = (IPluginModelBase[]) getExternalPlugins();
		} else {
			siblings = (IPluginModelBase[]) getWorkspacePlugins();
		}

		int groupState = -1;

		for (int i = 0; i < siblings.length; i++) {
			boolean state = pluginTreeViewer.getChecked(siblings[i]);
			if (groupState == -1)
				groupState = state ? 1 : 0;
			else if (groupState == 1 && state == false) {
				groupState = -1;
				break;
			} else if (groupState == 0 && state == true) {
				groupState = -1;
				break;
			}
		}
		// If group state is -1 (mixed), we should gray the parent.
		// Otherwise, we should set it to the children group state
		switch (groupState) {
			case 0 :
				pluginTreeViewer.setChecked(parent, false);
				pluginTreeViewer.setGrayed(parent, false);
				break;
			case 1 :
				pluginTreeViewer.setChecked(parent, true);
				pluginTreeViewer.setGrayed(parent, false);
				break;
			case -1 :
				pluginTreeViewer.setGrayed(parent, true);
				break;
		}
	}

	private void handleGroupStateChanged(Object group, boolean checked) {
		IPluginModelBase[] models;

		if (group.equals(workspacePlugins)) {
			models = (IPluginModelBase[]) getWorkspacePlugins();
		} else {
			models = (IPluginModelBase[]) getExternalPlugins();
		}
		for (int i = 0; i < models.length; i++) {
			pluginTreeViewer.setChecked(models[i], checked);
		}
		if (pluginTreeViewer.getGrayed(group))
			pluginTreeViewer.setGrayed(group, false);
	}

	public void storeSettings() {
		IDialogSettings initialSettings = getDialogSettings();
		boolean useDefault = useDefaultCheck.getSelection();
		initialSettings.put(SETTINGS_USECUSTOM, !useDefault);
		initialSettings.put(SETTINGS_SHOWNAMES, !showNamesCheck.getSelection());

		if (useDefault)
			return;
		// store deselected projects
		StringBuffer wbuf = new StringBuffer();
		IPluginModelBase[] workspaceModels = getWorkspacePlugins();

		for (int i = 0; i < workspaceModels.length; i++) {
			IPluginModelBase model = (IPluginModelBase) workspaceModels[i];
			if (pluginTreeViewer.getChecked(model))
				continue;
			wbuf.append(model.getPluginBase().getId());
			wbuf.append(File.pathSeparatorChar);
		}

		StringBuffer exbuf = new StringBuffer();
		IPluginModelBase[] externalModels = (IPluginModelBase[]) getExternalPlugins();

		for (int i = 0; i < externalModels.length; i++) {
			IPluginModelBase model = (IPluginModelBase) externalModels[i];
			if (pluginTreeViewer.getChecked(model))
				continue;
			exbuf.append(model.getPluginBase().getId());
			exbuf.append(File.pathSeparatorChar);
		}
		initialSettings.put(SETTINGS_WSPROJECT, wbuf.toString());
		initialSettings.put(SETTINGS_EXTPLUGINS, exbuf.toString());
	}

	static void setLauncherData(IDialogSettings settings, LauncherData data) {
		boolean useDefault = true;

		if (settings != null) {
			useDefault = !settings.getBoolean(SETTINGS_USECUSTOM);
		}
		ArrayList res = new ArrayList();
		// Deselected workspace plug-ins
		ArrayList deselectedWSPlugins = new ArrayList();
		ArrayList deselectedExPlugins = new ArrayList();
		String wstring = settings.get(SETTINGS_WSPROJECT);
		String exstring = settings.get(SETTINGS_EXTPLUGINS);
		if (wstring != null) {
			StringTokenizer tok = new StringTokenizer(wstring, File.pathSeparator);
			while (tok.hasMoreTokens()) {
				deselectedWSPlugins.add(tok.nextToken());
			}
		}
		if (exstring != null) {
			StringTokenizer tok = new StringTokenizer(exstring, File.pathSeparator);
			while (tok.hasMoreTokens()) {
				deselectedExPlugins.add(tok.nextToken());
			}
		}

		IPluginModelBase[] models = getWorkspacePlugins();
		for (int i = 0; i < models.length; i++) {
			IPluginModelBase model = models[i];
			if (useDefault || !deselectedWSPlugins.contains(model.getPluginBase().getId()))
				res.add(model);
		}
		models = getExternalPlugins();
		for (int i = 0; i < models.length; i++) {
			if (useDefault) {
				if (models[i].isEnabled())
					res.add(models[i]);
			} else if (!deselectedExPlugins.contains(models[i].getPluginBase().getId())) {
				res.add(models[i]);
			}
		}
		IPluginModelBase[] plugins =
			(IPluginModelBase[]) res.toArray(new IPluginModelBase[res.size()]);
		data.setPlugins(plugins);
	}

	private void updateStatus() {
		IStatus genStatus = validatePlugins();
		updateStatus(genStatus);
	}

	private IStatus validatePlugins() {
		IPluginModelBase[] plugins = getPlugins();
		if (plugins.length == 0) {
			return createStatus(IStatus.ERROR, "No plugins available.");
		}
		IPluginModelBase boot = findModel("org.eclipse.core.boot", plugins);
		if (boot == null) {
			return createStatus(IStatus.ERROR, "Plugin 'org.eclipse.core.boot' not found.");
		}
		if (findModel("org.eclipse.ui", plugins) != null) {
			if (findModel("org.eclipse.sdk", plugins) == null) {
				return createStatus(
					IStatus.WARNING,
					"'org.eclipse.sdk' not found. It is implicitly required by 'org.eclipse.ui'.");
			}
			File bootDir = new File(boot.getInstallLocation());
			File installDir = new File(bootDir.getParentFile().getParentFile(), "install");
			if (!installDir.exists()) {
				return createStatus(
					IStatus.WARNING,
					installDir.getPath()
						+ " not found.\nThe install directory is required by 'org.eclipse.ui'.");
			}
		};
		return createStatus(IStatus.OK, "");
	}

	private IPluginModelBase findModel(String id, IPluginModelBase[] models) {
		for (int i = 0; i < models.length; i++) {
			IPluginModelBase model = (IPluginModelBase) models[i];
			if (model.getPluginBase().getId().equals(id))
				return model;
		}
		return null;
	}

	/**
	 * Returns the selected plugins.
	 */

	public IPluginModelBase[] getPlugins() {
		ArrayList res = new ArrayList();
		boolean useDefault = useDefaultCheck.getSelection();
		if (useDefault) {
			IPluginModelBase[] models = getWorkspacePlugins();
			for (int i = 0; i < models.length; i++) {
				res.add(models[i]);
			}
			models = getExternalPlugins();
			for (int i = 0; i < models.length; i++) {
				if (models[i].isEnabled())
					res.add(models[i]);
			}

		} else {
			Object[] elements = pluginTreeViewer.getCheckedElements();
			for (int i = 0; i < elements.length; i++) {
				Object element = elements[i];
				if (element instanceof IPluginModelBase)
					res.add(element);
			}
		}
		return (IPluginModelBase[]) res.toArray(new IPluginModelBase[res.size()]);
	}
}