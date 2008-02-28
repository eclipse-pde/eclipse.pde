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
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.pde.api.tools.internal.ApiProfile;
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
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.PlatformUI;

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
	
	/**
	 * Wrapper for changes to the page that may (or may not) be
	 * reflected in the manager, depending if the page is cancelled or not
	 */
	class StateChange {
		int kind = -1;
		IApiProfile profile = null;
		
		/**
		 * Constructor
		 * @param profile
		 * @param kind
		 */
		public StateChange(IApiProfile profile, int kind) {
			this.profile = profile;
			this.kind = kind;
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#hashCode()
		 */
		public int hashCode() {
			return profile.getName().hashCode() + profile.getExecutionEnvironment().hashCode();
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		public boolean equals(Object obj) {
			IApiProfile comparator = null;
			if(obj instanceof StateChange) {
				comparator = ((StateChange)obj).profile;
			}
			if(obj instanceof IApiProfile) {
				comparator = (IApiProfile) obj;
			}
			if(comparator != null) {
				return (comparator.getName().equals(profile.getName()) &&
						comparator.getExecutionEnvironment().equals(profile.getExecutionEnvironment()));
			}
			return super.equals(obj);
		}
	}
	
	private IApiProfile originaldefault = null;
	private IApiProfileManager manager = ApiPlugin.getDefault().getApiProfileManager();
	
	
	private static final int ADD = 0;
	private static final int REMOVE = 1;
	private HashSet changes = new HashSet();
	private TableViewer tableviewer = null;
	private ArrayList backingcollection = new ArrayList();
	private String newdefault = null;
	private Button newbutton = null, 
				   removebutton = null, 
				   editbutton = null,
				   defaultbutton = null;
	private int rebuildcount = 0;
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createContents(Composite parent) {
		Composite comp = SWTFactory.createComposite(parent, 2, 1, GridData.FILL_BOTH, 0, 0);
		SWTFactory.createWrapLabel(comp, PreferenceMessages.ApiProfilesPreferencePage_0, 2, 200);
		SWTFactory.createWrapLabel(comp, PreferenceMessages.ApiProfilesPreferencePage_1, 2);
		Table table = new Table(comp, SWT.FULL_SELECTION | SWT.MULTI | SWT.BORDER);
		table.setLayoutData(new GridData(GridData.FILL_BOTH));
		tableviewer = new TableViewer(table);
		tableviewer.setLabelProvider(new ProfileLabelProvider());
		tableviewer.setContentProvider(new ArrayContentProvider());
		tableviewer.addDoubleClickListener(new IDoubleClickListener() {
			public void doubleClick(DoubleClickEvent event) {
				IStructuredSelection ss = (IStructuredSelection) event.getSelection();
				doEdit((IApiProfile) ss.getFirstElement());
			}
		});
		tableviewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				updateButtons();
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
		Composite bcomp = SWTFactory.createComposite(comp, 1, 1, GridData.FILL_VERTICAL | GridData.VERTICAL_ALIGN_BEGINNING, 0, 0);
		newbutton = SWTFactory.createPushButton(bcomp, PreferenceMessages.ApiProfilesPreferencePage_2, null);
		newbutton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				ApiProfileWizard wizard = new ApiProfileWizard(null);
				WizardDialog dialog = new WizardDialog(ApiUIPlugin.getShell(), wizard);
				if(dialog.open() == IDialogConstants.OK_ID) {
					IApiProfile profile = wizard.getProfile();
					if(profile != null) {
						changes.add(new StateChange(profile, ADD));
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
					changes.add(new StateChange(states[i], REMOVE));
				}
				backingcollection.removeAll(Arrays.asList(states));
				tableviewer.refresh();
			}
		});
		editbutton = SWTFactory.createPushButton(bcomp, PreferenceMessages.ApiProfilesPreferencePage_4, null);
		editbutton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				doEdit((IApiProfile)getCurrentSelection()[0]);
			}
		});
		SWTFactory.createVerticalSpacer(bcomp, 1);
		defaultbutton = SWTFactory.createPushButton(bcomp, PreferenceMessages.ApiProfilesPreferencePage_5, null);
		defaultbutton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				IApiProfile[] profiles = getCurrentSelection();
				newdefault = profiles[0].getName();
				tableviewer.refresh();
				defaultbutton.setEnabled(profiles.length == 1 && !isDefault(profiles[0]));
				rebuildcount = 0;
			}
		});
		updateButtons();
		originaldefault = manager.getDefaultApiProfile();
		PlatformUI.getWorkbench().getHelpSystem().setHelp(comp, IApiToolsHelpContextIds.APIPROFILES_PREF_PAGE);
		return comp;
	}

	/**
	 * Performs the edit operation for the edit button and the double click listener for the table
	 * @param profile
	 */
	protected void doEdit(IApiProfile profile) {
		try {
			IApiProfile clone = (IApiProfile) ((ApiProfile)profile).clone();
			ApiProfileWizard wizard = new ApiProfileWizard(clone);
			WizardDialog dialog = new WizardDialog(ApiUIPlugin.getShell(), wizard);
			if(dialog.open() == IDialogConstants.OK_ID) {
				clone = wizard.getProfile();
				if(profile != null) {
					//clear any pending edit updates
					changes.remove(profile);
					changes.add(new StateChange(clone, ADD));
					tableviewer.remove(profile);
					tableviewer.add(clone);
					tableviewer.setSelection(new StructuredSelection(clone), true);
				}
			}
		}
		catch(CloneNotSupportedException cnse) {
			ApiUIPlugin.log(cnse);
		}
	}
	
	/**
	 * updates the buttons on the page
	 */
	protected void updateButtons() {
		IApiProfile[] state = getCurrentSelection();
		removebutton.setEnabled(state.length > 0);
		editbutton.setEnabled(state.length == 1);
		defaultbutton.setEnabled(state.length == 1 && !isDefault(getCurrentSelection()[0]));
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
		StateChange change = null;
		for(Iterator iter = changes.iterator(); iter.hasNext();) {
			change = (StateChange) iter.next();
			if(change.kind == REMOVE) {
				//undo a removing change
				manager.addApiProfile(change.profile);
			}
		}
		if(originaldefault != null) {
 			manager.setDefaultApiProfile(originaldefault.getName());
 		}
		changes.clear();
		return super.performCancel();
	}

	/**
	 * Applies the changes from the current change set to the {@link ApiProfileManager}. When done
	 * the current change set is cleared.
	 */
	protected void applyChanges() {
		StateChange change = null;
		boolean build = false;
		for(Iterator iter = changes.iterator(); iter.hasNext();) {
			change = (StateChange) iter.next();
			switch(change.kind) {
				case REMOVE :
					manager.removeApiProfile(change.profile.getName());
					if(isDefault(change.profile)) {
						build = true;
					}
					break;
				case ADD :
					manager.addApiProfile(change.profile);
			}
		}
		if(newdefault != null) {
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
		changes.clear();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performOk()
	 */
	public boolean performOk() {
		applyChanges();
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.preference.PreferencePage#performApply()
	 */
	protected void performApply() {
		applyChanges();
	}
}
