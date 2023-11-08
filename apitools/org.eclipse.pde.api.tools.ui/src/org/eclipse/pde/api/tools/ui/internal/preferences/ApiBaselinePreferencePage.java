/*******************************************************************************
 * Copyright (c) 2007, 2019 IBM Corporation and others.
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
 *     Manumitting Technologies Inc - bug 466783
 *******************************************************************************/
package org.eclipse.pde.api.tools.ui.internal.preferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiBaselineManager;
import org.eclipse.pde.api.tools.internal.provisional.IApiMarkerConstants;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.pde.api.tools.ui.internal.ApiToolsLabelProvider;
import org.eclipse.pde.api.tools.ui.internal.IApiToolsHelpContextIds;
import org.eclipse.pde.api.tools.ui.internal.SWTFactory;
import org.eclipse.pde.api.tools.ui.internal.wizards.ApiBaselineWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;

/**
 * This preference page allows {@link IApiBaseline}s to be
 * created/removed/edited
 *
 * @since 1.0.0
 * @noextend This class is not intended to be subclassed by clients.
 */
public class ApiBaselinePreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	public static final String DATA_SELECT_OPTION_KEY = "select_option_key"; //$NON-NLS-1$
	public static final String MISSING_BASELINE_OPTION = "MISSING_BASELINE_OPTION"; //$NON-NLS-1$
	public static final String MISSING_PLUGIN_IN_BASELINE_OPTION = "MISSING_PLUGIN_IN_BASELINE_OPTION"; //$NON-NLS-1$
	public static final String ID = "org.eclipse.pde.api.tools.ui.apiprofiles.prefpage"; //$NON-NLS-1$

	/**
	 * Override to tell the label provider about uncommitted {@link IApiProfile}
	 * s that might have been set to be the new default
	 */
	class BaselineLabelProvider extends ApiToolsLabelProvider {
		@Override
		protected boolean isDefaultBaseline(Object element) {
			return isDefault(element);
		}
	}

	IApiBaselineManager manager = ApiPlugin.getDefault().getApiBaselineManager();

	private static HashSet<String> removed = new HashSet<>(8);
	CheckboxTableViewer tableviewer = null;
	ArrayList<IApiBaseline> backingcollection = new ArrayList<>(8);
	String newdefault = null;
	private Button newbutton = null;

	Button removebutton = null;

	Button editbutton = null;
	protected static int rebuildcount = 0;
	String origdefault = null;
	boolean dirty = false;
	private boolean defaultcontentchanged = false;
	boolean defaultchanged = false;

	/**
	 * The main configuration block for the page
	 */
	private ApiBaselinesConfigurationBlock block = null;

	@Override
	protected Control createContents(Composite parent) {
		Composite comp = SWTFactory.createComposite(parent, 1, 1, GridData.FILL_BOTH, 0, 0);
		SWTFactory.createWrapLabel(comp, PreferenceMessages.ApiProfilesPreferencePage_0, 2, 200);
		SWTFactory.createVerticalSpacer(comp, 1);

		Composite lcomp = SWTFactory.createComposite(comp, 2, 1, GridData.FILL_BOTH, 0, 0);
		SWTFactory.createWrapLabel(lcomp, PreferenceMessages.ApiProfilesPreferencePage_1, 2);
		Table table = new Table(lcomp, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER | SWT.CHECK);
		table.setLayoutData(new GridData(GridData.FILL_BOTH));
		table.addKeyListener(KeyListener.keyReleasedAdapter(e -> {
			if (e.stateMask == SWT.NONE && e.keyCode == SWT.DEL) {
				doRemove();
			}
		}));
		tableviewer = new CheckboxTableViewer(table);
		tableviewer.setUseHashlookup(true);
		tableviewer.setLabelProvider(new BaselineLabelProvider());
		tableviewer.setContentProvider(ArrayContentProvider.getInstance());
		tableviewer.addDoubleClickListener(event -> {
			IStructuredSelection ss = (IStructuredSelection) event.getSelection();
			doEdit((IApiBaseline) ss.getFirstElement());
		});
		tableviewer.addSelectionChangedListener(event -> {
			IApiBaseline[] state = getCurrentSelection();
			removebutton.setEnabled(state.length > 0);
			editbutton.setEnabled(state.length == 1);
		});
		tableviewer.addCheckStateListener(event -> {
			IApiBaseline baseline = (IApiBaseline) event.getElement();
			boolean checked = event.getChecked();
			if (checked) {
				tableviewer.setCheckedElements(new Object[] { baseline });
				newdefault = baseline.getName();
				defaultchanged = !newdefault.equals(origdefault);
			} else {
				tableviewer.setChecked(baseline, checked);
				newdefault = null;
				manager.setDefaultApiBaseline(null);
				defaultchanged = true;
			}
			rebuildcount = 0;
			tableviewer.refresh(true);
			dirty = true;
		});
		tableviewer.setComparator(new ViewerComparator() {
			@Override
			public int compare(Viewer viewer, Object e1, Object e2) {
				return ((IApiBaseline) e1).getName().compareTo(((IApiBaseline) e2).getName());
			}
		});
		BusyIndicator.showWhile(getShell().getDisplay(), () -> {
			backingcollection.addAll(Arrays.asList(manager.getApiBaselines()));
			tableviewer.setInput(backingcollection);
		});
		Composite bcomp = SWTFactory.createComposite(lcomp, 1, 1, GridData.FILL_VERTICAL | GridData.VERTICAL_ALIGN_BEGINNING, 0, 0);
		newbutton = SWTFactory.createPushButton(bcomp, PreferenceMessages.ApiProfilesPreferencePage_2, null);
		newbutton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
			ApiBaselineWizard wizard = new ApiBaselineWizard(null);
			WizardDialog dialog = new WizardDialog(getShell(), wizard);
			if (dialog.open() == IDialogConstants.OK_ID) {
				IApiBaseline profile = wizard.getProfile();
				if (profile != null) {
					backingcollection.add(profile);
					tableviewer.refresh();
					tableviewer.setSelection(new StructuredSelection(profile), true);
					if (backingcollection.size() == 1) {
						newdefault = profile.getName();
						tableviewer.setCheckedElements(new Object[] { profile });
						tableviewer.refresh(profile);
						defaultchanged = true;
						rebuildcount = 0;
					}
					dirty = true;
				}
			}
		}));
		editbutton = SWTFactory.createPushButton(bcomp, PreferenceMessages.ApiProfilesPreferencePage_4, null);
		editbutton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> doEdit(getCurrentSelection()[0])));
		editbutton.setEnabled(false);
		removebutton = SWTFactory.createPushButton(bcomp, PreferenceMessages.ApiProfilesPreferencePage_3, null);
		removebutton.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> doRemove()));
		removebutton.setEnabled(false);

		SWTFactory.createVerticalSpacer(bcomp, 1);

		IApiBaseline baseline = manager.getDefaultApiBaseline();
		if (baseline != null) {
			tableviewer.setCheckedElements(new Object[] { baseline });
		}
		origdefault = newdefault = (baseline == null ? null : baseline.getName());
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, IApiToolsHelpContextIds.APIBASELINE_PREF_PAGE);

		block = new ApiBaselinesConfigurationBlock((IWorkbenchPreferenceContainer) getContainer());
		block.createControl(comp, this);
		Dialog.applyDialogFont(comp);
		return comp;
	}

	/**
	 * Returns if the {@link IApiProfile} with the given name has been removed,
	 * but not yet committed back to the manager
	 *
	 * @param name the name of the {@link IApiProfile}
	 * @return true if the {@link IApiProfile} has been removed from the page,
	 *         false otherwise
	 */
	public static boolean isRemovedBaseline(String name) {
		return removed.contains(name);
	}

	/**
	 * Performs the remove action, from either the Remove button or from
	 * pressing the Delete key
	 */
	protected void doRemove() {
		IApiBaseline[] states = getCurrentSelection();
		for (IApiBaseline state : states) {
			if (isDefault(state)) {
				newdefault = null;
				manager.setDefaultApiBaseline(null);
				defaultchanged = true;
				rebuildcount = 0;
			}
			removed.add(state.getName());
		}
		if (backingcollection.removeAll(Arrays.asList(states))) {
			dirty = true;
		}
		tableviewer.refresh();
	}

	/**
	 * Performs the edit operation for the edit button and the double click
	 * listener for the table
	 *
	 * @param profile
	 */
	protected void doEdit(final IApiBaseline baseline) {
		ApiBaselineWizard wizard = new ApiBaselineWizard(baseline);
		WizardDialog dialog = new WizardDialog(getShell(), wizard);
		if (dialog.open() == IDialogConstants.OK_ID) {
			IApiBaseline newbaseline = wizard.getProfile();
			if (newbaseline != null) {
				// clear any pending edit updates
				removed.add(baseline.getName());
				backingcollection.remove(baseline);
				backingcollection.add(newbaseline);
				tableviewer.refresh();
				if (isDefault(baseline)) {
					tableviewer.setSelection(new StructuredSelection(newbaseline), true);
					tableviewer.setCheckedElements(new Object[] { newbaseline });
					newdefault = newbaseline.getName();
					rebuildcount = 0;
					defaultcontentchanged = wizard.contentChanged();
					tableviewer.refresh(true);
				}
				dirty = true;
			}
		}
	}

	/**
	 * Returns if the specified {@link IApiProfile} is the default profile or
	 * not
	 *
	 * @param element
	 * @return if the profile is the default or not
	 */
	protected boolean isDefault(Object element) {
		if (element instanceof IApiBaseline) {
			IApiBaseline profile = (IApiBaseline) element;
			if (newdefault == null) {
				IApiBaseline def = ApiPlugin.getDefault().getApiBaselineManager().getDefaultApiBaseline();
				if (def != null) {
					return profile.getName().equals(def.getName());
				}
			} else {
				return profile.getName().equals(newdefault);
			}
		}
		return false;
	}

	/**
	 * @return the current selection from the table viewer
	 */
	protected IApiBaseline[] getCurrentSelection() {
		IStructuredSelection ss = tableviewer.getStructuredSelection();
		if (ss.isEmpty()) {
			return new IApiBaseline[0];
		}
		return (IApiBaseline[]) tableviewer.getStructuredSelection().toList().toArray(new IApiBaseline[ss.size()]);
	}

	@Override
	public void init(IWorkbench workbench) {
	}

	@Override
	public boolean performCancel() {
		manager.setDefaultApiBaseline(origdefault);
		backingcollection.clear();
		removed.clear();
		if (this.block != null) {
			this.block.performCancel();
		}
		return super.performCancel();
	}

	/**
	 * Applies the changes from the current change set to the
	 * {@link ApiProfileManager}. When done the current change set is cleared.
	 */
	protected void applyChanges() {
		if (!dirty) {
			return;
		}
		// remove
		for (String string : removed) {
			manager.removeApiBaseline(string);
		}
		// add the new / changed ones
		for (IApiBaseline iApiBaseline : backingcollection) {
			manager.addApiBaseline(iApiBaseline);
		}
		manager.setDefaultApiBaseline(newdefault);
		if (defaultchanged && newdefault == null && origdefault != null) {
			findAndDeleteCompatibilityMarkers();
			Object[] checkedElements = tableviewer.getCheckedElements();
			this.block.setHasBaseline(checkedElements.length != 0);
			this.block.createMissingBaselineMarker();
			origdefault = newdefault;
			dirty = false;
			defaultcontentchanged = false;
			defaultchanged = false;
			removed.clear();
			return;
		}
		if (defaultchanged || defaultcontentchanged) {
			if (rebuildcount < 1) {
				rebuildcount++;
				IProject[] projects = Util.getApiProjects();
				// do not even ask if there are no projects to build
				if (projects != null) {
					int open = MessageDialog.open(MessageDialog.QUESTION, getShell(),
							PreferenceMessages.ApiProfilesPreferencePage_QuestionDialog_Title,
							PreferenceMessages.ApiProfilesPreferencePage_QuestionDialog_Text, SWT.NONE,
							PreferenceMessages.ApiProfilesPreferencePage_QuestionDialog_buildButtonLabel,
							PreferenceMessages.ApiProfilesPreferencePage_QuestionDialog_dontBuildButtonLabel);
					if (open == Window.OK) {
						Util.getBuildJob(projects).schedule();
					}
				}
			}
		}
		origdefault = newdefault;
		dirty = false;
		defaultcontentchanged = false;
		defaultchanged = false;
		removed.clear();
	}

	private void findAndDeleteCompatibilityMarkers() {
		IProject[] apiProjects = Util.getApiProjects();
		if (apiProjects == null) {
			return;
		}
		for (IProject iProject : apiProjects) {
			cleanupCompatibilityMarkers(iProject);
			try {
				iProject.deleteMarkers(IApiMarkerConstants.UNUSED_FILTER_PROBLEM_MARKER, false,
						IResource.DEPTH_INFINITE);
			} catch (CoreException e) {
			}

		}
		return;
	}

	/**
	 * Cleans up only API compatibility markers on the given {@link IResource}
	 *
	 * @param resource
	 *            the given resource
	 */
	void cleanupCompatibilityMarkers(IResource resource) {
		try {
			if (resource != null && resource.isAccessible()) {
				resource.deleteMarkers(IApiMarkerConstants.COMPATIBILITY_PROBLEM_MARKER, false,
						IResource.DEPTH_INFINITE);
				resource.deleteMarkers(IApiMarkerConstants.SINCE_TAGS_PROBLEM_MARKER, false, IResource.DEPTH_INFINITE);
				if (resource.getType() == IResource.PROJECT) {
					// on full builds
					resource.deleteMarkers(IApiMarkerConstants.VERSION_NUMBERING_PROBLEM_MARKER, false,
							IResource.DEPTH_INFINITE);
					resource.deleteMarkers(IApiMarkerConstants.DEFAULT_API_BASELINE_PROBLEM_MARKER, true,
							IResource.DEPTH_ZERO);
					resource.deleteMarkers(IApiMarkerConstants.API_COMPONENT_RESOLUTION_PROBLEM_MARKER, true,
							IResource.DEPTH_ZERO);
				}
			}
		} catch (CoreException e) {
			ApiPlugin.log(e.getStatus());
		}
	}
	@Override
	public boolean performOk() {
		Object[] checkedElements = tableviewer.getCheckedElements();
		this.block.setHasBaseline(checkedElements.length != 0);
		this.block.performOK();
		applyChanges();
		return true;
	}

	@Override
	protected void performApply() {
		this.block.performApply();
		applyChanges();
	}

	@Override
	protected void performDefaults() {
		this.block.performDefaults();
		applyChanges();
	}
	@Override
	public void applyData(Object data) {
		if (data instanceof Map) {
			Map<?, ?> pageData = (Map<?, ?>) data;
			Object key = pageData.get(ApiErrorsWarningsPreferencePage.DATA_SELECT_OPTION_KEY);
			if (key instanceof String) {
				String option = (String) key;
				if (option.equals(ApiBaselinePreferencePage.MISSING_BASELINE_OPTION)) {
					block.selectOption(0);
				}
				if (option.equals(ApiBaselinePreferencePage.MISSING_PLUGIN_IN_BASELINE_OPTION)) {
					block.selectOption(1);
				}
			}
		}
	}

}
