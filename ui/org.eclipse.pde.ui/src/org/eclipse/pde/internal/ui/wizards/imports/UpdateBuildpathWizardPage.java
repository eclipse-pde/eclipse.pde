/*
 * Copyright (c) 2002 IBM Corp.  All rights reserved.
 * This file is made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 */
package org.eclipse.pde.internal.ui.wizards.imports;

import java.util.Vector;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.parts.WizardCheckboxTablePart;
import org.eclipse.pde.internal.ui.wizards.ListUtil;
import org.eclipse.pde.internal.ui.wizards.StatusWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.update.ui.forms.internal.FormWidgetFactory;

public class UpdateBuildpathWizardPage extends StatusWizardPage {
	private IPluginModelBase[] selected;
	private boolean block;
	private CheckboxTableViewer pluginListViewer;
	private static final String KEY_TITLE = "UpdateBuildpathWizard.title";
	private static final String KEY_DESC = "UpdateBuildpathWizard.desc";
	private static final String KEY_SHOW_NAMES =
		"ImportWizard.DetailedPage.showNames";
	private static final String KEY_PLUGIN_LIST =
		"ImportWizard.DetailedPage.pluginList";
	private static final String KEY_NO_PLUGINS = "ImportWizard.messages.noPlugins";
	private static final String KEY_NO_SELECTED =
		"ImportWizard.errors.noPluginSelected";
	private static final String KEY_OUT_OF_SYNC = "PluginModelManager.outOfSync";
	
	private TablePart tablePart;

	public class BuildpathContentProvider
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

	public UpdateBuildpathWizardPage(IPluginModelBase[] selected) {
		super("UpdateBuildpathWizardPage", true);
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
		pluginListViewer.setContentProvider(new BuildpathContentProvider());
		pluginListViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());

		GridData gd = (GridData)tablePart.getControl().getLayoutData();
		gd.heightHint = 300;
		gd.widthHint = 300;

		pluginListViewer.setInput(PDEPlugin.getDefault());
		tablePart.setSelection(selected);
		setControl(container);
	}

	public void storeSettings() {
	}

	public Object[] getSelected() {
		return tablePart.getSelection();
	}

	private void dialogChanged() {
		IStatus genStatus = validatePlugins();
		updateStatus(genStatus);
	}

	private Object[] getModels() {
		Vector result = new Vector();
		try {
			IPluginModelBase[] models =
				PDECore.getDefault().getWorkspaceModelManager().getAllModels();
			for (int i = 0; i < models.length; i++) {
				if (models[i].getPluginBase().getLibraries().length == 0)
					continue;
				if (models[i].getUnderlyingResource().getProject().hasNature(JavaCore.NATURE_ID))
					result.add(models[i]);
			}
			
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
		
		return result.toArray();
	}

	private IStatus validatePlugins() {
		Object[] allModels = getModels();
		if (allModels == null || allModels.length == 0) {
			return createStatus(IStatus.ERROR, PDEPlugin.getResourceString(KEY_NO_PLUGINS));
		}
		if (tablePart.getSelectionCount() == 0) {
			return createStatus(
				IStatus.ERROR,
				PDEPlugin.getResourceString(KEY_NO_SELECTED));
		}
		return createStatus(IStatus.OK, "");
	}
}