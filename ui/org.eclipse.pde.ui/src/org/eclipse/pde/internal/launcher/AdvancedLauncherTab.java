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
import org.eclipse.swt.layout.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.elements.*;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.pde.internal.preferences.ExternalPluginsBlock;
import org.eclipse.jface.dialogs.IDialogConstants;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.debug.ui.*;
import org.eclipse.debug.core.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.pde.internal.util.SWTUtil;

public class AdvancedLauncherTab
	extends AbstractLauncherTab
	implements ILaunchConfigurationTab, ILauncherSettings {
	public static final String KEY_WORKSPACE_PLUGINS =
		"Preferences.AdvancedTracingPage.workspacePlugins";
	public static final String KEY_EXTERNAL_PLUGINS =
		"Preferences.AdvancedTracingPage.externalPlugins";
	private Button useDefaultRadio;
	private Button useListRadio;
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

	public AdvancedLauncherTab() {
		PDEPlugin.getDefault().getLabelProvider().connect(this);
	}

	public void dispose() {
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
		super.dispose();
	}

	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		GridData gd;
		composite.setLayout(layout);

		createStartingSpace(composite, 1);
		
		useDefaultRadio = new Button(composite, SWT.RADIO);
		useDefaultRadio.setText("&Launch with all workspace and enabled external plug-ins");
		fillIntoGrid(useDefaultRadio, 1, false);
		
		useListRadio = new Button(composite, SWT.RADIO);
		useListRadio.setText("&Choose plug-ins and fragments to launch from the list");
		fillIntoGrid(useListRadio, 1, false);

		visibleLabel = new Label(composite, SWT.NULL);
		visibleLabel.setText("&Visible plug-ins and fragments:");
		fillIntoGrid(visibleLabel, 1, false);

		Control list = createPluginList(composite);
		gd = new GridData(GridData.FILL_BOTH);
		list.setLayoutData(gd);

		defaultsButton = new Button(composite, SWT.PUSH);
		defaultsButton.setText("Restore &Defaults");
		gd = new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING);
		defaultsButton.setLayoutData(gd);
		SWTUtil.setButtonDimensionHint(defaultsButton);

		hookListeners();
		pluginTreeViewer.reveal(workspacePlugins);
		setControl(composite);
	}

	private void hookListeners() {
		SelectionAdapter adapter = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				useDefaultChanged();
			}
		};
		useDefaultRadio.addSelectionListener(adapter);
		useListRadio.addSelectionListener(adapter);
		defaultsButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				Vector checked = computeInitialCheckState();
				pluginTreeViewer.setCheckedElements(checked.toArray());
				updateStatus();
			}
		});
	}

	private void useDefaultChanged() {
		boolean useDefault = useDefaultRadio.getSelection();
		adjustCustomControlEnableState(!useDefault);
		updateStatus();
	}

	private void adjustCustomControlEnableState(boolean enable) {
		visibleLabel.setEnabled(enable);
		pluginTreeViewer.getTree().setEnabled(enable);
		defaultsButton.setEnabled(enable);
		//restoreLabel.setEnabled(enable);
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

		Image pluginsImage =
			PDEPlugin.getDefault().getLabelProvider().get(
				PDEPluginImages.DESC_REQ_PLUGINS_OBJ);

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
		IPluginModelBase[] plugins = PDEPlugin.getDefault().getExternalModelManager().getModels();
		IPluginModelBase[] fragments = PDEPlugin.getDefault().getExternalModelManager().getFragmentModels(null);
		return getAllPlugins(plugins, fragments);
	}
	
	static IPluginModelBase [] getAllPlugins(IPluginModelBase[] plugins, IPluginModelBase[] fragments) {
		IPluginModelBase[] all =
			new IPluginModelBase[plugins.length + fragments.length];
		System.arraycopy(plugins, 0, all, 0, plugins.length);
		System.arraycopy(fragments, 0, all, plugins.length, fragments.length);
		return all;
	}

	static IPluginModelBase[] getWorkspacePlugins() {
		IPluginModelBase[] plugins =
			PDEPlugin.getDefault().getWorkspaceModelManager().getWorkspacePluginModels();
		IPluginModelBase[] fragments =
			PDEPlugin.getDefault().getWorkspaceModelManager().getWorkspaceFragmentModels();
		return getAllPlugins(plugins, fragments);
	}

	public void initializeFrom(ILaunchConfiguration config) {
		boolean useDefault = true;

		try {
			useDefault = config.getAttribute(USECUSTOM, useDefault);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
		// Need to set these before we refresh the viewer
		useDefaultRadio.setSelection(useDefault);
		useListRadio.setSelection(!useDefault);
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

			try {

				// Deselected workspace plug-ins
				String deselectedPluginIDs = config.getAttribute(WSPROJECT, (String) null);
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

				String exSettings = config.getAttribute(EXTPLUGINS, (String) null);
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
			} catch (CoreException e) {
				PDEPlugin.logException(e);
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

	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(USECUSTOM, true);
	}

	public void performApply(ILaunchConfigurationWorkingCopy config) {
		boolean useDefault = useDefaultRadio.getSelection();
		config.setAttribute(USECUSTOM, useDefault);

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
		config.setAttribute(WSPROJECT, wbuf.toString());
		config.setAttribute(EXTPLUGINS, exbuf.toString());
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
		/*
		if (findModel("org.eclipse.ui", plugins) != null) {
			if (findModel("org.eclipse.sdk", plugins) == null) {
				return createStatus(
					IStatus.WARNING,
					"'org.eclipse.sdk' not found. It is implicitly required by 'org.eclipse.ui'.");
			}
		};
		*/
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
		boolean useDefault = useDefaultRadio.getSelection();
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