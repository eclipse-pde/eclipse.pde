package org.eclipse.pde.internal.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

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
	public static final String PAGE_TITLE = "NewFeatureWizard.PlugPage.title";
	public static final String PAGE_DESC = "NewFeatureWizard.PlugPage.desc";
	private CheckboxTableViewer pluginViewer;
	private Image pluginImage;
	private Image fragmentImage;
	private PluginReference [] references;
	
	public class PluginReference {
		boolean checked;
		IPluginModelBase model;
		
		public boolean isFragment() {
			return model instanceof IFragmentModel;
		}
		public String toString() {
			IPluginBase base = model.getPluginBase();
			String label = model.getResourceString(base.getName());
			return label + " ("+base.getVersion()+")";
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
		public Image getColumnImage(Object obj, int index) {
			boolean fragment = ((PluginReference)obj).isFragment();
			return fragment ? fragmentImage : pluginImage;
		}
	}

public PluginListPage() {
	super("pluginListPage");
	pluginImage = PDEPluginImages.get(PDEPluginImages.IMG_PLUGIN_OBJ);
	fragmentImage = PDEPluginImages.get(PDEPluginImages.IMG_FRAGMENT_OBJ);
	setTitle(PDEPlugin.getResourceString(PAGE_TITLE));
	setDescription(PDEPlugin.getResourceString(PAGE_DESC));
}
public void createControl(Composite parent) {
	Composite container = new Composite(parent, SWT.NULL);
	GridLayout layout = new GridLayout();
	layout.verticalSpacing = 9;
	container.setLayout(layout);

	pluginViewer = CheckboxTableViewer.newCheckList(container, SWT.BORDER);
	pluginViewer.setContentProvider(new PluginContentProvider());
	pluginViewer.setLabelProvider(new PluginLabelProvider());
	pluginViewer.setSorter(ListUtil.NAME_SORTER);

	pluginViewer.addCheckStateListener(new ICheckStateListener() {
		public void checkStateChanged(CheckStateChangedEvent e) {
			handlePluginChecked((PluginReference) e.getElement(), e.getChecked());
		}
	});
	GridData gd = new GridData(GridData.FILL_BOTH);
	gd.heightHint = 250;
	pluginViewer.getTable().setLayoutData(gd);
	pluginViewer.setInput(PDEPlugin.getDefault().getWorkspaceModelManager());
	pluginViewer.getTable().setFocus();
	setControl(container);
}
private Object[] createPluginReferences() {
	if (references == null) {
		WorkspaceModelManager manager = PDEPlugin.getDefault().getWorkspaceModelManager();
		IPluginModel[] workspaceModels = manager.getWorkspacePluginModels();
		IFragmentModel [] fragmentModels = manager.getWorkspaceFragmentModels();
		references = new PluginReference[workspaceModels.length+fragmentModels.length];
		for (int i = 0; i < workspaceModels.length; i++) {
			IPluginModel model = workspaceModels[i];
			PluginReference reference = new PluginReference();
			reference.model = model;
			references[i] = reference;
		}
		int offset = workspaceModels.length;
		for (int i=0; i<fragmentModels.length; i++) {
			IFragmentModel model = fragmentModels[i];
			PluginReference reference = new PluginReference();
			reference.model = model;
			references[i+offset] = reference;
		}
	}
	return references;
}

public IPluginBase[] getSelectedPlugins() {
	Vector result = new Vector();
	if (references!=null && references.length>0) {
		for (int i=0; i<references.length; i++) {
			if (references[i].checked) {
				IPluginBase plugin = references[i].model.getPluginBase();
				result.add(plugin);
			}
		}
	}
	IPluginBase [] plugins = new IPluginBase[result.size()];
	result.copyInto(plugins);
	return plugins;
}
private void handlePluginChecked(PluginReference reference, boolean checked) {
	reference.checked = checked;
}
}
