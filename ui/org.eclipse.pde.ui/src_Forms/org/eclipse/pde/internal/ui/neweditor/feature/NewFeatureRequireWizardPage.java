/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.neweditor.feature;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.feature.*;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.help.*;

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
