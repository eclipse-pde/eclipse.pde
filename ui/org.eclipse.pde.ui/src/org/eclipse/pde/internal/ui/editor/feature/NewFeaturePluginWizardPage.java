package org.eclipse.pde.internal.ui.editor.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.feature.FeaturePlugin;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.ifeature.IFeaturePlugin;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.help.WorkbenchHelp;

public class NewFeaturePluginWizardPage extends ReferenceWizardPage {
	public static final String KEY_TITLE = "FeatureEditor.PluginSection.new.title";
	public static final String KEY_DESC = "FeatureEditor.PluginSection.new.desc";
	public static final String KEY_ADDING = "FeatureEditor.PluginSection.new.adding";
	public static final String KEY_UPDATING =
		"FeatureEditor.PluginSection.new.updating";

	public NewFeaturePluginWizardPage(IFeatureModel model) {
		super(model, false);
		setTitle(PDEPlugin.getResourceString(KEY_TITLE));
		setDescription(PDEPlugin.getResourceString(KEY_DESC));
	}
	
	protected boolean isOnTheList(IPluginModelBase candidate) {
		IPluginBase plugin = candidate.getPluginBase();
		IFeaturePlugin[] fplugins = model.getFeature().getPlugins();

		for (int i = 0; i < fplugins.length; i++) {
			IFeaturePlugin fplugin = fplugins[i];
			if (fplugin.getId().equals(plugin.getId()))
				return true;
		}
		return false;
	}

	protected void doAdd(Object [] candidates, IProgressMonitor monitor) throws CoreException {
		monitor.beginTask(
			PDEPlugin.getResourceString(KEY_ADDING),
			candidates.length + 1);
		IFeature feature = model.getFeature();
		IFeaturePlugin[] added = new IFeaturePlugin[candidates.length];
		for (int i = 0; i < candidates.length; i++) {
			IPluginModelBase candidate = (IPluginModelBase) candidates[i];
			monitor.subTask(candidate.getPluginBase().getTranslatedName());
			FeaturePlugin fplugin = (FeaturePlugin) model.getFactory().createPlugin();
			fplugin.loadFrom(candidate.getPluginBase());
			added[i] = fplugin;
			monitor.worked(1);
		}
		monitor.subTask("");
		monitor.setTaskName(PDEPlugin.getResourceString(KEY_UPDATING));
		feature.addPlugins(added);
		monitor.worked(1);
	}
	
	public void createControl(Composite parent) {
		super.createControl(parent);
		WorkbenchHelp.setHelp(getControl(),IHelpContextIds.FEATURE_ADD_PACKAGED_WIZARD);
	}

}