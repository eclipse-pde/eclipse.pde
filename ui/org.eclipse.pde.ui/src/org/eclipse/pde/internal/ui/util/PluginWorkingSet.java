/*******************************************************************************
 * Copyright (c) 2005, 2016 IBM Corporation and others.
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
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 487943
 *******************************************************************************/
package org.eclipse.pde.internal.ui.util;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.util.ArrayList;
import java.util.HashSet;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.PDEPreferencesManager;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.dialogs.*;

public class PluginWorkingSet extends WizardPage implements IWorkingSetPage {

	class ContentProvider implements ITreeContentProvider {
		@Override
		public Object[] getElements(Object inputElement) {
			return PluginRegistry.getAllModels();
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			return null;
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			return false;
		}
	}

	class WorkingSetLabelProvider extends LabelProvider {

		PDEPreferencesManager pref = PDEPlugin.getDefault().getPreferenceManager();

		public WorkingSetLabelProvider() {
			PDEPlugin.getDefault().getLabelProvider().connect(this);
		}

		@Override
		public String getText(Object element) {
			if (element instanceof IPluginModelBase) {
				IPluginBase plugin = ((IPluginModelBase) element).getPluginBase();
				String showType = pref.getString(IPreferenceConstants.PROP_SHOW_OBJECTS);
				if (showType.equals(IPreferenceConstants.VALUE_USE_IDS))
					return plugin.getId();
				return plugin.getTranslatedName();
			}
			return super.getText(element);
		}

		@Override
		public Image getImage(Object element) {
			return PDEPlugin.getDefault().getLabelProvider().getImage(element);
		}

		@Override
		public void dispose() {
			super.dispose();
			PDEPlugin.getDefault().getLabelProvider().disconnect(this);
		}

	}

	class CheckboxFilteredTree extends FilteredTree {

		public CheckboxFilteredTree(Composite parent, int treeStyle, PatternFilter filter) {
			super(parent, treeStyle, filter, true);
		}

		@Override
		protected TreeViewer doCreateTreeViewer(Composite parent, int style) {
			return new CheckboxTreeViewer(parent, style);
		}

		public CheckboxTreeViewer getCheckboxTreeViewer() {
			return (CheckboxTreeViewer) getViewer();
		}

	}

	private IWorkingSet fWorkingSet;
	private Text fWorkingSetName;
	private CheckboxFilteredTree fTree;
	private boolean fFirstCheck;

	public PluginWorkingSet() {
		super("page1", PDEUIMessages.PluginWorkingSet_title, PDEPluginImages.DESC_DEFCON_WIZ); //$NON-NLS-1$
		PDEPlugin.getDefault().getLabelProvider().connect(this);
	}

	@Override
	public void finish() {
		Object[] checked = fTree.getCheckboxTreeViewer().getCheckedElements();
		ArrayList<PersistablePluginObject> list = new ArrayList<>();
		for (Object checkedElement : checked) {
			String id = ((IPluginModelBase) checkedElement).getPluginBase().getId();
			if (id != null && id.length() > 0)
				list.add(new PersistablePluginObject(id));
		}
		PersistablePluginObject[] objects = list.toArray(new PersistablePluginObject[list.size()]);

		String workingSetName = fWorkingSetName.getText().trim();
		if (fWorkingSet == null) {
			IWorkingSetManager workingSetManager = PlatformUI.getWorkbench().getWorkingSetManager();
			fWorkingSet = workingSetManager.createWorkingSet(workingSetName, objects);
		} else {
			fWorkingSet.setName(workingSetName);
			fWorkingSet.setElements(objects);
		}
	}

	@Override
	public IWorkingSet getSelection() {
		return fWorkingSet;
	}

	@Override
	public void setSelection(IWorkingSet workingSet) {
		fWorkingSet = workingSet;
	}

