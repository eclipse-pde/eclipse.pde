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
package org.eclipse.pde.internal.ui.osgi.wizards;

import java.util.Vector;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.parts.WizardCheckboxTablePart;
import org.eclipse.pde.internal.ui.wizards.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.update.ui.forms.internal.FormWidgetFactory;

public class PluginToBundleWizardPage extends StatusWizardPage {
	private IPluginModelBase[] selected;
	private CheckboxTableViewer pluginListViewer;
	private static final String KEY_TITLE = "PluginToBundleWizard.title";
	private static final String KEY_DESC = "PluginToBundleWizard.desc";
	private static final String KEY_PLUGIN_LIST =
		"PluginToBundleWizard.availablePlugins";
	
	private TablePart tablePart;

	public class PluginToBundleContentProvider
		extends DefaultContentProvider
		implements IStructuredContentProvider {
		public Object[] getElements(Object parent) {
			return getModels();
		}
	}

	class TablePart extends WizardCheckboxTablePart {
		public TablePart(String mainLabel) {
			super(mainLabel);
		}

		public void updateCounter(int count) {
			super.updateCounter(count);
			dialogChanged();
		}
		protected StructuredViewer createStructuredViewer(
			Composite parent,
			int style,
			FormWidgetFactory factory) {
			StructuredViewer viewer =
				super.createStructuredViewer(parent, style, factory);
			viewer.setSorter(ListUtil.PLUGIN_SORTER);
			return viewer;
		}
	}

	public PluginToBundleWizardPage(IPluginModelBase[] selected) {
		super("PluginToBundleWizardPage", true);
		setTitle(PDEPlugin.getResourceString(KEY_TITLE));
		setDescription(PDEPlugin.getResourceString(KEY_DESC));

		this.selected = selected;
		tablePart = new TablePart(PDEPlugin.getResourceString(KEY_PLUGIN_LIST));
		PDEPlugin.getDefault().getLabelProvider().connect(this);
	}
	
	public void dispose() {
		super.dispose();
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
	}

	public void createControl(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.numColumns = 2;
		layout.marginHeight = 0;
		layout.marginWidth = 5;
		container.setLayout(layout);

		tablePart.createControl(container);

		pluginListViewer = tablePart.getTableViewer();
		pluginListViewer.setContentProvider(new PluginToBundleContentProvider());
		pluginListViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());

		GridData gd = (GridData)tablePart.getControl().getLayoutData();
		gd.heightHint = 300;
		gd.widthHint = 300;
		
		pluginListViewer.setInput(PDEPlugin.getDefault());
		tablePart.setSelection(selected);
	
		int counter = selected.length;
		// do not count projects without libraries (these will not be displayed in the table)
		for (int i = 0 ; i<selected.length; i++){
			if (((IPluginModelBase)selected[i]).getPluginBase().getLibraries().length == 0){
				counter --;
			}
		}
		tablePart.updateCounter(counter);
		
		setControl(container);
		Dialog.applyDialogFont(container);
		WorkbenchHelp.setHelp(container, IHelpContextIds.UPDATE_CLASSPATH);
	}

	public void storeSettings() {
	}

	public Object[] getSelected() {
		return tablePart.getSelection();
	}

	private void dialogChanged() {
		setPageComplete(tablePart.getSelectionCount() > 0);
	}

	private Object[] getModels() {
		Vector result = new Vector();
		IPluginModelBase[] models =
			PDECore.getDefault().getWorkspaceModelManager().getAllModels();
		for (int i = 0; i < models.length; i++) {
			result.add(models[i]);
		}
		
		return result.toArray();
	}
}
