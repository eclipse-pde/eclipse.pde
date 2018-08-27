/*******************************************************************************
 * Copyright (c) 2008, 2018 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Bartosz Michalik <bartosz.michalik@gmail.com> - bug 109440
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 487943
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.plugin;

import java.util.*;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.PluginRegistry;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.parts.WizardCheckboxTablePart;
import org.eclipse.pde.internal.ui.wizards.ListUtil;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.FormToolkit;

public class NewLibraryPluginCreationUpdateRefPage extends WizardPage {

	private IPluginModelBase[] fSelected;
	private IPluginModelBase[] fUnmigrated;
	private CheckboxTableViewer pluginListViewer;
	private TablePart tablePart;
	private LibraryPluginFieldData fData;

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
		protected StructuredViewer createStructuredViewer(Composite parent, int style, FormToolkit toolkit) {
			StructuredViewer viewer = super.createStructuredViewer(parent, style, toolkit);
			viewer.setComparator(ListUtil.PLUGIN_COMPARATOR);
			return viewer;
		}
	}

	public NewLibraryPluginCreationUpdateRefPage(LibraryPluginFieldData data, Collection<?> initialJarPaths, Collection<?> selection) {
		super("UpdateReferences"); //$NON-NLS-1$
		setTitle(PDEUIMessages.UpdateBuildpathWizard_title);
		setDescription(PDEUIMessages.UpdateBuildpathWizard_desc);
		computeUnmigrated();
		computeSelected(selection);
		fData = data;
		tablePart = new TablePart(PDEUIMessages.UpdateBuildpathWizard_availablePlugins);
		PDEPlugin.getDefault().getLabelProvider().connect(this);
	}

	private void computeSelected(Collection<?> initialSelection) {
		if (initialSelection == null || initialSelection.isEmpty())
			return;
		Set<IPluginModelBase> selected = new HashSet<>();
		Iterator<?> iter = initialSelection.iterator();
		while (iter.hasNext()) {
			Object obj = iter.next();
			if (obj instanceof IProject) {
				IPluginModelBase model = PluginRegistry.findModel((IProject) obj);
				if (model != null) {
					selected.add(model);
				}
			}
		}
		fSelected = selected.toArray(new IPluginModelBase[selected.size()]);

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
		if (fSelected != null && fSelected.length > 0) {
			tablePart.setSelection(fSelected);
		}
		setControl(container);
		Dialog.applyDialogFont(container);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(container, IHelpContextIds.UPDATE_CLASSPATH);
	}

	private void computeUnmigrated() {
		IPluginModelBase[] models = PluginRegistry.getWorkspaceModels();
		ArrayList<IPluginModelBase> modelArray = new ArrayList<>();
		try {
			for (IPluginModelBase model : models) {
				if (model.getUnderlyingResource().getProject().hasNature(JavaCore.NATURE_ID))
					modelArray.add(model);
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
		fUnmigrated = modelArray.toArray(new IPluginModelBase[modelArray.size()]);
	}

	public void setEnable(boolean enabled) {
		tablePart.setEnabled(enabled);
	}

	public void updateData() {
		IPluginModelBase[] modelBase = new IPluginModelBase[tablePart.getSelectionCount()];
		for (int i = 0; i < modelBase.length; ++i) {
			modelBase[i] = (IPluginModelBase) tablePart.getSelection()[i];
		}
		fData.setPluginsToUpdate(modelBase);
	}

}
