/*******************************************************************************
 * Copyright (c) 2005, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Ian Bull <irbull@cs.uvic.ca> - bug 204404
 *******************************************************************************/
package org.eclipse.pde.internal.ui.launcher;

import java.util.*;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.elements.DefaultContentProvider;
import org.eclipse.pde.internal.ui.elements.NamedElement;
import org.eclipse.pde.internal.ui.util.PersistablePluginObject;
import org.eclipse.pde.internal.ui.util.SWTUtil;
import org.eclipse.pde.internal.ui.wizards.ListUtil;
import org.eclipse.pde.ui.launcher.AbstractLauncherTab;
import org.eclipse.pde.ui.launcher.IPDELauncherConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.dialogs.IWorkingSetSelectionDialog;

public abstract class AbstractPluginBlock {

	protected AbstractLauncherTab fTab;

	protected CheckboxTreeViewer fPluginTreeViewer;
	protected NamedElement fWorkspacePlugins;
	protected NamedElement fExternalPlugins;
	protected IPluginModelBase[] fExternalModels;
	protected IPluginModelBase[] fWorkspaceModels;
	protected int fNumExternalChecked;
	protected int fNumWorkspaceChecked;

	private Button fIncludeOptionalButton;
	protected Button fAddWorkspaceButton;
	private Button fAutoValidate;

	private Button fSelectAllButton;
	private Button fDeselectButton;
	private Button fWorkingSetButton;
	private Button fAddRequiredButton;
	private Button fDefaultsButton;
	private Button fFilterButton;

	private Listener fListener = new Listener();

	private Label fCounter;

	private LaunchValidationOperation fOperation;
	private PluginStatusDialog fDialog;

	private Button fValidateButton;

	class Listener extends SelectionAdapter {

		private void filterAffectingControl(SelectionEvent e) {
			boolean resetFilterButton = false;
			Object source = e.getSource();

			// If the filter is on, turn it off, apply the action, and turn it back on.
			// This has to happen this way because there is no real model behind
			// the view.  The only model is the actual plug-in model, and the state
			// does not get set on that model until an apply is performed.
			if (fFilterButton.getSelection()) {
				fFilterButton.setSelection(false);
				handleFilterButton();
				resetFilterButton = true;
			}
			if (source == fSelectAllButton) {
				toggleGroups(true);
			} else if (source == fDeselectButton) {
				toggleGroups(false);
			} else if (source == fWorkingSetButton) {
				handleWorkingSets();
			} else if (source == fAddRequiredButton) {
				computeSubset();
			} else if (source == fDefaultsButton) {
				handleRestoreDefaults();
			}

			if (resetFilterButton) {
				resetFilterButton = false;
				fFilterButton.setSelection(true);
				handleFilterButton();
			}
		}

		public void widgetSelected(SelectionEvent e) {
			Object source = e.getSource();

			if (source == fFilterButton) {
				handleFilterButton();
			} else if (source == fSelectAllButton || source == fDeselectButton || source == fWorkingSetButton || source == fAddRequiredButton || source == fDefaultsButton) {
				// These are all the controls that may affect the filtering.  For example, the filter
				// is enabled only to show selected bundles, and the user invokes "select all", we need
				// to update the filter.  
				filterAffectingControl(e);
			} else if (source == fValidateButton) {
				handleValidate();
			}
			fTab.updateLaunchConfigurationDialog();
		}
	}

	class PluginContentProvider extends DefaultContentProvider implements ITreeContentProvider {
		public boolean hasChildren(Object parent) {
			return !(parent instanceof IPluginModelBase);
		}

		public Object[] getChildren(Object parent) {
			if (parent == fExternalPlugins)
				return fExternalModels;
			if (parent == fWorkspacePlugins)
				return fWorkspaceModels;
			return new Object[0];
		}

		public Object getParent(Object child) {
			if (child instanceof IPluginModelBase) {
				IResource resource = ((IPluginModelBase) child).getUnderlyingResource();
				return resource == null ? fExternalPlugins : fWorkspacePlugins;
			}
			return null;
		}

