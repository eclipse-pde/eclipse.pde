package org.eclipse.pde.internal.ui.launcher;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.resources.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.jface.preference.*;
import org.eclipse.swt.*;
import org.eclipse.ui.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.pde.internal.ui.elements.*;
import org.eclipse.pde.internal.ui.*;
import java.util.*;
import org.eclipse.ui.views.properties.*;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.debug.core.*;
import org.eclipse.swt.events.*;
import org.eclipse.pde.internal.ui.editor.manifest.NullMenuManager;
import org.eclipse.pde.internal.ui.editor.manifest.NullToolBarManager;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.pde.internal.core.*;

public class TracingLauncherTab
	extends AbstractLauncherTab
	implements ILauncherSettings {
	public static final String KEY_DESC = "TracingLauncherTab.desc";
	private static final String KEY_TRACING = "TracingLauncherTab.tracing";
	public static final String KEY_PLUGINS = "TracingLauncherTab.plugins";
	public static final String KEY_WORKSPACE_PLUGINS =
		"TracingLauncherTab.workspacePlugins";
	public static final String KEY_EXTERNAL_PLUGINS =
		"TracingLauncherTab.externalPlugins";
	public static final String KEY_OPTIONS = "TracingLauncherTab.options";
	public static final String KEY_MAXIMIZE = "TracingLauncherTab.maximize";
	public static final String KEY_RESTORE = "TracingLauncherTab.restore";

	private static final String S_SELECTED_PLUGIN = "selectedPlugin";
	private static final String S_MAXIMIZED = "maximized";
	private Button tracingCheck;
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
	private Label propertyLabel;
	private ToolItem maximizeItem;

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

	public TracingLauncherTab() {
		//setDescription(PDEPlugin.getResourceString(KEY_DESC));
		PDEPlugin.getDefault().getLabelProvider().connect(this);
	}

	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		container.setLayout(layout);

		createStartingSpace(container, 1);

		tracingCheck = new Button(container, SWT.CHECK);
		tracingCheck.setText(PDEPlugin.getResourceString(KEY_TRACING));
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		tracingCheck.setLayoutData(gd);
		tracingCheck.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				tracingCheckChanged();
			}
		});

		Label separator = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		separator.setLayoutData(gd);

		sashForm = new SashForm(container, SWT.VERTICAL);
		gd = new GridData(GridData.FILL_BOTH);
		sashForm.setLayoutData(gd);

		Composite treeChild = new Composite(sashForm, SWT.NULL);
		GridLayout clayout = new GridLayout();
		clayout.marginWidth = 0;
		clayout.marginHeight = 0;
		treeChild.setLayout(clayout);

		Label label = new Label(treeChild, SWT.NULL);
		label.setText(PDEPlugin.getResourceString(KEY_PLUGINS));
		Control c = createPluginList(treeChild);
		gd = new GridData(GridData.FILL_BOTH);
		c.setLayoutData(gd);

		tableChild = new Composite(sashForm, SWT.NULL);
		clayout = new GridLayout();
		//clayout.numColumns = 2;
		clayout.marginWidth = 0;
		clayout.marginHeight = 0;
		clayout.verticalSpacing = 2;
		tableChild.setLayout(clayout);

		Composite titleBar = new Composite(tableChild, SWT.NULL);
		clayout = new GridLayout();
		clayout.numColumns = 2;
		clayout.marginWidth = clayout.marginHeight = 0;
		titleBar.setLayout(clayout);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		titleBar.setLayoutData(gd);

		propertyLabel = new Label(titleBar, SWT.NULL);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		propertyLabel.setLayoutData(gd);
		updatePropertyLabel(null);
		ToolBar toolbar = new ToolBar(titleBar, SWT.FLAT);
		//gd = new GridData(GridData.HORIZONTAL_ALIGN_END | GridData.GRAB_HORIZONTAL);
		//toolbar.setLayoutData(gd);

		maximizeItem = new ToolItem(toolbar, SWT.PUSH);
		maximizeItem.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				toggleMaximize();
			}
		});
		updateMaximizeItem();

		c = createPropertySheet(toolbar, tableChild);
		gd = new GridData(GridData.FILL_BOTH);
		c.setLayoutData(gd);
		propertyLabel.addMouseListener(new MouseAdapter() {
			public void mouseDoubleClick(MouseEvent e) {
				toggleMaximize();
			}
		});
		initialize();
		setControl(container);
	}

	private void toggleMaximize() {
		doMaximize(sashForm.getMaximizedControl() == null);
	}

	private void doMaximize(boolean maximize) {
		Control maxControl = maximize ? tableChild : null;
		sashForm.setMaximizedControl(maxControl);
		updateMaximizeItem();
	}

	private void updateMaximizeItem() {
		boolean maximized = sashForm.getMaximizedControl() != null;
		Image image;
		String tooltip;

		if (maximized) {
			image =
				PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_RESTORE);
			tooltip = PDEPlugin.getResourceString(KEY_RESTORE);
		} else {
			image =
				PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_MAXIMIZE);
			tooltip = PDEPlugin.getResourceString(KEY_MAXIMIZE);
		}
		maximizeItem.setImage(image);
		maximizeItem.setToolTipText(tooltip);
		maximizeItem.getParent().redraw();
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
		//ToolBarManager manager = new ToolBarManager(toolbar);
		propertySheet.makeContributions(
			new NullMenuManager(),
			new NullToolBarManager(),
			null);
		//manager.update(true);
		return composite;
	}
	public void dispose() {
		if (propertySheet != null)
			propertySheet.dispose();
		super.dispose();
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
	}

	private IDialogSettings getDialogSettings() {
		IDialogSettings master = PDEPlugin.getDefault().getDialogSettings();
		String sectionId = "tracingLauncherTab";
		IDialogSettings section = master.getSection(sectionId);
		if (section == null) {
			section = master.addNewSection(sectionId);
		}
		return section;
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
				PDECore.getDefault().getTracingOptionsManager().getTemplateTable(id);
			adaptable = new TracingPropertySource(model, masterOptions, defaults);
			propertySources.put(model, adaptable);
		}
		return adaptable;
	}
	private Object[] getExternalTraceablePlugins() {
		if (externalList == null) {
			externalList = new Vector();
			IPluginModel[] models =
				PDECore.getDefault().getExternalModelManager().getModels();
			fillTraceableModelList(models, externalList);
		}
		return externalList.toArray();
	}
	private Object[] getWorkspaceTraceablePlugins() {
		if (workspaceList == null) {
			workspaceList = new Vector();
			IPluginModel[] models =
				PDECore.getDefault().getWorkspaceModelManager().getWorkspacePluginModels();
			fillTraceableModelList(models, workspaceList);
		}
		return workspaceList.toArray();
	}

	private void initialize() {
		pluginTreeViewer.setInput(PDEPlugin.getDefault());
		pluginTreeViewer.reveal(workspacePlugins);
	}

	private void tracingCheckChanged() {
		boolean enabled = tracingCheck.getSelection();
		pluginTreeViewer.getTree().setEnabled(enabled);
		propertySheet.getControl().setEnabled(enabled);
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

	public void initializeFrom(final ILaunchConfiguration config) {
		BusyIndicator.showWhile(getControl().getDisplay(), new Runnable() {
			public void run() {
				doInitializeFrom(config);
			}
		});
	}

	private void doInitializeFrom(ILaunchConfiguration config) {
		masterOptions =
			PDECore.getDefault().getTracingOptionsManager().getTracingTemplateCopy();
		propertySources.clear();
		try {
			boolean tracing = false;
			tracing = config.getAttribute(TRACING, tracing);
			tracingCheck.setSelection(tracing);
			tracingCheckChanged();

			Map options = config.getAttribute(TRACING_OPTIONS, (Map) null);
			if (options != null)
				initializeFrom(options);

			IDialogSettings settings = getDialogSettings();
			String selectedPlugin = settings.get(S_SELECTED_PLUGIN);
			if (selectedPlugin != null && selectedPlugin.length() > 0) {
				selectPlugin(selectedPlugin);
			}
			boolean maximized = settings.getBoolean(S_MAXIMIZED);
			doMaximize(maximized);

		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	public void performApply(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(TRACING, isTracingEnabled());
		boolean changes = false;
		for (Enumeration enum = propertySources.elements(); enum.hasMoreElements();) {
			TracingPropertySource source = (TracingPropertySource) enum.nextElement();
			if (source.isModified()) {
				changes = true;
				source.save();
			}
		}
		boolean maximized = sashForm.getMaximizedControl() != null;
		IDialogSettings settings = getDialogSettings();
		settings.put(S_MAXIMIZED, maximized);
		IStructuredSelection sel =
			(IStructuredSelection) pluginTreeViewer.getSelection();
		if (!sel.isEmpty()) {
			IPluginModel model = (IPluginModel) sel.getFirstElement();
			IPlugin plugin = model.getPlugin();
			settings.put(S_SELECTED_PLUGIN, plugin.getId());
		}
		if (changes)
			config.setAttribute(TRACING_OPTIONS, masterOptions);
	}

	private void initializeFrom(Map options) {
		masterOptions.putAll(options);
	}

	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		//Properties template = PDEPlugin.getDefault().getTracingOptionsManager().getTracingTemplateCopy();
		//config.setAttribute(TRACING_OPTIONS, template);
		config.setAttribute(TRACING, false);
	}

	public boolean isTracingEnabled() {
		return tracingCheck.getSelection();
	}

	private void updatePropertyLabel(IPluginModel model) {
		String text;
		if (model == null) {
			text = PDEPlugin.getResourceString(KEY_OPTIONS);
		} else {
			text = PDEPlugin.getDefault().getLabelProvider().getText(model);
		}
		propertyLabel.setText(text);
	}

	private void pluginSelected(IPluginModel model) {
		IAdaptable adaptable = getAdaptable(model);
		ISelection selection =
			adaptable != null
				? new StructuredSelection(adaptable)
				: new StructuredSelection();
		propertySheet.selectionChanged(null, selection);
		currentSource = (TracingPropertySource) adaptable;
		updatePropertyLabel(model);
	}
	public String getName() {
		return "&Tracing";
	}
}