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
import org.eclipse.pde.internal.parts.WizardCheckboxTablePart;

public class PluginListPage extends WizardPage {
	public static final String PAGE_TITLE = "NewFeatureWizard.PlugPage.title";
	public static final String PAGE_DESC = "NewFeatureWizard.PlugPage.desc";
	private WizardCheckboxTablePart tablePart;
	private Image pluginImage;
	private Image fragmentImage;
	private IPluginModelBase [] models;

	class PluginContentProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			return getPluginModels();
		}
	}

	class PluginLabelProvider
		extends LabelProvider
		implements ITableLabelProvider {
		public String getColumnText(Object obj, int index) {
			if (index == 0) {
				IPluginModelBase model = (IPluginModelBase) obj;
				IPluginBase base = model.getPluginBase();
				String label = model.getResourceString(base.getName());
				return label + " (" + base.getVersion() + ")";
			}
			return "";
		}
		public Image getColumnImage(Object obj, int index) {
			IPluginModelBase model = (IPluginModelBase) obj;
			return model.isFragmentModel() ? fragmentImage : pluginImage;
		}
	}

	public PluginListPage() {
		super("pluginListPage");
		pluginImage = PDEPluginImages.get(PDEPluginImages.IMG_PLUGIN_OBJ);
		fragmentImage = PDEPluginImages.get(PDEPluginImages.IMG_FRAGMENT_OBJ);
		setTitle(PDEPlugin.getResourceString(PAGE_TITLE));
		setDescription(PDEPlugin.getResourceString(PAGE_DESC));
		tablePart = new WizardCheckboxTablePart(null);
	}

	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.verticalSpacing = 9;
		container.setLayout(layout);

		tablePart.createControl(container);
		CheckboxTableViewer pluginViewer = tablePart.getTableViewer();
		pluginViewer.setContentProvider(new PluginContentProvider());
		pluginViewer.setLabelProvider(new PluginLabelProvider());
		GridData gd = (GridData) tablePart.getControl().getLayoutData();
		gd.heightHint = 250;
		pluginViewer.setInput(PDEPlugin.getDefault().getWorkspaceModelManager());
		pluginViewer.getTable().setFocus();
		setControl(container);
	}

	private Object[] getPluginModels() {
		if (models == null) {
			WorkspaceModelManager manager =
				PDEPlugin.getDefault().getWorkspaceModelManager();
			IPluginModel[] workspaceModels = manager.getWorkspacePluginModels();
			IFragmentModel[] fragmentModels = manager.getWorkspaceFragmentModels();
			models =
				new IPluginModelBase[workspaceModels.length + fragmentModels.length];
			System.arraycopy(workspaceModels, 0, models, 0, workspaceModels.length);
			System.arraycopy(fragmentModels, 0, models, workspaceModels.length, fragmentModels.length);
		}
		return models;
	}

	public IPluginBase[] getSelectedPlugins() {
		Object[] result = tablePart.getSelection();
		IPluginBase[] plugins = new IPluginBase[result.length];
		for (int i=0; i<result.length; i++) {
			IPluginModelBase model = (IPluginModelBase)result[i];
			plugins[i] = model.getPluginBase();
		}
		return plugins;
	}
}