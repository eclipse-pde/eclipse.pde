package org.eclipse.pde.internal.editor.manifest;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.resources.*;
import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.swt.*;
import org.eclipse.ui.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.pde.internal.elements.*;
import org.eclipse.pde.internal.*;
import java.util.*;
import org.eclipse.ui.views.properties.*;
import org.eclipse.pde.internal.builders.*;


public class NewDependencyWizardPage extends WizardPage {
	public static final String KEY_TITLE = "ManifestEditor.ImportListSection.new.title";
	public static final String KEY_DESC = "ManifestEditor.ImportListSection.new.desc";
	public static final String KEY_PLUGINS = "ManifestEditor.ImportListSection.new.label";
	public static final String KEY_WORKSPACE_PLUGINS = "Preferences.AdvancedTracingPage.workspacePlugins";
	public static final String KEY_EXTERNAL_PLUGINS = "Preferences.AdvancedTracingPage.externalPlugins";
	public static final String KEY_LOOP_WARNING = "ManifestEditor.ImportListSection.loopWarning";
	private IPluginModel model;
	private CheckboxTreeViewer pluginTreeViewer;
	private Image pluginImage;
	private Image errorPluginImage;
	private Image pluginsImage;
	private NamedElement workspacePlugins;
	private NamedElement externalPlugins;
	private Vector externalList;
	private Vector workspaceList;
	private Vector candidates = new Vector();

	class PluginLabelProvider extends LabelProvider {
		public String getText(Object obj) {
			if (obj instanceof IPluginModel) {
				IPluginModel model = (IPluginModel) obj;
				return model.getPlugin().getTranslatedName();
				//return name + " " + model.getPlugin().getVersion();
			}
			return obj.toString();
		}
		public Image getImage(Object obj) {
			if (obj instanceof IPluginModel) {
				boolean error = !((IPluginModel)obj).isLoaded();
				return error?errorPluginImage:pluginImage;
			}
			if (obj instanceof NamedElement)
				return ((NamedElement) obj).getImage();
			return null;
		}
	}

	class PluginContentProvider
		extends DefaultContentProvider
		implements ITreeContentProvider {
		public boolean hasChildren(Object parent) {
			if (parent instanceof IPluginModel) return false;
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

public NewDependencyWizardPage(IPluginModel model) {
	super("newDependencyPage");
	this.model = model;
	pluginImage = PDEPluginImages.get(PDEPluginImages.IMG_PLUGIN_OBJ);
	errorPluginImage = PDEPluginImages.get(PDEPluginImages.IMG_ERR_PLUGIN_OBJ);
	pluginsImage = PDEPluginImages.DESC_REQ_PLUGINS_OBJ.createImage();
	setTitle(PDEPlugin.getResourceString(KEY_TITLE));
	setDescription(PDEPlugin.getResourceString(KEY_DESC));
	setPageComplete(false);
}

public void createControl(Composite parent) {
	Composite container = new Composite(parent, SWT.NULL);
	GridLayout layout = new GridLayout();
	//layout.numColumns = 2;
	//layout.makeColumnsEqualWidth=true;
	container.setLayout(layout);

	Label label = new Label(container, SWT.NULL);
	label.setText(PDEPlugin.getResourceString(KEY_PLUGINS));

	Control c = createPluginList(container);
	GridData gd = new GridData(GridData.FILL_BOTH);
	c.setLayoutData(gd);

	initialize();
	setControl(container);
}

protected Control createPluginList(Composite parent) {
	pluginTreeViewer = new CheckboxTreeViewer(parent, SWT.BORDER);
	pluginTreeViewer.setContentProvider(new PluginContentProvider());
	pluginTreeViewer.setLabelProvider(new PluginLabelProvider());
	pluginTreeViewer.setAutoExpandLevel(2);
	pluginTreeViewer.addFilter(new ViewerFilter () {
		public boolean select(Viewer v, Object parent, Object object) {
			if (object instanceof IPluginModel) {
				IPluginModel model = (IPluginModel)object;
				boolean include = model.isEnabled();
				if (include) {
					include = !isOnTheList(model);
				}
				return include;
			}
			return true;
		}
	});
	pluginTreeViewer.addCheckStateListener(new ICheckStateListener () {
		public void checkStateChanged(CheckStateChangedEvent event) {
			Object element = event.getElement();
			if (element instanceof IPluginModel) {
				IPluginModel model = (IPluginModel)event.getElement();
				handleCheckStateChanged(model, event.getChecked());
			}
			else {
				pluginTreeViewer.setChecked(element, false);
			}
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
	workspacePlugins = new NamedElement(PDEPlugin.getResourceString(KEY_WORKSPACE_PLUGINS), pluginsImage);
	externalPlugins = new NamedElement(PDEPlugin.getResourceString(KEY_EXTERNAL_PLUGINS), pluginsImage);
	return pluginTreeViewer.getTree();
}

public void dispose() {
	pluginsImage.dispose();
	super.dispose();
}

private boolean isOnTheList(IPluginModel candidate) {
	IPlugin plugin = candidate.getPlugin();
	IPluginImport [] imports = model.getPlugin().getImports();
	
	for (int i=0; i<imports.length; i++) {
		IPluginImport iimport = imports[i];
		if (iimport.getId().equals(plugin.getId()))
		   return true;
	}
	return false;
}

private Object[] getExternalPlugins() {
	return PDEPlugin.getDefault().getExternalModelManager().getModels();
}

private Object[] getWorkspacePlugins() {
	return PDEPlugin.getDefault().getWorkspaceModelManager().getWorkspacePluginModels();
}

public void init(IWorkbench workbench) {}

private void initialize() {
	pluginTreeViewer.setInput(PDEPlugin.getDefault());
	pluginTreeViewer.setGrayed(workspacePlugins, true);
	pluginTreeViewer.setGrayed(externalPlugins, true);
	pluginTreeViewer.reveal(workspacePlugins);
}

private void pluginSelected(IPluginModel model) {
}

private void handleCheckStateChanged(IPluginModel candidate, boolean checked) {
	if (checked) candidates.add(candidate);
	else candidates.remove(candidate);
	setPageComplete(candidates.size()>0);
	if (candidates.size()>0) {
		IPlugin [] plugins = new IPlugin[candidates.size()];
		for (int i=0; i<candidates.size(); i++) {
			plugins[i] = ((IPluginModel)candidates.get(i)).getPlugin();
		}
		DependencyLoop [] loops = DependencyLoopFinder.findLoops(model.getPlugin(), plugins, true);
		if (loops.length>0) {
			setMessage(PDEPlugin.getResourceString(KEY_LOOP_WARNING));
		}
		else
			setMessage(null);
	}
	else
		setMessage(null);
}

public boolean finish() {
	IPlugin plugin = model.getPlugin();
	try {
		for (int i=0; i<candidates.size(); i++) {
			IPluginModel candidate = (IPluginModel)candidates.get(i);
			IPluginImport importNode = model.getFactory().createImport();
			importNode.setId(candidate.getPlugin().getId());
			plugin.add(importNode);
		}
	}
	catch (CoreException e) {
		PDEPlugin.logException(e);
		return false;
	}
	return true;
}

}