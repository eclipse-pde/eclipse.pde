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
package org.eclipse.pde.internal.ui.wizards.tools;

import java.util.Vector;

import org.eclipse.core.resources.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.osgi.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.parts.WizardCheckboxTablePart;
import org.eclipse.pde.internal.ui.wizards.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.update.ui.forms.internal.FormWidgetFactory;

public class PluginToBundleWizardPage extends WizardPage {
	private IPluginModelBase[] fSelectedPlugins;
	private CheckboxTableViewer fPluginListViewer;
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
		super("PluginToBundleWizardPage");
		setTitle(PDEPlugin.getResourceString(KEY_TITLE));
		setDescription(PDEPlugin.getResourceString(KEY_DESC));

		this.fSelectedPlugins = selected;
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

		fPluginListViewer = tablePart.getTableViewer();
		fPluginListViewer.setContentProvider(new PluginToBundleContentProvider());
		fPluginListViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());

		GridData gd = (GridData)tablePart.getControl().getLayoutData();
		gd.heightHint = 300;
		gd.widthHint = 300;
		
		fPluginListViewer.setInput(PDEPlugin.getDefault());
		tablePart.setSelection(fSelectedPlugins);
	
		tablePart.updateCounter(fSelectedPlugins.length);
		
		setControl(container);
		Dialog.applyDialogFont(container);
	}

	public void storeSettings() {
	}

	public Object[] getSelected() {
		return tablePart.getSelection();
	}

	private void dialogChanged() {
		setPageComplete(tablePart.getSelectionCount() > 0);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.WizardPage#isPageComplete()
	 */
	public boolean isPageComplete() {
		return tablePart.getSelectionCount() > 0;
	}

	private Object[] getModels() {
		Vector result = new Vector();
		IPluginModelBase[] models = PDECore.getDefault()
				.getWorkspaceModelManager().getAllModels();
		for (int i = 0; i < models.length; i++) {
			IProject project = models[i].getUnderlyingResource().getProject();
			if (OSGiWorkspaceModelManager.isPluginProject(project)
					&& !OSGiWorkspaceModelManager.isBundleProject(project)
					&& !models[i].getUnderlyingResource().isLinked())
				result.add(models[i]);
		}
		return result.toArray();
	}
}
