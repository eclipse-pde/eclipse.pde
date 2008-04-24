/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Bartosz Michalik <bartosz.michalik@gmail.com> - bug 109440
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
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
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

	public class BuildpathContentProvider extends DefaultContentProvider implements IStructuredContentProvider {
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

		protected StructuredViewer createStructuredViewer(Composite parent, int style, FormToolkit toolkit) {
			StructuredViewer viewer = super.createStructuredViewer(parent, style, toolkit);
			viewer.setComparator(ListUtil.PLUGIN_COMPARATOR);
			return viewer;
		}
	}

	public NewLibraryPluginCreationUpdateRefPage(LibraryPluginFieldData data, Collection initialJarPaths, Collection selection) {
		super("UpdateReferences"); //$NON-NLS-1$
		setTitle(PDEUIMessages.UpdateBuildpathWizard_title);
		setDescription(PDEUIMessages.UpdateBuildpathWizard_desc);
		computeUnmigrated();
		computeSelected(selection);
		fData = data;
		tablePart = new TablePart(PDEUIMessages.UpdateBuildpathWizard_availablePlugins);
		PDEPlugin.getDefault().getLabelProvider().connect(this);
	}

	private void computeSelected(Collection initialSelection) {
		if (initialSelection == null || initialSelection.size() == 0)
			return;
		Set selected = new HashSet();
		Iterator iter = initialSelection.iterator();
		while (iter.hasNext()) {
			Object obj = iter.next();
			if (obj instanceof IProject) {
				IPluginModelBase model = PluginRegistry.findModel((IProject) obj);
				if (model != null) {
					selected.add(model);
				}
			}
		}
		fSelected = (IPluginModelBase[]) selected.toArray(new IPluginModelBase[selected.size()]);

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
		ArrayList modelArray = new ArrayList();
		try {
			for (int i = 0; i < models.length; i++) {
				if (models[i].getUnderlyingResource().getProject().hasNature(JavaCore.NATURE_ID))
					modelArray.add(models[i]);
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
		fUnmigrated = (IPluginModelBase[]) modelArray.toArray(new IPluginModelBase[modelArray.size()]);
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
