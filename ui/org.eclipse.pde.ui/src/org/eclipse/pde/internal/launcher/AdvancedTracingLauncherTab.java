package org.eclipse.pde.internal.launcher;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.resources.*;
import org.eclipse.pde.model.plugin.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.jface.preference.*;
import org.eclipse.swt.*;
import org.eclipse.ui.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.pde.internal.elements.*;
import org.eclipse.pde.internal.*;
import java.util.*;
import org.eclipse.ui.views.properties.*;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.debug.core.*;
import org.eclipse.swt.events.*;
import org.eclipse.pde.internal.editor.manifest.NullMenuManager;
import org.eclipse.pde.internal.editor.manifest.NullToolBarManager;
import org.eclipse.jface.action.ToolBarManager;

public class AdvancedTracingLauncherTab
	extends AbstractLauncherTab
	implements ILauncherSettings {
	public static final String KEY_DESC = "Preferences.AdvancedTracingPage.desc";
	public static final String KEY_PLUGINS =
		"Preferences.AdvancedTracingPage.plugins";
	public static final String KEY_WORKSPACE_PLUGINS =
		"Preferences.AdvancedTracingPage.workspacePlugins";
	public static final String KEY_EXTERNAL_PLUGINS =
		"Preferences.AdvancedTracingPage.externalPlugins";
	public static final String KEY_OPTIONS =
		"Preferences.AdvancedTracingPage.options";
	private TreeViewer pluginTreeViewer;
	private NamedElement workspacePlugins;
	private NamedElement externalPlugins;
	private Properties masterOptions;
	private Hashtable propertySources = new Hashtable();
	private TracingPropertySource currentSource;
	private Vector externalList;
	private Vector workspaceList;
	private PropertySheetPage propertySheet;
	private SashForm sashForm;
	private Composite tableChild;

	class PluginContentProvider
		extends DefaultContentProvider
		implements ITreeContentProvider {
		public boolean hasChildren(Object parent) {
			if (parent instanceof IPluginModel)
				return false;
			return true;
		}
		public Object[] getChildren(Object parent) {
			if (parent == externalPlugins) {
				return getExternalTraceablePlugins();
			}
			if (parent == workspacePlugins) {
				return getWorkspaceTraceablePlugins();
			}
			return new Object[0];
		}
		public Object getParent(Object child) {
			if (child instanceof IPluginModel) {
				IPluginModel model = (IPluginModel) child;
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

	public AdvancedTracingLauncherTab() {
		//setDescription(PDEPlugin.getResourceString(KEY_DESC));
		PDEPlugin.getDefault().getLabelProvider().connect(this);
	}

	public void createControl(Composite parent) {
		sashForm = new SashForm(parent, SWT.VERTICAL);

		Composite treeChild = new Composite(sashForm, SWT.NULL);
		GridLayout clayout = new GridLayout();
		clayout.marginWidth = 0;
		clayout.marginHeight = 0;
		treeChild.setLayout(clayout);

		Label label = new Label(treeChild, SWT.NULL);
		label.setText(PDEPlugin.getResourceString(KEY_PLUGINS));
		Control c = createPluginList(treeChild);
		GridData gd = new GridData(GridData.FILL_BOTH);
		c.setLayoutData(gd);

		tableChild = new Composite(sashForm, SWT.NULL);
		clayout = new GridLayout();
		//clayout.numColumns = 2;
		clayout.marginWidth = 0;
		clayout.marginHeight = 0;
		tableChild.setLayout(clayout);

		Composite titleBar = new Composite(tableChild, SWT.NULL);
		clayout = new GridLayout();
		clayout.numColumns = 2;
		clayout.marginWidth = clayout.marginHeight = 0;
		titleBar.setLayout(clayout);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		titleBar.setLayoutData(gd);

		label = new Label(titleBar, SWT.NULL);
		label.setText(PDEPlugin.getResourceString(KEY_OPTIONS));
		ToolBar toolbar = new ToolBar(titleBar, SWT.FLAT);
		gd = new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.GRAB_HORIZONTAL);
		//gd.widthHint = 100;
		toolbar.setLayoutData(gd);

		c = createPropertySheet(toolbar, tableChild);
		gd = new GridData(GridData.FILL_BOTH);
		c.setLayoutData(gd);
		titleBar.addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(MouseEvent e) {
				doMaximize(sashForm.getMaximizedControl() == null);
			}
		});

		initialize();
		setControl(sashForm);
	}

	private void doMaximize(boolean maximize) {
		Control maxControl = maximize ? tableChild : null;
		sashForm.setMaximizedControl(maxControl);
	}

	protected Control createPluginList(Composite parent) {
		pluginTreeViewer = new TreeViewer(parent, SWT.BORDER);
		pluginTreeViewer.setContentProvider(new PluginContentProvider());
		pluginTreeViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		pluginTreeViewer.setAutoExpandLevel(3);
		pluginTreeViewer.addFilter(new ViewerFilter() {
			public boolean select(Viewer v, Object parent, Object object) {
				if (object instanceof IPluginModel) {
					return ((IPluginModel) object).isEnabled();
				}
				return true;
			}
		});
		pluginTreeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent e) {
				Object item = ((IStructuredSelection) e.getSelection()).getFirstElement();
				if (item instanceof IPluginModel)
					pluginSelected((IPluginModel) item);
				else
					pluginSelected(null);
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
	protected Control createPropertySheet(
		final ToolBar toolbar,
		Composite parent) {
		Composite composite = new Composite(parent, SWT.BORDER);
		GridLayout layout = new GridLayout();
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		composite.setLayout(layout);
		propertySheet = new PropertySheetPage();
		propertySheet.createControl(composite);
		GridData gd = new GridData(GridData.FILL_BOTH);
		propertySheet.getControl().setLayoutData(gd);
		ToolBarManager manager = new ToolBarManager(toolbar);
		propertySheet.makeContributions(new NullMenuManager(), manager, null);
		manager.update(true);
		return composite;
	}
	public void dispose() {
		propertySheet.dispose();
		super.dispose();
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
	}
	private void fillTraceableModelList(IPluginModel[] models, Vector result) {
		for (int i = 0; i < models.length; i++) {
			IPluginModel model = models[i];
			if (TracingOptionsManager.isTraceable(model))
				result.add(model);
		}
	}
	private IAdaptable getAdaptable(IPluginModel model) {
		if (model == null)
			return null;
		IAdaptable adaptable = (IAdaptable) propertySources.get(model);
		if (adaptable == null) {
			String id = model.getPlugin().getId();
			Hashtable defaults =
				PDEPlugin.getDefault().getTracingOptionsManager().getTemplateTable(id);
			adaptable = new TracingPropertySource(model, masterOptions, defaults);
			propertySources.put(model, adaptable);
		}
		return adaptable;
	}
	private Object[] getExternalTraceablePlugins() {
		if (externalList == null) {
			externalList = new Vector();
			IPluginModel[] models =
				PDEPlugin.getDefault().getExternalModelManager().getModels();
			fillTraceableModelList(models, externalList);
		}
		return externalList.toArray();
	}
	private Object[] getWorkspaceTraceablePlugins() {
		if (workspaceList == null) {
			workspaceList = new Vector();
			IPluginModel[] models =
				PDEPlugin.getDefault().getWorkspaceModelManager().getWorkspacePluginModels();
			fillTraceableModelList(models, workspaceList);
		}
		return workspaceList.toArray();
	}

	private void initialize() {
		pluginTreeViewer.setInput(PDEPlugin.getDefault());
		pluginTreeViewer.reveal(workspacePlugins);
	}

	private void selectPlugin(String pluginId) {
		IPluginModel model = findModel(pluginId, workspaceList);
		if (model == null)
			model = findModel(pluginId, externalList);
		if (model != null)
			pluginTreeViewer.setSelection(new StructuredSelection(model), true);
	}

	private IPluginModel findModel(String id, Vector list) {
		if (list == null)
			return null;
		for (int i = 0; i < list.size(); i++) {
			IPluginModel model = (IPluginModel) list.get(i);
			IPlugin plugin = model.getPlugin();
			if (plugin.getId().equals(id))
				return model;
		}
		return null;
	}

	public void initializeFrom(ILaunchConfiguration config) {
		masterOptions =
			PDEPlugin.getDefault().getTracingOptionsManager().getTracingTemplateCopy();
		try {
			String selectedPlugin = config.getAttribute(SELECTED_PLUGIN, (String) null);
			if (selectedPlugin != null) {
				selectPlugin(selectedPlugin);
			}
			boolean maximized = config.getAttribute(MAXIMIZED, false);
			doMaximize(maximized);
	
			Map options = config.getAttribute(TRACING_OPTIONS, (Map) null);
			if (options!=null)
				initializeFrom(options);
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	private void initializeFrom(Map options) {
		Set keys = options.keySet();
		for (Iterator iter = keys.iterator(); iter.hasNext();) {
			Object key = iter.next();
			if (masterOptions.containsKey(key)) {
				masterOptions.put(key, options.get(key));
			}
		}
	}

	protected void performDefaults() {
		if (currentSource != null) {
			currentSource.reset();
			propertySheet.refresh();
		}
		//super.performDefaults();
	}

	public void performApply(ILaunchConfigurationWorkingCopy config) {
		boolean changes = false;
		for (Enumeration enum = propertySources.elements(); enum.hasMoreElements();) {
			TracingPropertySource source = (TracingPropertySource) enum.nextElement();
			if (source.isModified()) {
				changes = true;
				source.save();
			}
		}
		boolean maximized = sashForm.getMaximizedControl() != null;
		config.setAttribute(MAXIMIZED, maximized);
		IStructuredSelection sel =
			(IStructuredSelection) pluginTreeViewer.getSelection();
		if (!sel.isEmpty()) {
			IPluginModel model = (IPluginModel) sel.getFirstElement();
			IPlugin plugin = model.getPlugin();
			config.setAttribute(SELECTED_PLUGIN, plugin.getId());
		}
		if (!changes)
			return;
		config.setAttribute(TRACING_OPTIONS, masterOptions);
	}

	private void pluginSelected(IPluginModel model) {
		IAdaptable adaptable = getAdaptable(model);
		ISelection selection =
			adaptable != null
				? new StructuredSelection(adaptable)
				: new StructuredSelection();
		propertySheet.selectionChanged(null, selection);
		currentSource = (TracingPropertySource) adaptable;
	}
}