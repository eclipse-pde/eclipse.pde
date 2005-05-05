/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.util;

import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.elements.*;
import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.swt.graphics.*;
import org.eclipse.swt.layout.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.dialogs.*;


public class PluginWorkingSet extends WizardPage implements IWorkingSetPage {
	
	class ContentProvider extends DefaultContentProvider implements IStructuredContentProvider {
		public Object[] getElements(Object inputElement) {
			return PDECore.getDefault().getModelManager().getAllPlugins();
		}	
	}
	
	class WorkingSetLabelProvider extends LabelProvider {
		
		Preferences pref = PDEPlugin.getDefault().getPluginPreferences();
		
		public WorkingSetLabelProvider() {
			PDEPlugin.getDefault().getLabelProvider().connect(this);
		}
		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
		 */
		public String getText(Object element) {
			if (element instanceof IPluginModelBase) {
				IPluginBase plugin = ((IPluginModelBase)element).getPluginBase();
				String showType = pref.getString(IPreferenceConstants.PROP_SHOW_OBJECTS);
				if (showType.equals(IPreferenceConstants.VALUE_USE_IDS))
					return plugin.getId();
				return plugin.getTranslatedName();
			}
			return super.getText(element);
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.LabelProvider#getImage(java.lang.Object)
		 */
		public Image getImage(Object element) {
			return PDEPlugin.getDefault().getLabelProvider().getImage(element);
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.LabelProvider#dispose()
		 */
		public void dispose() {
			super.dispose();
			PDEPlugin.getDefault().getLabelProvider().disconnect(this);
		}
		
	}

	private IWorkingSet fWorkingSet;
	private Text fWorkingSetName;
	private CheckboxTableViewer fTable;
	private boolean fFirstCheck;

	public PluginWorkingSet() {
		super("page1", PDEUIMessages.PluginWorkingSet_title, PDEPluginImages.DESC_DEFCON_WIZ); //$NON-NLS-1$ //$NON-NLS-2$
		PDEPlugin.getDefault().getLabelProvider().connect(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.IWorkingSetPage#finish()
	 */
	public void finish() {
		Object[] checked = fTable.getCheckedElements();
		ArrayList list = new ArrayList();
		for (int i = 0; i < checked.length; i++) {
			String id = ((IPluginModelBase)checked[i]).getPluginBase().getId();
			if (id != null && id.length() > 0)
				list.add(new PersistablePluginObject(id));
		}
		PersistablePluginObject[] objects = (PersistablePluginObject[])list.toArray(new PersistablePluginObject[list.size()]);
		
		String workingSetName = fWorkingSetName.getText().trim();
		if (fWorkingSet == null) {
			IWorkingSetManager workingSetManager = PlatformUI.getWorkbench().getWorkingSetManager();
			fWorkingSet= workingSetManager.createWorkingSet(workingSetName, objects);
		} else {
			fWorkingSet.setName(workingSetName);
			fWorkingSet.setElements(objects);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.IWorkingSetPage#getSelection()
	 */
	public IWorkingSet getSelection() {
		return fWorkingSet;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.IWorkingSetPage#setSelection(org.eclipse.ui.IWorkingSet)
	 */
	public void setSelection(IWorkingSet workingSet) {
		fWorkingSet = workingSet;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite composite= new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		setControl(composite);

		Label label= new Label(composite, SWT.WRAP);
		label.setText(PDEUIMessages.PluginWorkingSet_setName); //$NON-NLS-1$
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		fWorkingSetName= new Text(composite, SWT.SINGLE | SWT.BORDER);
		fWorkingSetName.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fWorkingSetName.addModifyListener(
			new ModifyListener() {
				public void modifyText(ModifyEvent e) {
					validatePage();
				}
			}
		);
		fWorkingSetName.setFocus();
		
		label= new Label(composite, SWT.WRAP);
		label.setText(PDEUIMessages.PluginWorkingSet_setContent); //$NON-NLS-1$
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		fTable= CheckboxTableViewer.newCheckList(composite, SWT.BORDER);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 250;
		fTable.getControl().setLayoutData(gd);
		final IStructuredContentProvider fTableContentProvider = new ContentProvider(); 
		fTable.setContentProvider(fTableContentProvider);
		fTable.setLabelProvider(new WorkingSetLabelProvider());
		fTable.setUseHashlookup(true);
		fTable.setInput(PDECore.getDefault().getModelManager());

		fTable.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				validatePage();
			}
		});

		// Add select / deselect all buttons for bug 46669
		Composite buttonComposite = new Composite(composite, SWT.NONE);
		buttonComposite.setLayout(new GridLayout(2, false));
		buttonComposite.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		
		Button selectAllButton = new Button(buttonComposite, SWT.PUSH);
		selectAllButton.setText(PDEUIMessages.PluginWorkingSet_selectAll_label);
		selectAllButton.setToolTipText(PDEUIMessages.PluginWorkingSet_selectAll_toolTip);
		selectAllButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent selectionEvent) {
				fTable.setCheckedElements(fTableContentProvider.getElements(fTable.getInput()));
				validatePage();
			}
		});

		Button deselectAllButton = new Button(buttonComposite, SWT.PUSH);
		deselectAllButton.setText(PDEUIMessages.PluginWorkingSet_deselectAll_label);
		deselectAllButton.setToolTipText(PDEUIMessages.PluginWorkingSet_deselectAll_toolTip);
		deselectAllButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent selectionEvent) {
				fTable.setCheckedElements(new Object[0]);
				validatePage();
			}
		});
		
		initialize();
		Dialog.applyDialogFont(composite);
	}
	
	/**
	 * 
	 */
	private void initialize() {
		if (fWorkingSet != null) {
			HashSet set = new HashSet();
			IAdaptable[] elements = fWorkingSet.getElements();
			for (int i = 0; i < elements.length; i++) {
				set.add(((PersistablePluginObject)elements[i]).getPluginID());
			}
			for (int i = 0; i < fTable.getTable().getItemCount(); i++) {
				IPluginModelBase model = (IPluginModelBase)fTable.getElementAt(i);
				String id = model.getPluginBase().getId();
				if (id == null)
					continue;
				if (set.contains(id)) {
					fTable.setChecked(model, true);
					set.remove(id);
				}
				if (set.isEmpty())
					break;
			}
			fWorkingSetName.setText(fWorkingSet.getName());
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#dispose()
	 */
	public void dispose() {
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
	}
	
	private void validatePage() {
		String errorMessage= null; //$NON-NLS-1$
		String newText= fWorkingSetName.getText();

		if (newText.trim().length() == 0) { //$NON-NLS-1$
			errorMessage = PDEUIMessages.PluginWorkingSet_emptyName; //$NON-NLS-1$
			if (fFirstCheck) {
				setPageComplete(false);
				fFirstCheck= false;
				return;
			}
		}
		if (errorMessage == null && fTable.getCheckedElements().length == 0) {
			errorMessage = PDEUIMessages.PluginWorkingSet_noPluginsChecked; //$NON-NLS-1$
		}
		
		if (errorMessage == null && fWorkingSet == null) {
			IWorkingSet[] workingSets = PlatformUI.getWorkbench().getWorkingSetManager().getWorkingSets();
			for (int i = 0; i < workingSets.length; i++) {
				if (newText.equals(workingSets[i].getName())) {
					errorMessage = PDEUIMessages.PluginWorkingSet_nameInUse; //$NON-NLS-1$
					break;
				}
			}
		}
		setErrorMessage(errorMessage);
		setPageComplete(errorMessage == null);		
	}

}
