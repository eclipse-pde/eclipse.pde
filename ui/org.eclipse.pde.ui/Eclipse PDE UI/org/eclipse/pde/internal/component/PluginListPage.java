package org.eclipse.pde.internal.component;

import java.util.*;
import org.eclipse.pde.internal.*;
import org.eclipse.swt.layout.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.swt.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.pde.internal.base.model.plugin.*;
import org.eclipse.pde.internal.elements.*;
import org.eclipse.pde.internal.wizards.*;


public class PluginListPage extends WizardPage {
	public static final String PAGE_TITLE = "NewComponentWizard.PlugPage.title";
	public static final String PAGE_DESC = "NewComponentWizard.PlugPage.desc";
	private CheckboxTableViewer pluginViewer;
	private Image pluginImage;
	private PluginReference [] references;
	
	public class PluginReference {
		boolean checked;
		IPluginModel model;
		public String toString() {
			return model.getPlugin().getName();
		}
	}

	class PluginContentProvider extends DefaultContentProvider implements IStructuredContentProvider {
		public Object [] getElements(Object parent) {
			return createPluginReferences();
		}
	}

	class PluginLabelProvider extends LabelProvider implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			if (index==0) {
				return obj.toString();
			}
			return "";
		}
		public String getColumnText(Viewer v, Object obj, int index) {
			return getColumnText(obj, index);
		}
		public Image getColumnImage(Object obj, int index) {
			return pluginImage;
		}
		public Image getColumnImage(Viewer v, Object obj, int index) {
			return getColumnImage(obj, index);
		}
	}

public PluginListPage() {
	super("pluginListPage");
	pluginImage = PDEPluginImages.DESC_PLUGIN_OBJ.createImage();
	setTitle(PDEPlugin.getResourceString(PAGE_TITLE));
	setDescription(PDEPlugin.getResourceString(PAGE_DESC));
}
public void createControl(Composite parent) {
	Composite container = new Composite(parent, SWT.NULL);
	GridLayout layout = new GridLayout();
	layout.verticalSpacing = 9;
	container.setLayout(layout);

	pluginViewer = new CheckboxTableViewer(container, SWT.BORDER);
	pluginViewer.setContentProvider(new PluginContentProvider());
	pluginViewer.setLabelProvider(new PluginLabelProvider());
	pluginViewer.setSorter(ListUtil.NAME_SORTER);

	pluginViewer.addCheckStateListener(new ICheckStateListener() {
		public void checkStateChanged(CheckStateChangedEvent e) {
			handlePluginChecked((PluginReference) e.getElement(), e.getChecked());
		}
	});
	GridData gd = new GridData(GridData.FILL_BOTH);
	pluginViewer.getTable().setLayoutData(gd);
	pluginViewer.setInput(PDEPlugin.getDefault().getWorkspaceModelManager());
	pluginViewer.getTable().setFocus();
	setControl(container);
}
private Object[] createPluginReferences() {
	if (references == null) {
		WorkspaceModelManager manager = PDEPlugin.getDefault().getWorkspaceModelManager();
		IPluginModel[] workspaceModels = manager.getWorkspacePluginModels();
		references = new PluginReference[workspaceModels.length];
		for (int i = 0; i < workspaceModels.length; i++) {
			IPluginModel model = workspaceModels[i];
			PluginReference reference = new PluginReference();
			reference.model = model;
			references[i] = reference;

		}
	}
	return references;
}
public void dispose() {
	pluginImage.dispose();
	super.dispose();
}
public IPlugin[] getSelectedPlugins() {
	Vector result = new Vector();
	if (references!=null && references.length>0) {
		for (int i=0; i<references.length; i++) {
			if (references[i].checked) {
				IPlugin plugin = references[i].model.getPlugin();
				result.add(plugin);
			}
		}
	}
	IPlugin [] plugins = new IPlugin[result.size()];
	result.copyInto(plugins);
	return plugins;
}
private void handlePluginChecked(PluginReference reference, boolean checked) {
	reference.checked = checked;
}
}
