/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.ui.launcher;

import java.io.File;
import java.net.URL;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.*;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.elements.*;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.pde.internal.ui.wizards.ListUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.help.WorkbenchHelp;

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
	private static final String KEY_USE_FEATURES =
		"AdvancedLauncherTab.useFeatures";
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

	private Button useDefaultRadio;
	private Button useFeaturesRadio;
	private Button useListRadio;
	private CheckboxTreeViewer pluginTreeViewer;
	private Label visibleLabel;
	private Label restoreLabel;
	private NamedElement workspacePlugins;
	private NamedElement externalPlugins;
	private IPluginModelBase[] externalModels;
	private IPluginModelBase[] workspaceModels;
	private Button defaultsButton;
	private Button pluginPathButton;
	private int numExternalChecked = 0;
	private int numWorkspaceChecked = 0;
	private boolean firstReveal = true;
	private boolean check;

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
				return externalModels;
			}
			if (parent == workspacePlugins) {
				return workspaceModels;
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
		externalModels = getExternalPlugins();
		workspaceModels = getWorkspacePlugins();
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
		useDefaultRadio.setText(PDEPlugin.getResourceString(KEY_USE_DEFAULT));
		fillIntoGrid(useDefaultRadio, 1, false);

		useFeaturesRadio = new Button(composite, SWT.RADIO);
		useFeaturesRadio.setText(PDEPlugin.getResourceString(KEY_USE_FEATURES));
		fillIntoGrid(useFeaturesRadio, 1, false);

		useListRadio = new Button(composite, SWT.RADIO);
		useListRadio.setText(PDEPlugin.getResourceString(KEY_USE_LIST));
		fillIntoGrid(useListRadio, 1, false);

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

		pluginPathButton = new Button(buttonContainer, SWT.PUSH);
		pluginPathButton.setText(PDEPlugin.getResourceString(KEY_PLUGIN_PATH));
		pluginPathButton.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(pluginPathButton);

		defaultsButton = new Button(buttonContainer, SWT.PUSH);
		defaultsButton.setText(PDEPlugin.getResourceString(KEY_DEFAULTS));
		defaultsButton.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(defaultsButton);

		hookListeners();
		setControl(composite);

		WorkbenchHelp.setHelp(composite, IHelpContextIds.LAUNCHER_ADVANCED);
	}

	private void hookListeners() {
		SelectionAdapter adapter = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (firstReveal) {
					pluginTreeViewer.reveal(workspacePlugins);
					firstReveal = false;
				}
				useDefaultChanged();
			}
		};
		useDefaultRadio.addSelectionListener(adapter);
		useFeaturesRadio.addSelectionListener(adapter);
		//useListRadio.addSelectionListener(adapter);
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
						setChanged(true);
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

	private void useDefaultChanged() {
		boolean useDefault = !useListRadio.getSelection();
		adjustCustomControlEnableState(!useDefault);
		pluginPathButton.setEnabled(!useFeaturesRadio.getSelection());
		if (!updateStatus())
			updateLaunchConfigurationDialog();
		/*else 
			setChanged(true);*/
	}

	private void adjustCustomControlEnableState(boolean enable) {
		//		visibleLabel.setEnabled(enable);
		//		pluginTreeViewer.getTree().setEnabled(enable);
		//		defaultsButton.setEnabled(enable);
		visibleLabel.setVisible(enable);
		pluginTreeViewer.getTree().setVisible(enable);
		defaultsButton.setVisible(enable);
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
						if (!updateStatus()) {
							check = true;
							updateLaunchConfigurationDialog();
						}

					}
				});

			}
		});
		pluginTreeViewer.setSorter(new ListUtil.PluginSorter() {
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
			PDECore.getDefault().getExternalModelManager().getFragmentModels();
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
	}

	private ArrayList parseDeselectedIds(ILaunchConfiguration config)
		throws CoreException {
		ArrayList deselected = new ArrayList();
		String deselectedPluginIDs =
			config.getAttribute(WSPROJECT, (String) null);
		if (deselectedPluginIDs != null) {
			StringTokenizer tok =
				new StringTokenizer(deselectedPluginIDs, File.pathSeparator);
			while (tok.hasMoreTokens()) {
				deselected.add(tok.nextToken());
			}
		}
		return deselected;
	}

	private ArrayList initWorkspacePluginsState(ILaunchConfiguration config)
		throws CoreException {

		ArrayList result = new ArrayList();
		ArrayList deselected = parseDeselectedIds(config);

		for (int i = 0; i < workspaceModels.length; i++) {
			IPluginModelBase desc = workspaceModels[i];
			if (!deselected.contains(desc.getPluginBase().getId())) {
				result.add(desc);
			}
		}

		int size = result.size();

		if (size > 0)
			result.add(workspacePlugins);
		pluginTreeViewer.setGrayed(
			workspacePlugins,
			size > 0 && size < workspaceModels.length);
		return result;
	}

	private ArrayList initExternalPluginsState(ILaunchConfiguration config)
		throws CoreException {

		String exSettings = config.getAttribute(EXTPLUGINS, (String) null);
		ExternalStates states =
			new ExternalStates((exSettings != null) ? exSettings : "");
		ArrayList result = new ArrayList();

		for (int i = 0; i < externalModels.length; i++) {
			IPluginModelBase desc = externalModels[i];
			ExternalState es = states.getState(desc.getPluginBase().getId());
			if (es != null) {
				// use the saved state
				if (es.state) {
					result.add(desc);
				}
			} else if (desc.isEnabled()) {
				result.add(desc);
			}
		}

		int size = result.size();

		if (size > 0)
			result.add(externalPlugins);
		pluginTreeViewer.setGrayed(
			externalPlugins,
			size > 0 && size < externalModels.length);

		return result;
	}

	public void initialize(ILaunchConfiguration config) {

		try {
			useDefaultRadio.setSelection(config.getAttribute(USECUSTOM, true));
			useFeaturesRadio.setSelection(
				config.getAttribute(USEFEATURES, false));
			useListRadio.setSelection(!useDefaultRadio.getSelection());

			if (pluginTreeViewer.getInput() == null)
				pluginTreeViewer.setInput(PDEPlugin.getDefault());

			if (useDefaultRadio.getSelection()) {
				pluginTreeViewer.setCheckedElements(
					computeInitialCheckState().toArray());
			} else if (useListRadio.getSelection()) {
				ArrayList result = initWorkspacePluginsState(config);
				result.addAll(initExternalPluginsState(config));
				pluginTreeViewer.setCheckedElements(result.toArray());
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}

		adjustCustomControlEnableState(useListRadio.getSelection());
		updateStatus();
	}

	private Vector computeInitialCheckState() {
		Vector checked = new Vector();
		Hashtable wtable = new Hashtable();
		numWorkspaceChecked = 0;
		numExternalChecked = 0;

		for (int i = 0; i < workspaceModels.length; i++) {
			IPluginModelBase model = workspaceModels[i];
			checked.add(model);
			numWorkspaceChecked += 1;
			String id = model.getPluginBase().getId();
			if (id != null)
				wtable.put(model.getPluginBase().getId(), model);
		}
		if (checked.size() > 0)
			checked.add(workspacePlugins);
		pluginTreeViewer.setGrayed(workspacePlugins, false);

		for (int i = 0; i < externalModels.length; i++) {
			IPluginModelBase model = externalModels[i];
			boolean masked = wtable.get(model.getPluginBase().getId()) != null;
			if (!masked && model.isEnabled()) {
				checked.add(model);
				numExternalChecked += 1;
			}
		}

		if (numExternalChecked > 0)
			checked.add(externalPlugins);
		pluginTreeViewer.setGrayed(
			externalPlugins,
			numExternalChecked > 0
				&& numExternalChecked < externalModels.length);

		return checked;
	}

	private void handleCheckStateChanged(
		IPluginModelBase model,
		boolean checked) {

		if (model.getUnderlyingResource() == null) {
			if (checked) {
				numExternalChecked += 1;
			} else {
				numExternalChecked -= 1;
			}
			pluginTreeViewer.setChecked(
				externalPlugins,
				numExternalChecked > 0);
			pluginTreeViewer.setGrayed(
				externalPlugins,
				numExternalChecked > 0
					&& numExternalChecked < externalModels.length);
		} else {
			if (checked) {
				numWorkspaceChecked += 1;
			} else {
				numWorkspaceChecked -= 1;
			}
			pluginTreeViewer.setChecked(
				workspacePlugins,
				numWorkspaceChecked > 0);
			pluginTreeViewer.setGrayed(
				workspacePlugins,
				numWorkspaceChecked > 0
					&& numWorkspaceChecked < workspaceModels.length);
		}

		setChanged(true);
	}

	private void handleGroupStateChanged(Object group, boolean checked) {
		pluginTreeViewer.setSubtreeChecked(group, checked);
		pluginTreeViewer.setGrayed(group, false);

		if (group == workspacePlugins)
			numWorkspaceChecked = checked ? workspaceModels.length : 0;
		else if (group == externalPlugins)
			numExternalChecked = checked ? externalModels.length : 0;

		setChanged(true);
	}

	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(USECUSTOM, true);
		config.setAttribute(USEFEATURES, false);
	}

	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		if (isChanged()) {
			if (!check) {
				configuration.setAttribute(
					USECUSTOM,
					useDefaultRadio.getSelection());
				configuration.setAttribute(
					USEFEATURES,
					useFeaturesRadio.getSelection());
				setChanged(false);
			} else {
				check = false;
				configuration.setAttribute(EXTPLUGINS, "");
			}
		}
	}

	public void doPerformApply(ILaunchConfigurationWorkingCopy configuration) {
		if (!configuration.isDirty())
			return;

		final ILaunchConfigurationWorkingCopy config = configuration;
		BusyIndicator.showWhile(Display.getCurrent(), new Runnable() {
			public void run() {
				config.setAttribute(USECUSTOM, useDefaultRadio.getSelection());
				config.setAttribute(
					USEFEATURES,
					useFeaturesRadio.getSelection());

				if (!useListRadio.getSelection()) {
					setChanged(false);
					return;
				}
				// store deselected projects
				StringBuffer wbuf = new StringBuffer();

				for (int i = 0; i < workspaceModels.length; i++) {
					IPluginModelBase model =
						(IPluginModelBase) workspaceModels[i];
					if (!pluginTreeViewer.getChecked(model))
						wbuf.append(
							model.getPluginBase().getId()
								+ File.pathSeparatorChar);
				}

				StringBuffer exbuf = new StringBuffer();

				// Store external state
				for (int i = 0; i < externalModels.length; i++) {
					IPluginModelBase model =
						(IPluginModelBase) externalModels[i];
					exbuf.append(model.getPluginBase().getId());
					exbuf.append(
						pluginTreeViewer.getChecked(model) ? ",t" : ",f");
					exbuf.append(File.pathSeparatorChar);
				}
				config.setAttribute(WSPROJECT, wbuf.toString());
				config.setAttribute(EXTPLUGINS, exbuf.toString());
				setChanged(false);
			}
		});
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
			//SWTUtil.setDialogSize(dialog, 500, 400);
			dialog.getShell().setSize(500, 500);
			dialog.open();
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	private boolean updateStatus() {
		return updateStatus(validatePlugins());
	}

	private IStatus validatePlugins() {
		if (!useFeaturesRadio.getSelection()) {

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
			for (int i = 0; i < plugins.length; i++) {
				IPluginModelBase model = plugins[i];
				if (model.isLoaded() == false) {
					return createStatus(
						IStatus.WARNING,
						PDEPlugin.getResourceString(KEY_ERROR_BROKEN_PLUGINS));
				}
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
		boolean useDefault = useDefaultRadio.getSelection();
		if (useDefault) {
			Hashtable wtable = new Hashtable();
			for (int i = 0; i < workspaceModels.length; i++) {
				// check for null is to accomodate previous unclean exits (e.g. workspace crashes)
				if (workspaceModels[i].getPluginBase().getId() != null) {
					res.add(workspaceModels[i]);
					wtable.put(
						workspaceModels[i].getPluginBase().getId(),
						workspaceModels[i]);
				}
			}
			for (int i = 0; i < externalModels.length; i++) {
				IPluginModelBase model = externalModels[i];
				boolean masked =
					wtable.get(model.getPluginBase().getId()) != null;
				if (!masked && externalModels[i].isEnabled())
					res.add(externalModels[i]);
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