/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.ui.launcher;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.CoreSettings;
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEPluginImages;
import org.eclipse.pde.internal.ui.TargetPlatform;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.elements.NamedElement;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

public class AdvancedLauncherTab
	extends AbstractLauncherTab
	implements ILaunchConfigurationTab, ILauncherSettings {
	private static final String KEY_NAME = "AdvancedLauncherTab.name";
	private static final String KEY_WORKSPACE_PLUGINS =
		"AdvancedLauncherTab.workspacePlugins";
	private static final String KEY_EXTERNAL_PLUGINS =
		"AdvancedLauncherTab.externalPlugins";
	private static final String KEY_USE_DEFAULT =
		"AdvancedLauncherTab.useDefault";
	private static final String KEY_USE_LIST = "AdvancedLauncherTab.useList";
	private static final String KEY_VISIBLE_LIST =
		"AdvancedLauncherTab.visibleList";
	private static final String KEY_DEFAULTS = "AdvancedLauncherTab.defaults";
	private static final String KEY_PLUGIN_PATH =
		"AdvancedLauncherTab.pluginPath";
	private static final String KEY_PLUGIN_PATH_TITLE =
		"AdvancedLauncherTab.pluginPath.title";
	private static final String KEY_ERROR_NO_PLUGINS =
		"AdvancedLauncherTab.error.no_plugins";
	private static final String KEY_ERROR_NO_BOOT =
		"AdvancedLauncherTab.error.no_boot";
	private static final String KEY_ERROR_BROKEN_PLUGINS =
		"AdvancedLauncherTab.error.brokenPlugins";

	//private Button useDefaultRadio;
	//private Button useListRadio;
	private CheckboxTreeViewer pluginTreeViewer;
	private Label visibleLabel;
	private Label restoreLabel;
	private NamedElement workspacePlugins;
	private NamedElement externalPlugins;
	private Vector externalList;
	private Vector workspaceList;
	private Button defaultsButton;
	private Button pluginPathButton;

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
			StringTokenizer stok =
				new StringTokenizer(settings, File.pathSeparator);
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

		visibleLabel = new Label(composite, SWT.NULL);
		visibleLabel.setText(PDEPlugin.getResourceString(KEY_VISIBLE_LIST));
		fillIntoGrid(visibleLabel, 1, false);

		Control list = createPluginList(composite);
		gd = new GridData(GridData.FILL_BOTH);
		list.setLayoutData(gd);

		Composite buttonContainer = new Composite(composite, SWT.NULL);
		layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginWidth = layout.marginHeight = 0;
		layout.horizontalSpacing = 10;
		buttonContainer.setLayout(layout);

		gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		buttonContainer.setLayoutData(gd);

		defaultsButton = new Button(buttonContainer, SWT.PUSH);
		defaultsButton.setText(PDEPlugin.getResourceString(KEY_DEFAULTS));
		defaultsButton.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(defaultsButton);

		pluginPathButton = new Button(buttonContainer, SWT.PUSH);
		pluginPathButton.setText(PDEPlugin.getResourceString(KEY_PLUGIN_PATH));
		pluginPathButton.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(pluginPathButton);

		hookListeners();
		pluginTreeViewer.reveal(workspacePlugins);
		setControl(composite);
	}

	private void hookListeners() {
		defaultsButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				BusyIndicator
					.showWhile(
						pluginTreeViewer.getControl().getDisplay(),
						new Runnable() {
					public void run() {
						Vector checked = computeInitialCheckState();
						pluginTreeViewer.setCheckedElements(checked.toArray());
						updateStatus();
					}
				});
			}
		});
		pluginPathButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				showPluginPaths();
			}
		});
	}

	private void adjustCustomControlEnableState(boolean enable) {
		visibleLabel.setEnabled(enable);
		pluginTreeViewer.getTree().setEnabled(enable);
		defaultsButton.setEnabled(enable);
	}

	private GridData fillIntoGrid(Control control, int hspan, boolean grab) {
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = hspan;
		gd.grabExcessHorizontalSpace = grab;
		control.setLayoutData(gd);
		return gd;
	}

	protected Control createPluginList(final Composite parent) {
		pluginTreeViewer = new CheckboxTreeViewer(parent, SWT.BORDER);
		pluginTreeViewer.setContentProvider(new PluginContentProvider());
		pluginTreeViewer.setLabelProvider(
			PDEPlugin.getDefault().getLabelProvider());
		pluginTreeViewer.setAutoExpandLevel(2);
		pluginTreeViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(final CheckStateChangedEvent event) {
				final Object element = event.getElement();
				BusyIndicator.showWhile(parent.getDisplay(), new Runnable() {
					public void run() {
						if (element instanceof IPluginModelBase) {
							IPluginModelBase model =
								(IPluginModelBase) event.getElement();
							handleCheckStateChanged(model, event.getChecked());
						} else {
							handleGroupStateChanged(
								element,
								event.getChecked());
						}
						updateStatus();
					}
				});

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
		IPluginModelBase[] plugins =
			PDECore.getDefault().getExternalModelManager().getModels();
		IPluginModelBase[] fragments =
			PDECore.getDefault().getExternalModelManager().getFragmentModels(
				null);
		return getAllPlugins(plugins, fragments);
	}

	static IPluginModelBase[] getAllPlugins(
		IPluginModelBase[] plugins,
		IPluginModelBase[] fragments) {
		IPluginModelBase[] all =
			new IPluginModelBase[plugins.length + fragments.length];
		System.arraycopy(plugins, 0, all, 0, plugins.length);
		System.arraycopy(fragments, 0, all, plugins.length, fragments.length);
		return all;
	}

	static IPluginModelBase[] getWorkspacePlugins() {
		IPluginModelBase[] plugins =
			PDECore
				.getDefault()
				.getWorkspaceModelManager()
				.getWorkspacePluginModels();
		IPluginModelBase[] fragments =
			PDECore
				.getDefault()
				.getWorkspaceModelManager()
				.getWorkspaceFragmentModels();
		return getAllPlugins(plugins, fragments);
	}

	public void initializeFrom(ILaunchConfiguration config) {
		// Need to set these before we refresh the viewer
		//useDefaultRadio.setSelection(useDefault);
		//useListRadio.setSelection(!useDefault);
		if (pluginTreeViewer.getInput() == null)
			pluginTreeViewer.setInput(PDEPlugin.getDefault());
		boolean useDefault = true;
		try {
			useDefault = config.getAttribute(USECUSTOM, useDefault);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
		Vector result = null;
		{
			result = new Vector();
			boolean addRoot = false;
			boolean mixed = false;

			IPluginModelBase[] ws = getWorkspacePlugins();

			try {
				// Deselected workspace plug-ins
				String deselectedPluginIDs =
					config.getAttribute(WSPROJECT, (String) null);
				if (!useDefault && deselectedPluginIDs != null) {
					ArrayList deselected = new ArrayList();
					StringTokenizer tok =
						new StringTokenizer(
							deselectedPluginIDs,
							File.pathSeparator);
					while (tok.hasMoreTokens()) {
						deselected.add(tok.nextToken());
					}
					for (int i = 0; i < ws.length; i++) {
						IPluginModelBase desc = ws[i];
						if (!deselected
							.contains(desc.getPluginBase().getId())) {
							if (!addRoot) {
								addRoot = true;
								result.add(workspacePlugins);
							}
							result.add(desc);
						}
						else mixed=true;
					}
				} else {
					for (int i = 0; i < ws.length; i++) {
						result.add(ws[i]);
					}
					result.add(workspacePlugins);
					addRoot=true;
				}
				if (addRoot && mixed) {
					pluginTreeViewer.setGrayed(workspacePlugins, true);
				}
				// External state
				addRoot = false;
				mixed = false;

				IPluginModelBase[] ex = getExternalPlugins();

				String exSettings =
					config.getAttribute(EXTPLUGINS, (String) null);
				if (exSettings == null)
					exSettings = "";
				ExternalStates states = new ExternalStates(exSettings);

				for (int i = 0; i < ex.length; i++) {
					IPluginModelBase desc = ex[i];
					ExternalState es =
						states.getState(desc.getPluginBase().getId());
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
		//adjustCustomControlEnableState(false);
		updateStatus();
	}

	private String getModelKey(IPluginModelBase model) {
		IPluginBase plugin = model.getPluginBase();
		String id = plugin.getId();
		//String version = plugin.getVersion();
		//String key = id + "_"+version;
		String key = id;
		return key;
	}

	private Vector computeInitialCheckState() {
		IPluginModelBase[] models = (IPluginModelBase[]) getWorkspacePlugins();
		Vector checked = new Vector();
		Hashtable wtable = new Hashtable();

		for (int i = 0; i < models.length; i++) {
			IPluginModelBase model = models[i];
			checked.add(model);
			wtable.put(getModelKey(model), model);
		}
		checked.add(workspacePlugins);
		if (pluginTreeViewer.getGrayed(workspacePlugins))
			pluginTreeViewer.setGrayed(workspacePlugins, false);

		models = (IPluginModelBase[]) getExternalPlugins();
		for (int i = 0; i < models.length; i++) {
			IPluginModelBase model = models[i];
			boolean masked = wtable.get(getModelKey(model)) != null;
			if (!masked && model.isEnabled())
				checked.add(model);
		}
		CoreSettings pstore = PDECore.getDefault().getSettings();
		String exMode = pstore.getString(ICoreConstants.CHECKED_PLUGINS);
		boolean externalMixed = false;
		if (exMode.length() > 0) {
			if (exMode.equals(ICoreConstants.VALUE_SAVED_ALL)) {
				checked.add(externalPlugins);
			} else if (!exMode.equals(ICoreConstants.VALUE_SAVED_NONE)) {
				checked.add(externalPlugins);
				externalMixed = true;
			}
		}
		if (pluginTreeViewer.getGrayed(externalPlugins) != externalMixed)
			pluginTreeViewer.setGrayed(externalPlugins, externalMixed);
		return checked;
	}

	private void handleCheckStateChanged(
		IPluginModelBase model,
		boolean checked) {
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
		setChanged(true);
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
		setChanged(true);
	}

	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		//config.setAttribute(USECUSTOM, true);
	}

	public void performApply(ILaunchConfigurationWorkingCopy config) {
		if (!isChanged())
			return;
		config.setAttribute(USECUSTOM, false);

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
		setChanged(false);
	}

	private void showPluginPaths() {
		IPluginModelBase[] plugins = getPlugins();
		try {
			URL[] urls = TargetPlatform.createPluginPath(plugins);
			PluginPathDialog dialog =
				new PluginPathDialog(pluginPathButton.getShell(), urls);
			dialog.create();
			dialog.getShell().setText(
				PDEPlugin.getResourceString(KEY_PLUGIN_PATH_TITLE));
			SWTUtil.setDialogSize(dialog, 500, 400);
			dialog.open();
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	private void updateStatus() {
		IStatus genStatus = validatePlugins();
		updateStatus(genStatus);
	}

	private IStatus validatePlugins() {
		IPluginModelBase[] plugins = getPlugins();
		if (plugins.length == 0) {
			return createStatus(
				IStatus.ERROR,
				PDEPlugin.getResourceString(KEY_ERROR_NO_PLUGINS));
		}
		IPluginModelBase boot = findModel("org.eclipse.core.boot", plugins);
		if (boot == null) {
			return createStatus(
				IStatus.ERROR,
				PDEPlugin.getResourceString(KEY_ERROR_NO_BOOT));
		}
		for (int i=0; i<plugins.length; i++) {
			IPluginModelBase model = plugins[i];
			if (model.isLoaded()==false) {
				return createStatus(
				IStatus.WARNING,
				PDEPlugin.getResourceString(KEY_ERROR_BROKEN_PLUGINS));
			}
		}
		return createStatus(IStatus.OK, "");
	}

	private IPluginModelBase findModel(String id, IPluginModelBase[] models) {
		for (int i = 0; i < models.length; i++) {
			IPluginModelBase model = (IPluginModelBase) models[i];
			IPluginBase pluginBase = model.getPluginBase();
			if (pluginBase != null) {
				String pid = pluginBase.getId();
				if (pid != null && pid.equals(id))
					return model;
			}
		}
		return null;
	}

	/**
	 * Returns the selected plugins.
	 */

	public IPluginModelBase[] getPlugins() {
		ArrayList res = new ArrayList();
		boolean useDefault = false; //useDefaultRadio.getSelection();
		if (useDefault) {
			IPluginModelBase[] models = getWorkspacePlugins();
			Hashtable wtable = new Hashtable();
			for (int i = 0; i < models.length; i++) {
				res.add(models[i]);
				wtable.put(getModelKey(models[i]), models[i]);
			}
			models = getExternalPlugins();
			for (int i = 0; i < models.length; i++) {
				IPluginModelBase model = models[i];
				boolean masked = wtable.get(getModelKey(model)) != null;
				if (!masked && models[i].isEnabled())
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
		return (IPluginModelBase[]) res.toArray(
			new IPluginModelBase[res.size()]);
	}
	public String getName() {
		return PDEPlugin.getResourceString(KEY_NAME);
	}
	public Image getImage() {
		return PDEPlugin.getDefault().getLabelProvider().get(
			PDEPluginImages.DESC_REQ_PLUGINS_OBJ);
	}
}