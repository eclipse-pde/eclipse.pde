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
package org.eclipse.pde.internal.ui.wizards.tools;

import java.util.*;

import org.eclipse.jface.dialogs.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.elements.*;
import org.eclipse.pde.internal.ui.parts.*;
import org.eclipse.pde.internal.ui.wizards.*;
import org.eclipse.swt.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.widgets.*;
import org.eclipse.ui.help.*;

public class MigratePluginWizardPage extends WizardPage {
	private IPluginModelBase[] fSelected;
	private CheckboxTableViewer fPluginListViewer;	
	private TablePart fTablePart;
	private Button fUpdateClasspathButton;
	private String S_UPDATE_CLASSATH = "updateClasspath"; //$NON-NLS-1$
	private String S_CLEAN_PROJECTS = "cleanProjects"; //$NON-NLS-1$
	private Button fCleanProjectsButton;

	public class ContentProvider
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
			FormToolkit toolkit) {
			StructuredViewer viewer =
				super.createStructuredViewer(parent, style, toolkit);
			viewer.setSorter(ListUtil.PLUGIN_SORTER);
			return viewer;
		}
	}

	public MigratePluginWizardPage(IPluginModelBase[] selected) {
		super("MigrateWizardPage"); //$NON-NLS-1$
		setTitle(PDEPlugin.getResourceString("MigrationWizard.title")); //$NON-NLS-1$
		setDescription(PDEPlugin.getResourceString("MigrationWizardPage.desc")); //$NON-NLS-1$

		this.fSelected = selected;
		fTablePart = new TablePart(PDEPlugin.getResourceString("MigrationWizardPage.label")); //$NON-NLS-1$
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
		layout.verticalSpacing = 10;
		container.setLayout(layout);

		fTablePart.createControl(container);

		fPluginListViewer = fTablePart.getTableViewer();
		fPluginListViewer.setContentProvider(new ContentProvider());
		fPluginListViewer.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());

		GridData gd = (GridData)fTablePart.getControl().getLayoutData();
		gd.heightHint = 300;
		gd.widthHint = 300;
		
		fPluginListViewer.setInput(PDEPlugin.getDefault());
		fTablePart.setSelection(fSelected);
		
		fUpdateClasspathButton = new Button(container, SWT.CHECK);
		fUpdateClasspathButton.setText(PDEPlugin.getResourceString("MigrationWizard.update")); //$NON-NLS-1$
		gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = 2;
		fUpdateClasspathButton.setLayoutData(gd);
		String update = getDialogSettings().get(S_UPDATE_CLASSATH);
		boolean doUpdate = update == null ? true : getDialogSettings().getBoolean(S_UPDATE_CLASSATH);
		fUpdateClasspathButton.setSelection(doUpdate);
		
		fCleanProjectsButton = new Button(container, SWT.CHECK);
		fCleanProjectsButton.setText(PDEPlugin.getResourceString("MigratePluginWizard.cleanProjects")); //$NON-NLS-1$
		gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.horizontalSpan = 2;
		fCleanProjectsButton.setLayoutData(gd);
		String clean = getDialogSettings().get(S_CLEAN_PROJECTS);
		boolean doClean = clean == null ? true : getDialogSettings().getBoolean(S_CLEAN_PROJECTS);
		fCleanProjectsButton.setSelection(doClean);

		setControl(container);
		Dialog.applyDialogFont(container);
		WorkbenchHelp.setHelp(container, IHelpContextIds.MIGRATE_3_0);
	}

	public IPluginModelBase[] getSelected() {
		Object[] objects = fTablePart.getSelection();
		IPluginModelBase [] models = new IPluginModelBase[objects.length];
		System.arraycopy(objects, 0, models, 0, objects.length);
		return models;
	}

	private void dialogChanged() {
		setPageComplete(fTablePart.getSelectionCount() > 0);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.WizardPage#isPageComplete()
	 */
	public boolean isPageComplete() {
		return fTablePart.getSelectionCount() > 0;
	}

	private Object[] getModels() {
		Vector result = new Vector();
		IPluginModelBase[] models =
			PDECore.getDefault().getWorkspaceModelManager().getAllModels();
		for (int i = 0; i < models.length; i++) {
			if (!models[i].getUnderlyingResource().isLinked()
				&& models[i].isLoaded()
				&& models[i].getPluginBase().getSchemaVersion() == null) {
				result.add(models[i]);
			}
		}
		return result.toArray();
	}
	
	public void storeSettings() {
		IDialogSettings settings = getDialogSettings();
		settings.put(S_UPDATE_CLASSATH, fUpdateClasspathButton.getSelection());
		settings.put(S_CLEAN_PROJECTS, fCleanProjectsButton.getSelection());
	}
	
	public boolean isUpdateClasspathRequested() {
		return fUpdateClasspathButton.getSelection();
	}
	
	public boolean isCleanProjectsRequested() {
		return fCleanProjectsButton.getSelection();
	}

}