	@Override
	public void createControl(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		setControl(composite);

		Label label = new Label(composite, SWT.WRAP);
		label.setText(PDEUIMessages.PluginWorkingSet_setName);
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		fWorkingSetName = new Text(composite, SWT.SINGLE | SWT.BORDER);
		fWorkingSetName.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fWorkingSetName.addModifyListener(e -> validatePage());
		fWorkingSetName.setFocus();

		label = new Label(composite, SWT.WRAP);
		label.setText(PDEUIMessages.PluginWorkingSet_setContent);
		label.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		fTree = new CheckboxFilteredTree(composite, SWT.BORDER, new PatternFilter());
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 250;
		fTree.getViewer().getControl().setLayoutData(gd);
		final IStructuredContentProvider fTableContentProvider = new ContentProvider();
		fTree.getCheckboxTreeViewer().setContentProvider(fTableContentProvider);
		fTree.getCheckboxTreeViewer().setLabelProvider(new WorkingSetLabelProvider());
		fTree.getCheckboxTreeViewer().setUseHashlookup(true);
		fTree.getCheckboxTreeViewer().setInput(PDECore.getDefault());

		fTree.getCheckboxTreeViewer().addCheckStateListener(event -> validatePage());

		// Add select / deselect all buttons for bug 46669
		Composite buttonComposite = new Composite(composite, SWT.NONE);
		buttonComposite.setLayout(new GridLayout(2, true));
		buttonComposite.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));

		Button selectAllButton = new Button(buttonComposite, SWT.PUSH);
		selectAllButton.setText(PDEUIMessages.PluginWorkingSet_selectAll_label);
		selectAllButton.setToolTipText(PDEUIMessages.PluginWorkingSet_selectAll_toolTip);
		selectAllButton.addSelectionListener(widgetSelectedAdapter(selectionEvent -> {
			fTree.getCheckboxTreeViewer().setCheckedElements(fTableContentProvider.getElements(fTree.getCheckboxTreeViewer().getInput()));
			validatePage();
		}));
		selectAllButton.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(selectAllButton);

		Button deselectAllButton = new Button(buttonComposite, SWT.PUSH);
		deselectAllButton.setText(PDEUIMessages.PluginWorkingSet_deselectAll_label);
		deselectAllButton.setToolTipText(PDEUIMessages.PluginWorkingSet_deselectAll_toolTip);
		deselectAllButton.addSelectionListener(widgetSelectedAdapter(selectionEvent -> {
			fTree.getCheckboxTreeViewer().setCheckedElements(new Object[0]);
			validatePage();
		}));
		deselectAllButton.setLayoutData(new GridData());
		SWTUtil.setButtonDimensionHint(deselectAllButton);
		setPageComplete(false);
		setMessage(PDEUIMessages.PluginWorkingSet_message);

		initialize();
		Dialog.applyDialogFont(composite);

		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.PLUGIN_WORKING_SET);
	}

	/**
	 *
	 */
	private void initialize() {
		if (fWorkingSet != null) {
			HashSet<String> set = new HashSet<>();
			IAdaptable[] elements = fWorkingSet.getElements();
			for (IAdaptable element : elements) {
				if (element instanceof PersistablePluginObject)
					set.add(((PersistablePluginObject) element).getPluginID());
			}

			IPluginModelBase[] bases = PluginRegistry.getAllModels();
			for (IPluginModelBase model : bases) {

				String id = model.getPluginBase().getId();
				if (id == null)
					continue;
				if (set.contains(id)) {
					fTree.getCheckboxTreeViewer().setChecked(model, true);
					set.remove(id);
				}
				if (set.isEmpty())
					break;
			}
			fWorkingSetName.setText(fWorkingSet.getName());
		}
	}

	@Override
	public void dispose() {
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
		super.dispose();
	}

	private void validatePage() {
		String errorMessage = null;
		String newText = fWorkingSetName.getText();

		if (newText.trim().length() == 0) {
			errorMessage = PDEUIMessages.PluginWorkingSet_emptyName;
			if (fFirstCheck) {
				setPageComplete(false);
				fFirstCheck = false;
				return;
			}
		}
		if (errorMessage == null && fTree.getCheckboxTreeViewer().getCheckedElements().length == 0) {
			errorMessage = PDEUIMessages.PluginWorkingSet_noPluginsChecked;
		}

		if (errorMessage == null && fWorkingSet == null) {
			IWorkingSet[] workingSets = PlatformUI.getWorkbench().getWorkingSetManager().getWorkingSets();
			for (IWorkingSet workingSet : workingSets) {
				if (newText.equals(workingSet.getName())) {
					errorMessage = PDEUIMessages.PluginWorkingSet_nameInUse;
					break;
				}
			}
		}
		setErrorMessage(errorMessage);
		setPageComplete(errorMessage == null);
	}

}
