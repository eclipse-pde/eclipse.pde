/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.wizards.feature;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.elements.*;
import org.eclipse.pde.internal.ui.wizards.*;
import org.eclipse.pde.internal.ui.wizards.feature.NewFeaturePatchWizard.*;
import org.eclipse.swt.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.help.*;

public class PatchPluginListPage extends BasePluginListPage {

	public static final String PAGE_TITLE = "PatchPlugins.title"; //$NON-NLS-1$
	public static final String PAGE_DESC = "PatchPlugins.desc"; //$NON-NLS-1$
	private IProjectProvider provider;
	private CheckboxTableViewer pluginViewer;

	/**
	 * @param provider
	 */
	public PatchPluginListPage(IProjectProvider provider) {
		super("patchPluginList"); //$NON-NLS-1$
		this.provider = provider;
		setTitle(PDEPlugin.getResourceString(PAGE_TITLE));
		setDescription(PDEPlugin.getResourceString(PAGE_DESC));
	}

	class PluginContentProvider extends DefaultContentProvider
			implements
				IStructuredContentProvider {

		public Object[] getElements(Object parent) {
			return getPluginModels();
		}
	}

	public PatchPluginListPage() {
		super("patchPluginListPage"); //$NON-NLS-1$
		setTitle(PDEPlugin.getResourceString(PAGE_TITLE));
		setDescription(PDEPlugin.getResourceString(PAGE_DESC));
	}

	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NULL);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.verticalSpacing = 9;
		container.setLayout(layout);

		tablePart.createControl(container);
		pluginViewer = tablePart.getTableViewer();
		pluginViewer.setContentProvider(new PluginContentProvider());
		pluginViewer
				.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		pluginViewer.setSorter(ListUtil.PLUGIN_SORTER);
		GridData gd = (GridData) tablePart.getControl().getLayoutData();
		gd.heightHint = 250;
		pluginViewer.setInput(PDECore.getDefault().getWorkspaceModelManager());
		tablePart.setSelection(new Object[0]);
		setControl(container);
		Dialog.applyDialogFont(container);
		WorkbenchHelp.setHelp(container,
				IHelpContextIds.NEW_PATCH_REFERENCED_PLUGINS);
	}

	private Object[] getPluginModels() {
		IFeatureModel featureModel = ((FeaturePatchProvider)provider).getFeatureToPatch();
		if (featureModel== null)
			return new Object[0];
		return featureModel.getFeature().getPlugins();
	}

	public IFeaturePlugin[] getSelectedPlugins(){
		IFeatureModel featureModel = ((FeaturePatchProvider)provider).getFeatureToPatch();
		if (featureModel== null)
			return new IFeaturePlugin[0];
		Object[] result = tablePart.getSelection();
		IFeaturePlugin[] plugins = new IFeaturePlugin[result.length];
		for (int i = 0 ;i<plugins.length; i++){
			plugins[i] = (IFeaturePlugin)result[i];
		}
		return plugins;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.wizards.feature.BasePluginListPage#setVisible(boolean)
	 */
	public void setVisible(boolean visible) {
		if (visible){
			pluginViewer.refresh();
		}
		super.setVisible(visible);
	}
}
