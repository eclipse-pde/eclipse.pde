package org.eclipse.pde.internal.ui.wizards.imports;

import java.util.ArrayList;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.plugin.IFragment;
import org.eclipse.pde.core.plugin.IFragmentModel;
import org.eclipse.pde.core.plugin.IPlugin;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.core.plugin.IPluginModel;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.ui.PDEPlugin;

public abstract class BaseImportWizardSecondPage extends WizardPage {
	
	protected PluginImportWizardFirstPage page1;
	protected IPluginModelBase[] models = new IPluginModelBase[0];
	protected ArrayList selected = new ArrayList();
	private String location;

	public BaseImportWizardSecondPage(String pageName, PluginImportWizardFirstPage page) {
		super(pageName);
		this.page1 = page;
		PDEPlugin.getDefault().getLabelProvider().connect(this);
	}


	public void dispose() {
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
	}
	
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible && isRefreshNeeded()) {
			models = page1.getModels();
			refreshPage();
		}
	}

	protected void refreshPage() {
	}


	protected boolean isRefreshNeeded() {
		String currLocation = page1.getDropLocation();
		if (location == null || !location.equals(currLocation)) {
			location = page1.getDropLocation();
			return true;
		}
		return false;	
	}
	
	private IPluginModelBase findModel(String id) {
		for (int i = 0; i < models.length; i++) {
			if (models[i].getPluginBase().getId().equals(id))
				return models[i];
		}
		return null;
	}

	private IFragmentModel[] findFragments(IPlugin plugin) {
		ArrayList result = new ArrayList();
		for (int i = 0; i < models.length; i++) {
			if (models[i] instanceof IFragmentModel) {
				IFragment fragment = ((IFragmentModel) models[i]).getFragment();
				if (plugin.getId().equalsIgnoreCase(fragment.getPluginId())) {
					result.add(models[i]);
				}
			}
		}
		return (IFragmentModel[]) result.toArray(new IFragmentModel[result.size()]);
	}

	protected void addPluginAndDependencies(IPluginModelBase model) {
		addPluginAndDependencies(model, true);
	}

	private void addPluginAndDependencies(
		IPluginModelBase model,
		boolean addFragmentPlugin) {
			
		if (!selected.contains(model)) {
			selected.add(model);
			addDependencies(model, addFragmentPlugin);
		}
	}
	
	protected void addDependencies(IPluginModelBase model, boolean addFragmentPlugin) {
		if (model instanceof IPluginModel) {
			IPlugin plugin = ((IPluginModel) model).getPlugin();
			IPluginImport[] required = plugin.getImports();
			if (required.length > 0) {
				for (int i = 0; i < required.length; i++) {
					IPluginModelBase found = findModel(required[i].getId());
					if (found != null) {
						addPluginAndDependencies(found);
					}
				}
			}
			IFragmentModel[] fragments = findFragments(plugin);
			for (int i = 0; i < fragments.length; i++) {
				addPluginAndDependencies(fragments[i], false);
			}
		}
		if (addFragmentPlugin && model instanceof IFragmentModel) {
			IFragment fragment = ((IFragmentModel) model).getFragment();
			IPluginModelBase found = findModel(fragment.getPluginId());
			if (found != null) {
				addPluginAndDependencies(found);
			}
		}
		
	}
	
	public IPluginModelBase[] getModelsToImport() {
		return (IPluginModelBase[]) selected.toArray(new IPluginModelBase[selected.size()]);
	}

}
