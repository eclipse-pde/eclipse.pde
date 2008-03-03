/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.ui.internal.preferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.api.tools.internal.ApiProfileManager;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiProfile;
import org.eclipse.pde.api.tools.internal.provisional.IApiProfileManager;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.pde.api.tools.ui.internal.ApiToolsLabelProvider;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;
import org.eclipse.pde.api.tools.ui.internal.IApiToolsHelpContextIds;
import org.eclipse.pde.api.tools.ui.internal.SWTFactory;
import org.eclipse.pde.api.tools.ui.internal.wizards.ApiProfileWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;

/**
 * This preference page allows {@link IApiProfile}s to be created/removed/edited
 * @since 1.0.0
 */
public class ApiProfilesPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
	/**
	 * Override to tell the label provider about uncommitted {@link IApiProfile}s that might have been set to 
	 * be the new default
	 */
	class ProfileLabelProvider extends ApiToolsLabelProvider {
		
		/* (non-Javadoc)
		 * @see org.eclipse.pde.api.tools.ui.internal.ApiToolsLabelProvider#isDefaultProfile(java.lang.Object)
		 */
		protected boolean isDefaultProfile(Object element) {
			return isDefault(element);
		}
	}
	
	private IApiProfileManager manager = ApiPlugin.getDefault().getApiProfileManager();

	private HashSet removed = new HashSet();
	private CheckboxTableViewer tableviewer = null;
	private ArrayList backingcollection = new ArrayList();
	private String newdefault = null;
	private Button newbutton = null, 
				   removebutton = null, 
				   editbutton = null;
	private int rebuildcount = 0;
	private String origdefault = null;
	
	/**
	 * The main configuration block for the page
	 */
	private ApiProfilesConfigurationBlock block = null;
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createContents(Composite parent) {
		Composite comp = SWTFactory.createComposite(parent, 1, 1, GridData.FILL_BOTH, 0, 0);
		Group profileGroup = SWTFactory.createGroup(comp, PreferenceMessages.ApiProfilesConfigurationBlock_profile_group_title, 2, 1, GridData.FILL_BOTH);

		SWTFactory.createWrapLabel(profileGroup, PreferenceMessages.ApiProfilesPreferencePage_0, 2, 200);
		SWTFactory.createWrapLabel(profileGroup, PreferenceMessages.ApiProfilesPreferencePage_1, 2);
		Table table = new Table(profileGroup, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER | SWT.CHECK);
		table.setLayoutData(new GridData(GridData.FILL_BOTH));
		tableviewer = new CheckboxTableViewer(table);
		tableviewer.setLabelProvider(new ProfileLabelProvider());
		tableviewer.setContentProvider(new ArrayContentProvider());
		tableviewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				IStructuredSelection ss = (IStructuredSelection) event.getSelection();
				doEdit((IApiProfile) ss.getFirstElement());
			}
		});
		tableviewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				IApiProfile profile = (IApiProfile) event.getElement();
				if(event.getChecked()) {
					tableviewer.setCheckedElements(new Object[] {profile});
					newdefault = profile.getName();
				}
				else {
					newdefault = null;
					manager.setDefaultApiProfile(null);
				}
				rebuildcount = 0;
				tableviewer.refresh(true);
			}
		});
		tableviewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				IApiProfile[] state = getCurrentSelection();
				removebutton.setEnabled(state.length > 0);
				editbutton.setEnabled(state.length == 1);
			}
		});
		tableviewer.setComparator(new ViewerComparator() {
			public int compare(Viewer viewer, Object e1, Object e2) {
				return ((IApiProfile)e1).getName().compareTo(((IApiProfile)e2).getName());
			}
		});
		BusyIndicator.showWhile(getShell().getDisplay(), new Runnable() {
			public void run() {	
				backingcollection.addAll(Arrays.asList(manager.getApiProfiles()));
				tableviewer.setInput(backingcollection);
			}
		});
		Composite bcomp = SWTFactory.createComposite(profileGroup, 1, 1, GridData.FILL_VERTICAL | GridData.VERTICAL_ALIGN_BEGINNING, 0, 0);
		newbutton = SWTFactory.createPushButton(bcomp, PreferenceMessages.ApiProfilesPreferencePage_2, null);
		newbutton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				ApiProfileWizard wizard = new ApiProfileWizard(null);
				WizardDialog dialog = new WizardDialog(ApiUIPlugin.getShell(), wizard);
				if(dialog.open() == IDialogConstants.OK_ID) {
					IApiProfile profile = wizard.getProfile();
					if(profile != null) {
						backingcollection.add(profile);
						tableviewer.refresh();
						tableviewer.setSelection(new StructuredSelection(profile), true);
					}
				}
			}
		});
		removebutton = SWTFactory.createPushButton(bcomp, PreferenceMessages.ApiProfilesPreferencePage_3, null);
		removebutton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				IApiProfile[] states = getCurrentSelection();
				for(int i = 0; i < states.length; i++) {
					removed.add(states[i].getName());
				}
				backingcollection.removeAll(Arrays.asList(states));
				tableviewer.refresh();
			}
		});
		removebutton.setEnabled(false);
		editbutton = SWTFactory.createPushButton(bcomp, PreferenceMessages.ApiProfilesPreferencePage_4, null);
		editbutton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				doEdit((IApiProfile)getCurrentSelection()[0]);
			}
		});
		editbutton.setEnabled(false);
		IApiProfile profile = manager.getDefaultApiProfile();
		origdefault = newdefault = (profile == null ? null : profile.getName());
		initialize();
		PlatformUI.getWorkbench().getHelpSystem().setHelp(profileGroup, IApiToolsHelpContextIds.APIPROFILES_PREF_PAGE);

		block = new ApiProfilesConfigurationBlock((IWorkbenchPreferenceContainer)getContainer());
		block.createControl(comp, this);
		return comp;
	}
	
	/**
	 * Performs the edit operation for the edit button and the double click listener for the table
	 * @param profile
	 */
	protected void doEdit(final IApiProfile profile) {
		ApiProfileWizard wizard = new ApiProfileWizard(profile);
		WizardDialog dialog = new WizardDialog(ApiUIPlugin.getShell(), wizard);
		if(dialog.open() == IDialogConstants.OK_ID) {
			IApiProfile newprofile = wizard.getProfile();
			if(newprofile != null) {
				//clear any pending edit updates
				removed.add(profile.getName());
				backingcollection.remove(profile);
				backingcollection.add(newprofile);
				tableviewer.refresh();
				if(isDefault(profile)) {
					tableviewer.setCheckedElements(new Object[] {newprofile});
					tableviewer.setSelection(new StructuredSelection(newprofile), true);
					newdefault = newprofile.getName();
					rebuildcount = 0;
					tableviewer.refresh(true);
				}
			}
		}
	}
	
	/**
	 * updates the buttons on the page
	 */
	protected void initialize() {
		IApiProfile def = ApiPlugin.getDefault().getApiProfileManager().getDefaultApiProfile();
		if(def != null) {
			tableviewer.setCheckedElements(new Object[] {def});
		}
	}
	
	/**
	 * Returns if the specified {@link IApiProfile} is the default profile or not
	 * @param element
	 * @return if the profile is the default or not
	 */
	protected boolean isDefault(Object element) {
		if(element instanceof IApiProfile) {
			IApiProfile profile = (IApiProfile) element;
			if(newdefault == null) {
				IApiProfile def = ApiPlugin.getDefault().getApiProfileManager().getDefaultApiProfile();
				if(def != null) {
					return profile.getName().equals(def.getName());
				}
			}
			else {
				return profile.getName().equals(newdefault);
			}
		}
		return false;
	}
	
	/**
	 * @return the current selection from the table viewer
	 */
	protected IApiProfile[] getCurrentSelection() {
		IStructuredSelection ss = (IStructuredSelection) tableviewer.getSelection();
		if(ss.isEmpty()) {
			return new IApiProfile[0];
		}
		return (IApiProfile[]) ((IStructuredSelection) tableviewer.getSelection()).toList().toArray(new IApiProfile[ss.size()]);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
	 */
	public void init(IWorkbench workbench) {}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performCancel()
	 */
	public boolean performCancel() {
		manager.setDefaultApiProfile(origdefault);
		backingcollection.clear();
		removed.clear();
		if(this.block != null) {
			this.block.performCancel();
		}
		return super.performCancel();
	}

	/**
	 * Applies the changes from the current change set to the {@link ApiProfileManager}. When done
	 * the current change set is cleared.
	 */
	protected void applyChanges() {
		boolean build = false;
		//remove 
		for(Iterator iter = removed.iterator(); iter.hasNext();) {
			manager.removeApiProfile((String) iter.next());
		}
		//add the new / changed ones
		for(Iterator iter = backingcollection.iterator(); iter.hasNext();) {
			manager.addApiProfile((IApiProfile) iter.next());
		}
		IApiProfile def = ApiPlugin.getDefault().getApiProfileManager().getDefaultApiProfile();
		if(def != null && !def.getName().equals(newdefault)) {
			manager.setDefaultApiProfile(newdefault);
			build = true;
		}
		else if(def == null) {
			manager.setDefaultApiProfile(newdefault);
			build = true;
		}
		if(build) {
			if(rebuildcount < 1) {
				rebuildcount++;
				if(MessageDialog.openQuestion(getShell(), PreferenceMessages.ApiProfilesPreferencePage_6, PreferenceMessages.ApiProfilesPreferencePage_7)) {
					Util.getBuildJob(null).schedule();
				}
			}
		}
		origdefault = newdefault;
		removed.clear();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performOk()
	 */
	public boolean performOk() {
		this.block.performOK();
		applyChanges();
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performApply()
	 */
	protected void performApply() {
		this.block.performApply();
		applyChanges();
	}
}
