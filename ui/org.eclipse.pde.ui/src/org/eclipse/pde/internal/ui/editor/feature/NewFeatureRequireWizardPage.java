package org.eclipse.pde.internal.ui.editor.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.core.plugin.IPluginBase;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.feature.*;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.core.ifeature.IFeature;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.ifeature.IFeaturePlugin;
import org.eclipse.pde.internal.ui.IHelpContextIds;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.help.WorkbenchHelp;

public class NewFeatureRequireWizardPage extends ReferenceWizardPage {
	public static final String KEY_TITLE = "FeatureEditor.RequiresSection.newPlugin.title";
	public static final String KEY_DESC = "FeatureEditor.RequiresSection.newPlugin.desc";
	public static final String KEY_ADDING = "FeatureEditor.RequiresSection.newPlugin.adding";
	public static final String KEY_UPDATING =
		"FeatureEditor.RequiresSection.newPlugin.updating";

	public NewFeatureRequireWizardPage(IFeatureModel model) {
		super(model, true);
		setTitle(PDEPlugin.getResourceString(KEY_TITLE));
		setDescription(PDEPlugin.getResourceString(KEY_DESC));
	}
	
	protected boolean isOnTheList(IPluginModelBase candidate) {
		IPluginBase plugin = candidate.getPluginBase();
		IFeatureImport[] imports = model.getFeature().getImports();

		for (int i = 0; i < imports.length; i++) {
			IFeatureImport fimport = imports[i];
			if (plugin.getId().equals(fimport.getId()))
				return true;
		}
		// don't show plug-ins that are listed in this feature
		IFeaturePlugin [] fplugins = model.getFeature().getPlugins();
		for (int i=0; i<fplugins.length; i++) {
			IFeaturePlugin fplugin = fplugins[i];
			if (plugin.getId().equals(fplugin.getId()))
				return true;
		}
		return false;
	}

	protected void doAdd(Object [] candidates, IProgressMonitor monitor) throws CoreException {
		monitor.beginTask(
			PDEPlugin.getResourceString(KEY_ADDING),
			candidates.length + 1);
		IFeature feature = model.getFeature();
		IFeatureImport[] added = new IFeatureImport[candidates.length];
		for (int i = 0; i < candidates.length; i++) {
			IPluginModelBase candidate = (IPluginModelBase) candidates[i];
			IPluginBase pluginBase = candidate.getPluginBase();
			monitor.subTask(pluginBase.getTranslatedName());
			FeatureImport fimport = (FeatureImport) model.getFactory().createImport();
			fimport.setPlugin((IPlugin)candidate.getPluginBase());
			fimport.setId(pluginBase.getId());
			added[i] = fimport;
			monitor.worked(1);
		}
		monitor.subTask("");
		monitor.setTaskName(PDEPlugin.getResourceString(KEY_UPDATING));
		feature.addImports(added);
		monitor.worked(1);
	}
	
	public void createControl(Composite parent) {
		super.createControl(parent);
		WorkbenchHelp.setHelp(getControl(), IHelpContextIds.FEATURE_ADD_REQUIRED_WIZARD);
	}

}