		public Object[] getElements(Object input) {
			ArrayList list = new ArrayList();
			if (fWorkspaceModels.length > 0)
				list.add(fWorkspacePlugins);
			if (fExternalModels.length > 0)
				list.add(fExternalPlugins);
			return list.toArray();
		}
	}

	public AbstractPluginBlock(AbstractLauncherTab tab) {
		fTab = tab;
		PDEPlugin.getDefault().getLabelProvider().connect(this);
		fExternalModels = getExternalModels();
		fWorkspaceModels = getWorkspaceModels();
	}

	/**
	 * Returns an array of external plugins that are currently enabled.
	 * @return array of external enabled plugins, possibly empty
	 */
	protected IPluginModelBase[] getExternalModels() {
		Preferences pref = PDECore.getDefault().getPluginPreferences();
		String saved = pref.getString(ICoreConstants.CHECKED_PLUGINS);
		if (saved.equals(ICoreConstants.VALUE_SAVED_NONE))
			return new IPluginModelBase[0];

		IPluginModelBase[] models = PluginRegistry.getExternalModels();
		if (saved.equals(ICoreConstants.VALUE_SAVED_ALL))
			return models;

		ArrayList list = new ArrayList(models.length);
		for (int i = 0; i < models.length; i++) {
			if (models[i].isEnabled()) {
				list.add(models[i]);
			}
		}
		return (IPluginModelBase[]) list.toArray(new IPluginModelBase[list.size()]);
	}

	/**
	 * Returns an array of plugins from the workspace.  Non-OSGi plugins (no valid bundle
	 * manifest) will be filtered out.
	 * @return array of workspace OSGi plugins, possibly empty
	 */
	protected IPluginModelBase[] getWorkspaceModels() {
		IPluginModelBase[] models = PluginRegistry.getWorkspaceModels();
		ArrayList list = new ArrayList(models.length);
		for (int i = 0; i < models.length; i++) {
			if (models[i].getBundleDescription() != null) {
				list.add(models[i]);
			}
		}
		return (IPluginModelBase[]) list.toArray(new IPluginModelBase[list.size()]);
	}

	protected void updateCounter() {
		if (fCounter != null) {
			int checked = fNumExternalChecked + fNumWorkspaceChecked;
			int total = fWorkspaceModels.length + fExternalModels.length;
			fCounter.setText(NLS.bind(PDEUIMessages.AbstractPluginBlock_counter, new Integer(checked), new Integer(total)));
		}
	}

