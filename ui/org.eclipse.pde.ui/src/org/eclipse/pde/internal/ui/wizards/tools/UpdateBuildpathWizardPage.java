/*******************************************************************************
 *  Copyright (c) 2000, 2016 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 487943
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.tools;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.parts.WizardCheckboxTablePart;
import org.eclipse.pde.internal.ui.wizards.ListUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.FormToolkit;

public class UpdateBuildpathWizardPage extends WizardPage {
	private IPluginModelBase[] fSelected;
	private IPluginModelBase[] fUnmigrated;
	private CheckboxTableViewer pluginListViewer;
	private TablePart tablePart;

	public class BuildpathContentProvider implements IStructuredContentProvider {
		@Override
		public Object[] getElements(Object parent) {
			if (fUnmigrated != null)
				return fUnmigrated;
			return new Object[0];
		}
	}

	class TablePart extends WizardCheckboxTablePart {
		public TablePart(String mainLabel) {
			super(mainLabel);
		}

		@Override
		public void updateCounter(int count) {
			super.updateCounter(count);
			dialogChanged();
		}

		@Override
		protected StructuredViewer createStructuredViewer(Composite parent, int style, FormToolkit toolkit) {
			StructuredViewer viewer = super.createStructuredViewer(parent, style, toolkit);
			viewer.setComparator(ListUtil.PLUGIN_COMPARATOR);
			return viewer;
		}
	}

	public UpdateBuildpathWizardPage(IPluginModelBase[] models, IPluginModelBase[] selected) {
		super("UpdateBuildpathWizardPage"); //$NON-NLS-1$
		setTitle(PDEUIMessages.UpdateBuildpathWizard_title);
		setDescription(PDEUIMessages.UpdateBuildpathWizard_desc);
		this.fUnmigrated = models;
		this.fSelected = selected;
		tablePart = new TablePart(PDEUIMessages.UpdateBuildpathWizard_availablePlugins);
		PDEPlugin.getDefault().getLabelProvider().connect(this);
	}

	@Override
	public void dispose() {
		super.dispose();
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
	}

	@Override
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

		GridData gd = (GridData) tablePart.getControl().getLayoutData();
		gd.heightHint = 300;
		gd.widthHint = 300;

		pluginListViewer.setInput(PDEPlugin.getDefault());
		tablePart.setSelection(fSelected);

		setControl(container);
		Dialog.applyDialogFont(container);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(container, IHelpContextIds.UPDATE_CLASSPATH);
	}

	public void storeSettings() {
	}

	public Object[] getSelected() {
		return tablePart.getSelection();
	}

	private void dialogChanged() {
		setPageComplete(tablePart.getSelectionCount() > 0);
	}

	@Override
	public boolean isPageComplete() {
		return tablePart.getSelectionCount() > 0;
	}
}
