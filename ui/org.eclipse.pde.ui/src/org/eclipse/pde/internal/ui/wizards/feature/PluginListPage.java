/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.feature;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.elements.*;
import org.eclipse.pde.internal.ui.wizards.*;
import org.eclipse.swt.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.help.*;

public class PluginListPage extends BasePluginListPage {
	public static final String PAGE_TITLE = "NewFeatureWizard.PlugPage.title"; //$NON-NLS-1$
	public static final String PAGE_DESC = "NewFeatureWizard.PlugPage.desc"; //$NON-NLS-1$

	class PluginContentProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			return PDECore.getDefault().getModelManager().getPlugins();
		}
	}

	public PluginListPage() {
		super("pluginListPage"); //$NON-NLS-1$
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
		CheckboxTableViewer pluginViewer = tablePart.getTableViewer();
		pluginViewer.setContentProvider(new PluginContentProvider());
		pluginViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		pluginViewer.setSorter(ListUtil.PLUGIN_SORTER);
		GridData gd = (GridData) tablePart.getControl().getLayoutData();
		gd.heightHint = 250;
		pluginViewer.setInput(PDECore.getDefault().getWorkspaceModelManager());
		tablePart.setSelection(new Object[0]);
		setControl(container);
		Dialog.applyDialogFont(container);
		WorkbenchHelp.setHelp(container, IHelpContextIds.NEW_FEATURE_REFERENCED_PLUGINS);
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