	public void createControl(Composite parent, int span, int indent) {
		createPluginViewer(parent, span - 1, indent);
		createButtonContainer(parent);
		fIncludeOptionalButton = createButton(parent, span, indent, NLS.bind(PDEUIMessages.AdvancedLauncherTab_includeOptional, fTab.getName().toLowerCase(Locale.ENGLISH)));
		fAddWorkspaceButton = createButton(parent, span, indent, NLS.bind(PDEUIMessages.AdvancedLauncherTab_addNew, fTab.getName().toLowerCase(Locale.ENGLISH)));

		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = span;
		Label label = new Label(parent, SWT.SEPARATOR | SWT.HORIZONTAL);
		label.setLayoutData(gd);

		fAutoValidate = createButton(parent, span - 1, indent, NLS.bind(PDEUIMessages.PluginsTabToolBar_auto_validate, fTab.getName().replaceAll("&", "").toLowerCase(Locale.ENGLISH))); //$NON-NLS-1$ //$NON-NLS-2$

		fValidateButton = new Button(parent, SWT.PUSH);
		fValidateButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END));
		fValidateButton.setText(NLS.bind(PDEUIMessages.PluginsTabToolBar_validate, fTab.getName().replaceAll("&", ""))); //$NON-NLS-1$ //$NON-NLS-2$
		SWTUtil.setButtonDimensionHint(fValidateButton);
		fValidateButton.addSelectionListener(fListener);
	}

	private Button createButton(Composite parent, int span, int indent, String text) {
		Button button = new Button(parent, SWT.CHECK);
		button.setText(text);

		GridData gd = new GridData();
		gd.horizontalSpan = span;
		gd.horizontalIndent = indent;
		button.setLayoutData(gd);
		button.addSelectionListener(fListener);

		return button;
	}

	protected ILabelProvider getLabelProvider() {
		return PDEPlugin.getDefault().getLabelProvider();
	}

	protected void createPluginViewer(Composite composite, int span, int indent) {
		fPluginTreeViewer = new CheckboxTreeViewer(composite, getTreeViewerStyle());
		fPluginTreeViewer.addCheckStateListener(new ICheckStateListener() {

			public void checkStateChanged(CheckStateChangedEvent event) {
				// Since a check on the root of a CheckBoxTreeViewer selects all its children 
				// (hidden or not), we need to ensure that all items are shown
				// if this happens.  Since it not clear what the best behaviour is here
				// this just "un-selects" the filter button.

				if (!event.getChecked())
					return; // just return if the check state goes to false
				// It is not clear if this is the best approach, but it 
				// is hard to tell without user feedback.  
				TreeItem[] items = fPluginTreeViewer.getTree().getItems();
				for (int i = 0; i < items.length; i++) {
					if (event.getElement() == items[i].getData()) {
						// If the even happens on the root of the tree
						fFilterButton.setSelection(false);
						handleFilterButton();
						return;
					}
				}
			}

		});
		fPluginTreeViewer.setContentProvider(new PluginContentProvider());
		fPluginTreeViewer.setLabelProvider(getLabelProvider());
		fPluginTreeViewer.setAutoExpandLevel(2);
		fPluginTreeViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(final CheckStateChangedEvent event) {
				Object element = event.getElement();
				if (element instanceof IPluginModelBase) {
					handleCheckStateChanged(event);
				} else {
					handleGroupStateChanged(element, event.getChecked());
				}
				fTab.updateLaunchConfigurationDialog();
			}
		});
		fPluginTreeViewer.setComparator(new ListUtil.PluginComparator() {
			public int category(Object obj) {
				if (obj == fWorkspacePlugins)
					return -1;
				return 0;
			}
		});

		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.horizontalSpan = span;
		gd.horizontalIndent = indent;
		fPluginTreeViewer.getTree().setLayoutData(gd);

		Image siteImage = PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_SITE_OBJ);

		fWorkspacePlugins = new NamedElement(PDEUIMessages.AdvancedLauncherTab_workspacePlugins, siteImage);
		fExternalPlugins = new NamedElement(PDEUIMessages.PluginsTab_target, siteImage);

		fPluginTreeViewer.addFilter(new Filter());
	}

	/**
	 * The view filter for the tree view.  Currently this filter only 
	 * filters unchecked items if the fFilterButton is selected.
	 * 
	 * @author Ian Bull
	 *
	 */
	class Filter extends ViewerFilter {
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (fFilterButton.getSelection()) {
				return fPluginTreeViewer.getChecked(element);
			}
			return true;
		}
	}

	private void createButtonContainer(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = layout.marginWidth = 0;
		layout.marginTop = 6;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_VERTICAL));

		fSelectAllButton = createButton(composite, PDEUIMessages.AdvancedLauncherTab_selectAll, SWT.PUSH);
		fDeselectButton = createButton(composite, PDEUIMessages.AdvancedLauncherTab_deselectAll, SWT.PUSH);
		fWorkingSetButton = createButton(composite, PDEUIMessages.AdvancedLauncherTab_workingSet, SWT.PUSH);
		fAddRequiredButton = createButton(composite, NLS.bind(PDEUIMessages.AdvancedLauncherTab_subset, fTab.getName()), SWT.PUSH);
		fDefaultsButton = createButton(composite, PDEUIMessages.AdvancedLauncherTab_defaults, SWT.PUSH);
		fFilterButton = createButton(composite, NLS.bind(PDEUIMessages.AdvancedLauncherTab_selectedBundles, fTab.getName().toLowerCase()), SWT.CHECK);
		GridData filterButtonGridData = new GridData(GridData.FILL_BOTH | GridData.VERTICAL_ALIGN_END);
		fFilterButton.setLayoutData(filterButtonGridData);

		fCounter = new Label(composite, SWT.NONE);
		fCounter.setLayoutData(new GridData(GridData.FILL_HORIZONTAL | GridData.VERTICAL_ALIGN_END));
		updateCounter();
	}

	protected int getTreeViewerStyle() {
		return SWT.BORDER;
	}

	private Button createButton(Composite composite, String text, int style) {
		Button button = new Button(composite, style);
		button.setText(text);
		button.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		SWTUtil.setButtonDimensionHint(button);
		button.addSelectionListener(fListener);
		return button;
	}

	protected void handleCheckStateChanged(CheckStateChangedEvent event) {
		IPluginModelBase model = (IPluginModelBase) event.getElement();
		if (model.getUnderlyingResource() == null) {
			if (event.getChecked()) {
				fNumExternalChecked += 1;
			} else {
				fNumExternalChecked -= 1;
			}
		} else {
			if (event.getChecked()) {
				fNumWorkspaceChecked += 1;
			} else {
				fNumWorkspaceChecked -= 1;
			}
		}
		adjustGroupState();
	}

	protected void handleGroupStateChanged(Object group, boolean checked) {
		fPluginTreeViewer.setSubtreeChecked(group, checked);
		fPluginTreeViewer.setGrayed(group, false);

		if (group == fWorkspacePlugins)
			fNumWorkspaceChecked = checked ? fWorkspaceModels.length : 0;
		else if (group == fExternalPlugins)
			fNumExternalChecked = checked ? fExternalModels.length : 0;

	}

	protected void toggleGroups(boolean select) {
		handleGroupStateChanged(fWorkspacePlugins, select);
		handleGroupStateChanged(fExternalPlugins, select);
	}

	protected void handleFilterButton() {
		refreshTreeView(fPluginTreeViewer);
		fPluginTreeViewer.refresh();
		fPluginTreeViewer.expandAll();
	}

	private void handleWorkingSets() {
		IWorkingSetManager workingSetManager = PlatformUI.getWorkbench().getWorkingSetManager();
		IWorkingSetSelectionDialog dialog = workingSetManager.createWorkingSetSelectionDialog(getShell(), true);
		if (dialog.open() == Window.OK) {
			String[] ids = getPluginIDs(dialog.getSelection());
			for (int i = 0; i < ids.length; i++) {
				IPluginModelBase model = PluginRegistry.findModel(ids[i]);
				if (model != null) {
					if (!fPluginTreeViewer.getChecked(model)) {
						setChecked(model, true);
						if (model.getUnderlyingResource() == null)
							fNumExternalChecked += 1;
						else
							fNumWorkspaceChecked += 1;
					}
				}
			}
			adjustGroupState();
		}
	}

	protected void setChecked(IPluginModelBase model, boolean checked) {
		fPluginTreeViewer.setChecked(model, checked);
	}

	private String[] getPluginIDs(IWorkingSet[] workingSets) {
		HashSet set = new HashSet();
		for (int i = 0; i < workingSets.length; i++) {
			IAdaptable[] elements = workingSets[i].getElements();
			for (int j = 0; j < elements.length; j++) {
				Object element = elements[j];
				if (element instanceof PersistablePluginObject) {
					set.add(((PersistablePluginObject) element).getPluginID());
				} else {
					if (element instanceof IJavaProject)
						element = ((IJavaProject) element).getProject();
					if (element instanceof IProject) {
						IPluginModelBase model = PluginRegistry.findModel((IProject) element);
						if (model != null)
							set.add(model.getPluginBase().getId());
					}
				}
			}
		}
		return (String[]) set.toArray(new String[set.size()]);
	}

	public void initializeFrom(ILaunchConfiguration config) throws CoreException {
		fIncludeOptionalButton.setSelection(config.getAttribute(IPDELauncherConstants.INCLUDE_OPTIONAL, true));
		fAddWorkspaceButton.setSelection(config.getAttribute(IPDELauncherConstants.AUTOMATIC_ADD, true));
		fAutoValidate.setSelection(config.getAttribute(IPDELauncherConstants.AUTOMATIC_VALIDATE, false));
		if (fPluginTreeViewer.getInput() == null) {
			fPluginTreeViewer.setUseHashlookup(true);
			fPluginTreeViewer.setInput(PDEPlugin.getDefault());
			fPluginTreeViewer.reveal(fWorkspacePlugins);
		}
		fFilterButton.setSelection(config.getAttribute(IPDELauncherConstants.SHOW_SELECTED_ONLY, false));
	}

	protected void computeSubset() {
		Object[] checked = fPluginTreeViewer.getCheckedElements();
		ArrayList toCheck = new ArrayList(checked.length);
		for (int i = 0; i < checked.length; i++)
			if (checked[i] instanceof IPluginModelBase)
				toCheck.add(checked[i]);

		Set additionalIds = DependencyManager.getDependencies(checked, fIncludeOptionalButton.getSelection());

		Iterator it = additionalIds.iterator();
		while (it.hasNext()) {
			String id = (String) it.next();
			if (findPlugin(id) == null) {
				ModelEntry entry = PluginRegistry.findEntry(id);
				if (entry != null) {
					IPluginModelBase model = entry.getModel();
					if (model != null)
						toCheck.add(model);
				}
			}
		}

		checked = toCheck.toArray();
		setCheckedElements(checked);
		fNumExternalChecked = 0;
		fNumWorkspaceChecked = 0;
		for (int i = 0; i < checked.length; i++) {
			if (((IPluginModelBase) checked[i]).getUnderlyingResource() != null)
				fNumWorkspaceChecked += 1;
			else
				fNumExternalChecked += 1;
		}
		adjustGroupState();
	}

	protected void setCheckedElements(Object[] checked) {
		fPluginTreeViewer.setCheckedElements(checked);
	}

	protected IPluginModelBase findPlugin(String id) {
		ModelEntry entry = PluginRegistry.findEntry(id);
		if (entry != null) {
			IPluginModelBase model = entry.getModel();
			if (fPluginTreeViewer.getChecked(model))
				return model;

			IPluginModelBase[] models = entry.getWorkspaceModels();
			for (int i = 0; i < models.length; i++) {
				if (fPluginTreeViewer.getChecked(models[i]))
					return models[i];
			}

			models = entry.getExternalModels();
			for (int i = 0; i < models.length; i++) {
				if (fPluginTreeViewer.getChecked(models[i]))
					return models[i];
			}
			return null;
		}
		return null;
	}

	protected void adjustGroupState() {
		fPluginTreeViewer.setChecked(fExternalPlugins, fNumExternalChecked > 0);
		fPluginTreeViewer.setGrayed(fExternalPlugins, fNumExternalChecked > 0 && fNumExternalChecked < fExternalModels.length);
		fPluginTreeViewer.setChecked(fWorkspacePlugins, fNumWorkspaceChecked > 0);
		fPluginTreeViewer.setGrayed(fWorkspacePlugins, fNumWorkspaceChecked > 0 && fNumWorkspaceChecked < fWorkspaceModels.length);
	}

	public void performApply(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(IPDELauncherConstants.INCLUDE_OPTIONAL, fIncludeOptionalButton.getSelection());
		config.setAttribute(IPDELauncherConstants.AUTOMATIC_ADD, fAddWorkspaceButton.getSelection());
		config.setAttribute(IPDELauncherConstants.AUTOMATIC_VALIDATE, fAutoValidate.getSelection());
		config.setAttribute(IPDELauncherConstants.SHOW_SELECTED_ONLY, fFilterButton.getSelection());
		savePluginState(config);
		updateCounter();
	}

	protected abstract void savePluginState(ILaunchConfigurationWorkingCopy config);

	public void setDefaults(ILaunchConfigurationWorkingCopy config) {
		config.setAttribute(IPDELauncherConstants.INCLUDE_OPTIONAL, true);
		config.setAttribute(IPDELauncherConstants.AUTOMATIC_ADD, true);
		config.setAttribute(IPDELauncherConstants.AUTOMATIC_VALIDATE, false);
		config.setAttribute(IPDELauncherConstants.SHOW_SELECTED_ONLY, false);
	}

	public void enableViewer(boolean enable) {
		fPluginTreeViewer.getTree().setEnabled(enable);
		fAddRequiredButton.setEnabled(enable);
		fDefaultsButton.setEnabled(enable);
		fWorkingSetButton.setEnabled(enable);
		fSelectAllButton.setEnabled(enable);
		fDeselectButton.setEnabled(enable);
		fIncludeOptionalButton.setEnabled(enable);
		fAddWorkspaceButton.setEnabled(enable);
		fCounter.setEnabled(enable);
		fFilterButton.setEnabled(enable);
	}

	public void dispose() {
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
	}

	protected boolean isEnabled() {
		return fPluginTreeViewer.getTree().isEnabled();
	}

	protected void handleRestoreDefaults() {
		TreeSet wtable = new TreeSet();
		fNumWorkspaceChecked = 0;
		fNumExternalChecked = 0;

		for (int i = 0; i < fWorkspaceModels.length; i++) {
			IPluginModelBase model = fWorkspaceModels[i];
			fNumWorkspaceChecked += 1;
			String id = model.getPluginBase().getId();
			if (id != null)
				wtable.add(model.getPluginBase().getId());
		}
		fPluginTreeViewer.setSubtreeChecked(fWorkspacePlugins, true);

		fNumExternalChecked = 0;
		for (int i = 0; i < fExternalModels.length; i++) {
			IPluginModelBase model = fExternalModels[i];
			boolean masked = wtable.contains(model.getPluginBase().getId());
			if (!masked && model.isEnabled()) {
				fPluginTreeViewer.setChecked(model, true);
				fNumExternalChecked += 1;
			}
		}
		adjustGroupState();
	}

	protected Shell getShell() {
		// use Shell of launcher window.  If launcher window is disposed (not sure how it could happen), use workbenchwindow.  Bug 168198
		try {
			Control c = fTab.getControl();
			if (!c.isDisposed())
				return c.getShell();
		} catch (SWTException e) {
		}
		return PDEPlugin.getActiveWorkbenchShell();
	}

	public void handleValidate() {
		if (fOperation == null)
			fOperation = createValidationOperation();
		try {
			fOperation.run(new NullProgressMonitor());
		} catch (CoreException e) {
			PDEPlugin.log(e);
		}
		if (fDialog == null) {
			if (fOperation.hasErrors()) {
				fDialog = new PluginStatusDialog(getShell(), SWT.CLOSE | SWT.MODELESS | SWT.BORDER | SWT.TITLE | SWT.RESIZE);
				fDialog.setInput(fOperation.getInput());
				fDialog.open();
				fDialog = null;
			} else if (fOperation.isEmpty()) {
				MessageDialog.openInformation(PDEPlugin.getActiveWorkbenchShell(), PDEUIMessages.PluginStatusDialog_pluginValidation, NLS.bind(PDEUIMessages.AbstractLauncherToolbar_noSelection, fTab.getName().toLowerCase(Locale.ENGLISH)));
			} else {
				MessageDialog.openInformation(PDEPlugin.getActiveWorkbenchShell(), PDEUIMessages.PluginStatusDialog_pluginValidation, PDEUIMessages.AbstractLauncherToolbar_noProblems);
			}
		} else {
			fDialog.refresh(fOperation.getInput());
		}
	}

	protected abstract LaunchValidationOperation createValidationOperation();

	/**
	 * called before the TreeView is refreshed. This allows any subclasses to cache 
	 * any information in the view and redisplay after the refresh.  This is used by the 
	 * OSGiBundleBlock to cache the values of the default launch and auto launch columns
	 * in the table tree.
	 * 
	 * @param treeView The tree view that will be refreshed.
	 */
	protected abstract void refreshTreeView(CheckboxTreeViewer treeView);

}
