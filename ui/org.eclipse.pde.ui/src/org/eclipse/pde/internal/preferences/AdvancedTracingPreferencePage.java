package org.eclipse.pde.internal.preferences;
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
import org.eclipse.pde.internal.editor.manifest.NullMenuManager;
import org.eclipse.pde.internal.editor.manifest.NullToolBarManager;

public class AdvancedTracingPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
	public static final String KEY_DESC = "Preferences.AdvancedTracingPage.desc";
	public static final String KEY_PLUGINS = "Preferences.AdvancedTracingPage.plugins";
	public static final String KEY_WORKSPACE_PLUGINS = "Preferences.AdvancedTracingPage.workspacePlugins";
	public static final String KEY_EXTERNAL_PLUGINS = "Preferences.AdvancedTracingPage.externalPlugins";
	public static final String KEY_OPTIONS = "Preferences.AdvancedTracingPage.options";
	private TreeViewer pluginTreeViewer;
	private NamedElement workspacePlugins;
	private Properties masterOptions;
	private NamedElement externalPlugins;
	private Hashtable propertySources=new Hashtable();
	private TracingPropertySource currentSource;
	private Vector externalList;
	private Vector workspaceList;
	private boolean wasHidden=false;
	private PropertySheetPage propertySheet;
	
	class PluginContentProvider
		extends DefaultContentProvider
		implements ITreeContentProvider {
		public boolean hasChildren(Object parent) {
			if (parent instanceof IPluginModel) return false;
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

public AdvancedTracingPreferencePage() {
	setDescription(PDEPlugin.getResourceString(KEY_DESC));
	PDEPlugin.getDefault().getLabelProvider().connect(this);
}
protected Control createContents(Composite parent) {
	Composite container = new Composite(parent, SWT.NULL);
	GridLayout layout = new GridLayout();
	layout.numColumns = 2;
	//layout.makeColumnsEqualWidth=true;
	container.setLayout(layout);

	Label label = new Label(container, SWT.NULL);
	label.setText(PDEPlugin.getResourceString(KEY_PLUGINS));
	label = new Label(container, SWT.NULL);
	label.setText(PDEPlugin.getResourceString(KEY_OPTIONS));

	Control c = createPluginList(container);
	GridData gd = new GridData(GridData.FILL_BOTH);
	gd.widthHint = 150;
	c.setLayoutData(gd);

	c = createPropertySheet(container);
	gd = new GridData(GridData.FILL_BOTH);
	gd.widthHint = 200;
	c.setLayoutData(gd);
	initialize();
	return container;
}
protected Control createPluginList(Composite parent) {
	pluginTreeViewer = new TreeViewer(parent, SWT.BORDER);
	pluginTreeViewer.setContentProvider(new PluginContentProvider());
	pluginTreeViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
	pluginTreeViewer.addFilter(new ViewerFilter () {
		public boolean select(Viewer v, Object parent, Object object) {
			if (object instanceof IPluginModel) {
				return ((IPluginModel)object).isEnabled();
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
	Image pluginsImage = PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_REQ_PLUGINS_OBJ);
	workspacePlugins = new NamedElement(PDEPlugin.getResourceString(KEY_WORKSPACE_PLUGINS), pluginsImage);
	externalPlugins = new NamedElement(PDEPlugin.getResourceString(KEY_EXTERNAL_PLUGINS), pluginsImage);
	return pluginTreeViewer.getTree();
}
protected Control createPropertySheet(Composite parent) {
	Composite composite = new Composite(parent, SWT.BORDER);
	GridLayout layout = new GridLayout();
	layout.marginWidth = 0;
	layout.marginHeight = 0;
	composite.setLayout(layout);
	propertySheet = new PropertySheetPage();
	propertySheet.createControl(composite);
	GridData gd = new GridData(GridData.FILL_BOTH);
	propertySheet.getControl().setLayoutData(gd);
	propertySheet.makeContributions(new NullMenuManager(), new NullToolBarManager(), null);
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
	if (externalList==null) {
		externalList = new Vector();
		IPluginModel [] models = PDEPlugin.getDefault().getExternalModelManager().getModels();
		fillTraceableModelList(models, externalList);
	}
	return externalList.toArray();
}
private Object[] getWorkspaceTraceablePlugins() {
	if (workspaceList==null) {
		workspaceList = new Vector();
		IPluginModel [] models = PDEPlugin.getDefault().getWorkspaceModelManager().getWorkspacePluginModels();
		fillTraceableModelList(models, workspaceList);
	}
	return workspaceList.toArray();
}
public void init(IWorkbench workbench) {}
private void initialize() {
	masterOptions = PDEPlugin.getDefault().getTracingOptionsManager().getTracingOptions();
	pluginTreeViewer.setInput(PDEPlugin.getDefault());
	pluginTreeViewer.reveal(workspacePlugins);
}
protected void performDefaults() {
	if (currentSource != null) {
		currentSource.reset();
		propertySheet.refresh();
	}
	super.performDefaults();
}
public boolean performOk() {
	for (Enumeration enum = propertySources.elements(); enum.hasMoreElements();) {
		TracingPropertySource source = (TracingPropertySource)enum.nextElement();
		source.save();
	}
	TracingOptionsManager mng = PDEPlugin.getDefault().getTracingOptionsManager();
	mng.setTracingOptions(masterOptions);
	mng.save();
	return true;
}
private void pluginSelected(IPluginModel model) {
	IAdaptable adaptable = getAdaptable(model);
	ISelection selection =
		adaptable != null
			? new StructuredSelection(adaptable)
			: new StructuredSelection();
	propertySheet.selectionChanged(null, selection);
	currentSource = (TracingPropertySource)adaptable;
}
public void setVisible(boolean visible) {
	if (visible) {
		if (wasHidden) {
			updateValues();
		}
		wasHidden = false;
	} else {
		wasHidden = true;
	}
	super.setVisible(visible);
}
private void updateValues() {
	initialize();
}
}
