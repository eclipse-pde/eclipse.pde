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
import org.eclipse.pde.model.plugin.*;
import org.eclipse.pde.internal.preferences.TargetPlatformPreferencePage;
import org.eclipse.pde.internal.wizards.StatusWizardPage;
import org.eclipse.swt.layout.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.elements.*;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.pde.internal.preferences.ExternalPluginsBlock;
import org.eclipse.jface.dialogs.IDialogConstants;

import org.eclipse.jface.dialogs.IDialogSettings;

public class WorkbenchLauncherWizardAdvancedPage
	extends StatusWizardPage
	implements ILauncherSettings {

	public static final String KEY_WORKSPACE_PLUGINS =
		"Preferences.AdvancedTracingPage.workspacePlugins";
	public static final String KEY_EXTERNAL_PLUGINS =
		"Preferences.AdvancedTracingPage.externalPlugins";
	private static final String KEY_OUT_OF_SYNC = "WorkspaceModelManager.outOfSync";

	private Button useDefaultCheck;
	private Button showNamesCheck;
	private CheckboxTreeViewer pluginTreeViewer;
	private Label visibleLabel;
	private Label restoreLabel;
	private NamedElement workspacePlugins;
	private NamedElement externalPlugins;
	private Vector externalList;
	private Vector workspaceList;
	private Button defaultsButton;

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

	static class ExternalState {
		String id;
		boolean state;
	}

	static class ExternalStates {
		Vector states = new Vector();

		ExternalStates() {
		}
		ExternalStates(String settings) {
			parseStates(settings);
		}

		public void parseStates(String settings) {
			StringTokenizer stok = new StringTokenizer(settings, File.pathSeparator);
			while (stok.hasMoreTokens()) {
				String token = stok.nextToken();
				ExternalState state = new ExternalState();
				int loc = token.lastIndexOf(',');
				if (loc == -1) {
					state.id = token;
					state.state = false;
				} else {
					state.id = token.substring(0, loc);
					state.state = token.charAt(loc + 1) == 't';
				}
				states.add(state);
			}
		}
		ExternalState getState(String id) {
			for (int i = 0; i < states.size(); i++) {
				ExternalState state = (ExternalState) states.get(i);
				if (state.id.equals(id)) {
					return state;
				}
			}
			return null;
		}
	}

	public WorkbenchLauncherWizardAdvancedPage(String title) {
		super("WorkbenchLauncherWizardAdvancedPage", false);
		setTitle(title);
		setDescription("Plug-ins and fragments visible to the plug-in loader.");
		PDEPlugin.getDefault().getLabelProvider().connect(this);
	}

	public void dispose() {
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
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
				updateStatus();
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
		pluginTreeViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
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
				updateStatus();
			}
		});
		pluginTreeViewer.setSorter(new ViewerSorter() {
			public int category(Object obj) {
				if (obj == workspacePlugins)
					return -1;
				if (obj == externalPlugins)
					return 1;
				return 0;
			}
		});
		
		Image pluginsImage = PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_REQ_PLUGINS_OBJ);

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

	static IPluginModelBase[] getExternalPlugins() {
		return PDEPlugin.getDefault().getExternalModelManager().getModels();
	}

	static IPluginModelBase[] getWorkspacePlugins() {
		IPluginModelBase[] plugins =
			PDEPlugin.getDefault().getWorkspaceModelManager().getWorkspacePluginModels();
		IPluginModelBase[] fragments =
			PDEPlugin.getDefault().getWorkspaceModelManager().getWorkspaceFragmentModels();
		IPluginModelBase[] all =
			new IPluginModelBase[plugins.length + fragments.length];
		System.arraycopy(plugins, 0, all, 0, plugins.length);
		System.arraycopy(fragments, 0, all, plugins.length, fragments.length);
		return all;
	}

	private void initializeFields() {
		IDialogSettings initialSettings = getDialogSettings();
		boolean useDefault = true;
		boolean showNames = true;

		if (initialSettings != null) {
			useDefault = !initialSettings.getBoolean(USECUSTOM);
		}
		// Need to set these before we refresh the viewer
		useDefaultCheck.setSelection(useDefault);
		showNamesCheck.setSelection(showNames);
		pluginTreeViewer.setInput(PDEPlugin.getDefault());
		Vector result = null;

		if (useDefault) {
			result = computeInitialCheckState();
		} else {
			result = new Vector();
			boolean addWorkspace = false;
			boolean addRoot = false;
			boolean mixed = false;

			IPluginModelBase[] ws = getWorkspacePlugins();

			// Deselected workspace plug-ins
			String deselectedPluginIDs = initialSettings.get(WSPROJECT);
			if (deselectedPluginIDs != null) {
				ArrayList deselected = new ArrayList();
				StringTokenizer tok =
					new StringTokenizer(deselectedPluginIDs, File.pathSeparator);
				while (tok.hasMoreTokens()) {
					deselected.add(tok.nextToken());
				}
				for (int i = 0; i < ws.length; i++) {
					IPluginModelBase desc = ws[i];
					if (!deselected.contains(desc.getPluginBase().getId())) {
						if (!addWorkspace) {
							addRoot = true;
							result.add(workspacePlugins);
						}
						result.add(desc);
					} else {
						mixed = true;
					}
				}
			}
			if (addRoot && mixed) {
				pluginTreeViewer.setGrayed(workspacePlugins, true);
			}
			// External state
			addRoot = false;
			mixed = false;

			IPluginModelBase[] ex = getExternalPlugins();

			String exSettings = initialSettings.get(EXTPLUGINS);
			if (exSettings == null)
				exSettings = "";
			ExternalStates states = new ExternalStates(exSettings);

			for (int i = 0; i < ex.length; i++) {
				IPluginModelBase desc = ex[i];
				ExternalState es = states.getState(desc.getPluginBase().getId());
				if (es != null) {
					// use the saved state
					if (es.state) {
						addRoot = true;
						result.add(desc);
					} else {
						mixed = true;
					}
				} else {
					// use the preference
					if (desc.isEnabled()) {
						addRoot = true;
						result.add(desc);
					} else {
						mixed = true;
					}
				}
			}
			if (addRoot) {
				result.add(externalPlugins);
				if (mixed) {
					pluginTreeViewer.setGrayed(externalPlugins, true);
				}
			}
		}
		pluginTreeViewer.setCheckedElements(result.toArray());
		adjustCustomControlEnableState(!useDefault);
		updateStatus();
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
		if (exMode.length() > 0) {
			if (exMode.equals(ExternalPluginsBlock.SAVED_ALL)) {
				checked.add(externalPlugins);
			} else if (!exMode.equals(ExternalPluginsBlock.SAVED_NONE)) {
				checked.add(externalPlugins);
				externalMixed = true;
			}
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
				pluginTreeViewer.setChecked(parent, true);
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
		initialSettings.put(USECUSTOM, !useDefault);

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
		IPluginModelBase[] externalModels = getExternalPlugins();

		// Store external state
		for (int i = 0; i < externalModels.length; i++) {
			IPluginModelBase model = (IPluginModelBase) externalModels[i];
			boolean state = pluginTreeViewer.getChecked(model);
			exbuf.append(model.getPluginBase().getId());
			exbuf.append(state ? ",t" : ",f");
			exbuf.append(File.pathSeparatorChar);
		}
		initialSettings.put(WSPROJECT, wbuf.toString());
		initialSettings.put(EXTPLUGINS, exbuf.toString());
	}

	static void setLauncherData(IDialogSettings settings, LauncherData data) {
		boolean useDefault = true;

		if (settings != null) {
			useDefault = !settings.getBoolean(USECUSTOM);
		}
		ArrayList res = new ArrayList();
		// Deselected workspace plug-ins
		ArrayList deselectedWSPlugins = new ArrayList();

		String wstring = settings.get(WSPROJECT);
		String exstring = settings.get(EXTPLUGINS);

		if (wstring != null) {
			StringTokenizer tok = new StringTokenizer(wstring, File.pathSeparator);
			while (tok.hasMoreTokens()) {
				deselectedWSPlugins.add(tok.nextToken());
			}
		}
		ExternalStates exstates = new ExternalStates();
		if (exstring != null) {
			exstates.parseStates(exstring);
		}

		IPluginModelBase[] models = getWorkspacePlugins();
		for (int i = 0; i < models.length; i++) {
			IPluginModelBase model = models[i];
			if (useDefault || !deselectedWSPlugins.contains(model.getPluginBase().getId()))
				res.add(model);
		}
		models = getExternalPlugins();
		for (int i = 0; i < models.length; i++) {
			IPluginModelBase model = models[i];
			if (useDefault) {
				if (model.isEnabled())
					res.add(model);
			} else {
				ExternalState es = exstates.getState(model.getPluginBase().getId());
				if (es != null && es.state) {
					res.add(model);
				} else if (model.isEnabled())
					res.add(model);
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