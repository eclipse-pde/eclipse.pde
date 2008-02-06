/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.ui.internal.wizards;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.api.tools.internal.provisional.Factory;
import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.IApiProfile;
import org.eclipse.pde.api.tools.internal.provisional.IRequiredComponentDescription;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.pde.api.tools.ui.internal.ApiToolsLabelProvider;
import org.eclipse.pde.api.tools.ui.internal.IApiToolsHelpContextIds;
import org.eclipse.pde.api.tools.ui.internal.SWTFactory;
import org.eclipse.pde.internal.core.PluginPathFinder;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

/**
 * The wizard page allowing a new api profiles to be created
 * or an existing one to be edited
 * @since 1.0.0
 */
public class ApiProfileWizardPage extends WizardPage {
	
	/**
	 * Resets the baseline contents based on current settings and a location from which
	 * to read plug-ins.
	 */
	class ReloadOperation implements IRunnableWithProgress {
		private String location, name, id, version, eeid;
	
		/**
		 * Constructor
		 * @param platformPath
		 */
		public ReloadOperation(String name, String id, String version, String eeid, String location) {
			this.location = location;
			this.name = name;
			this.id = id;
			this.version = version;
			this.eeid = eeid;
		}
		
		/**
		 * Returns a list of URLs of plug-ins from the configured location.
		 * 
		 * @return plug-in URLs
		 */
		private URL[] computePluginURLs() {
			URL[] base  = PluginPathFinder.getPluginPaths(location);		
			if (extralocations.size() == 0) {
				return base;
			}
			File[] extraLocations = new File[extralocations.size() * 2];
			for (int i = 0; i < extraLocations.length; i++) {
				String location = extralocations.get(i/2).toString();
				File dir = new File(location);
				extraLocations[i] = dir;
				dir = new File(dir, "plugins"); //$NON-NLS-1$
				extraLocations[++i] = dir;
			}
			URL[] additional = PluginPathFinder.scanLocations(extraLocations);
			if (additional.length == 0) {
				return base;
			}
			URL[] result = new URL[base.length + additional.length];
			System.arraycopy(base, 0, result, 0, base.length);
			System.arraycopy(additional, 0, result, base.length, additional.length);
			return result;
		}
			
		/* (non-Javadoc)
		 * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core.runtime.IProgressMonitor)
		 */
		public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {	
			monitor.beginTask(WizardMessages.ApiProfileWizardPage_0, 10);
			URL[] urls = computePluginURLs();
			monitor.worked(1);
			try {
				File eeFile = Util.createEEFile(eeid);
				profile = Factory.newApiProfile(name, id, version, eeFile);
			} catch (CoreException e) {
				throw new InvocationTargetException(e);
			} catch (IOException e) {
				throw new InvocationTargetException(e);
			}
			SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 8);
			subMonitor.beginTask("", urls.length); //$NON-NLS-1$
			List components = new ArrayList();
			for (int i = 0; i < urls.length; i++) {
				try {
					IApiComponent component = profile.newApiComponent(urls[i].getFile());
					if (component != null) {
						components.add(component);
					}
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				}
				subMonitor.worked(1);
			}
			subMonitor.done();
			profile.addApiComponents((IApiComponent[]) components.toArray(new IApiComponent[components.size()]), true);
			monitor.worked(1);
			monitor.done();
		}
	}
	
	private IApiProfile profile = null;
	private ArrayList extralocations = new ArrayList();
	private Text nametext = null,
				 versiontext = null,
				 idtext = null;
	private CheckboxTableViewer tableviewer = null;
	private Combo eecombo = null,
				  locationcombo = null;
	private Button browsebutton = null,
				   resetbutton = null,
				   reloadbutton = null,
				   enablesbutton = null,
				   enableallbutton = null,
				   disablesbutton = null,
				   disableallbutton = null,
				   addrequiredbutton = null;
	
	/**
	 * Constructor
	 * @param profile
	 */
	protected ApiProfileWizardPage(IApiProfile profile) {
		super(WizardMessages.ApiProfileWizardPage_1);
		this.profile = profile;
		setTitle(WizardMessages.ApiProfileWizardPage_1);
		if(profile == null) {
			setMessage(WizardMessages.ApiProfileWizardPage_3);
		}
		else {
			setMessage(WizardMessages.ApiProfileWizardPage_4);
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite comp = SWTFactory.createComposite(parent, 2, 1, GridData.FILL_HORIZONTAL);
		Composite ncomp = SWTFactory.createComposite(comp, 2, 2, GridData.FILL_HORIZONTAL, 0, 0);
		SWTFactory.createWrapLabel(ncomp, WizardMessages.ApiProfileWizardPage_5, 1);
		nametext = SWTFactory.createText(ncomp, SWT.BORDER | SWT.SINGLE, 1, GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL | GridData.BEGINNING);
		nametext.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				setPageComplete(pageValid());
			}
		});
		
		SWTFactory.createWrapLabel(ncomp, WizardMessages.ApiProfileWizardPage_6, 1);
		idtext = SWTFactory.createText(ncomp, SWT.BORDER | SWT.SINGLE, 1, GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL | GridData.BEGINNING);
		idtext.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				setPageComplete(pageValid());
			}
		});
		
		SWTFactory.createWrapLabel(ncomp, WizardMessages.ApiProfileWizardPage_7, 1);
		versiontext = SWTFactory.createText(ncomp, SWT.BORDER | SWT.SINGLE, 1, GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL | GridData.BEGINNING);
		versiontext.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				setPageComplete(pageValid());
			}
		});
		IExecutionEnvironment[] envs = JavaRuntime.getExecutionEnvironmentsManager().getExecutionEnvironments();
		String[] items = new String[envs.length];
		for(int i = 0; i < envs.length; i++) {
			items[i] = envs[i].getId();
		}
		Arrays.sort(items, new Comparator() {
			public int compare(Object o1, Object o2) {
				return ((String)o1).compareTo(o2);
			}
		});
		
		SWTFactory.createWrapLabel(ncomp, WizardMessages.ApiProfileWizardPage_8, 1, 160);
		eecombo = SWTFactory.createCombo(ncomp, SWT.READ_ONLY | SWT.BORDER | SWT.SINGLE | SWT.DROP_DOWN, 1, GridData.FILL_HORIZONTAL, items);
		
		SWTFactory.createHorizontalSpacer(comp, 1);
		Composite tcomp = SWTFactory.createComposite(comp, 4, 2, GridData.FILL_HORIZONTAL, 0, 0);
		SWTFactory.createWrapLabel(tcomp, WizardMessages.ApiProfileWizardPage_9, 1);
		locationcombo = SWTFactory.createCombo(tcomp, SWT.BORDER | SWT.SINGLE, 1, GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL | GridData.BEGINNING, null);
		locationcombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				setPageComplete(pageValid());
				updateButtons();
			}
		});
		browsebutton = SWTFactory.createPushButton(tcomp, WizardMessages.ApiProfileWizardPage_10, null);
		browsebutton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dialog = new DirectoryDialog(getShell());
				dialog.setMessage(WizardMessages.ApiProfileWizardPage_11);
				String loctext = locationcombo.getText().trim();
				if (loctext.length() > 0) {
					dialog.setFilterPath(loctext);
				}
				String newPath = dialog.open();
				if (newPath != null && !new Path(loctext).equals(new Path(newPath))) {
					locationcombo.setText(newPath);
					extralocations.clear();
					doReload();
				}
			}
		});
		resetbutton = SWTFactory.createPushButton(tcomp, WizardMessages.ApiProfileWizardPage_12, null);
		resetbutton.setEnabled(locationcombo.getText().trim().length() > 0);
		resetbutton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				extralocations.clear();
				doReload();
			}
		});
		SWTFactory.createWrapLabel(comp, WizardMessages.ApiProfileWizardPage_13, 2);
		
		Table table = new Table(comp, SWT.CHECK | SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.heightHint = 250;
		table.setLayoutData(gd);
		tableviewer = new CheckboxTableViewer(table);
		tableviewer.setLabelProvider(new ApiToolsLabelProvider());
		tableviewer.setContentProvider(new ArrayContentProvider());
		tableviewer.setComparator(new ViewerComparator());
		tableviewer.setInput(getCurrentComponents());
		tableviewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
			}
		});
		tableviewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				updateButtons();
			}
		});
		
		Composite bcomp = SWTFactory.createComposite(comp, 1, 1, GridData.FILL_VERTICAL | GridData.BEGINNING, 0, 0);
		reloadbutton = SWTFactory.createPushButton(bcomp, WizardMessages.ApiProfileWizardPage_14, null);
		reloadbutton.setEnabled(locationcombo.getText().trim().length() > 0);
		reloadbutton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				doReload();
			}
		});
		SWTFactory.createVerticalSpacer(bcomp, 1);
		enablesbutton = SWTFactory.createPushButton(bcomp, WizardMessages.ApiProfileWizardPage_15, null);
		enablesbutton.setEnabled(!tableviewer.getSelection().isEmpty());
		enablesbutton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				checkSelected(true);
			}
		});
		disablesbutton = SWTFactory.createPushButton(bcomp, WizardMessages.ApiProfileWizardPage_16, null);
		disablesbutton.setEnabled(!tableviewer.getSelection().isEmpty());
		disablesbutton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				checkSelected(false);
			}
		});
		enableallbutton = SWTFactory.createPushButton(bcomp, WizardMessages.ApiProfileWizardPage_17, null);
		enableallbutton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				checkAll(true);
			}
		});
		disableallbutton = SWTFactory.createPushButton(bcomp, WizardMessages.ApiProfileWizardPage_18, null);
		disableallbutton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				checkAll(false);
			}
		});
		SWTFactory.createVerticalSpacer(bcomp, 1);
		addrequiredbutton = SWTFactory.createPushButton(bcomp, WizardMessages.ApiProfileWizardPage_19, null);
		addrequiredbutton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				performRequired();
			}
		});
		setControl(comp);
		setPageComplete(profile != null);
		initialize();
		PlatformUI.getWorkbench().getHelpSystem().setHelp(comp, IApiToolsHelpContextIds.APIPROFILES_WIZARD_PAGE);
	}

	/**
	 * Runs over all of the checked elements in the viewer and sets checked any of the required 
	 * components from the checked ones (if they exist in the current profile).
	 * 
	 * This is an N^3 operation...
	 */
	protected void performRequired() {
		Object[] objects = tableviewer.getCheckedElements();
		IApiComponent component = null;
		IRequiredComponentDescription[] required = null;
		TableItem[] allcomponents = tableviewer.getTable().getItems();
		for(int i = 0; i < objects.length; i++) {
			component = (IApiComponent) objects[i];
			required = component.getRequiredComponents();
			for(int j = 0; j < required.length; j++) {
				for(int k = 0; k < allcomponents.length; k++) {
					component = (IApiComponent)allcomponents[k].getData(); 
					if(component != null && component.getId().equals(required[j].getId())) {
						tableviewer.setChecked(component, true);
					}
				}
			}
		}
	}
	
	/**
	 * Initializes the controls of the page if the profile is not <code>null</code>
	 */
	protected void initialize() {
		if(profile != null) {
			nametext.setText(profile.getName());
			idtext.setText(profile.getId());
			versiontext.setText(profile.getVersion());
			eecombo.setText(profile.getExecutionEnvironment());
			//TODO init component enablements?? if we decide to allow specific target elements to be enabled
			IApiComponent[] components = profile.getApiComponents();
			HashSet locations = new HashSet();
			IPath location = null;
			for(int i = 0; i < components.length; i++) {
				tableviewer.setChecked(components[i], profile.isEnabled(components[i]));
				if(!components[i].isSystemComponent()) {
					location = new Path(components[i].getLocation()).removeLastSegments(1);
					if(location.toFile().isDirectory()) {
						locations.add(location.removeTrailingSeparator().toOSString());
					}
				}
			}
			if(locations.size() > 0) {
				locationcombo.setItems((String[]) locations.toArray(new String[locations.size()]));
				locationcombo.select(0);
			}
		}
	}
	
	/**
	 * Reloads all of the plugins from the location specified in the location text field.
	 */
	protected void doReload() {
		ReloadOperation op = new ReloadOperation(nametext.getText().trim(), idtext.getText().trim(), versiontext.getText().trim(), eecombo.getText().trim(), locationcombo.getText().trim());
		try {
			PlatformUI.getWorkbench().getProgressService().run(true, false, op);
			tableviewer.setInput(getCurrentComponents());
			tableviewer.setAllChecked(true);
			tableviewer.refresh();
		} 
		catch (InvocationTargetException ite) {} 
		catch (InterruptedException ie) {}
	}
	
	/**
	 * @return if the page is valid, such that it is considered complete and can be 'finished'
	 */
	protected boolean pageValid() {
		String text = nametext.getText().trim();
		if(text.length() < 1) {
			setErrorMessage(WizardMessages.ApiProfileWizardPage_20);
			return false;
		}
		text = idtext.getText().trim();
		if(text.length() < 1) {
			setErrorMessage(WizardMessages.ApiProfileWizardPage_21);
			return false;
		}
		text = versiontext.getText().trim();
		if(text.length() < 1) {
			setErrorMessage(WizardMessages.ApiProfileWizardPage_22);
			return false;
		}
		text = locationcombo.getText().trim();
		if(text.length() < 1) {
			setErrorMessage(WizardMessages.ApiProfileWizardPage_23);
			reloadbutton.setEnabled(false);
			return false;
		}
		if(!new Path(text).toFile().exists()) {
			setErrorMessage(WizardMessages.ApiProfileWizardPage_24);
			reloadbutton.setEnabled(false);
			return false;
		}
		
		setErrorMessage(null);
		return true;
	}
	
	/**
	 * Returns the current API components in the baseline or an empty collection if none.
	 * 
	 * @return the current API components in the baseline or an empty collection if none
	 */
	protected IApiComponent[] getCurrentComponents() {
		if (profile != null) {
			return profile.getApiComponents();
		}
		return new IApiComponent[0];
	}
	
	/**
	 * Sets the items currently selected in the table viewer to be the specified checked state
	 * @param the desired checked state
	 */
	protected void checkSelected(boolean checked) {
		try {
			tableviewer.getTable().setRedraw(false);
			Object[] objs = ((IStructuredSelection) tableviewer.getSelection()).toArray();
			for(int i = 0; i < objs.length; i++) {
				tableviewer.setChecked(objs[i], checked);
			}
		}
		finally {
			tableviewer.getTable().setRedraw(true);
			tableviewer.refresh();
		}
	}
	
	/**
	 * Sets the checked state of all of the items in the table viewer to be the specified checked state
	 * @param checked the desired checked state
	 */
	protected void checkAll(boolean checked) {
		try {
			tableviewer.getTable().setRedraw(false);
			tableviewer.setAllChecked(checked);
		}
		finally {
			tableviewer.getTable().setRedraw(true);
			tableviewer.refresh();
		}
	}
	
	/**
	 * Updates the state of a variety of buttons on this page
	 */
	protected void updateButtons() {
		String loctext = locationcombo.getText().trim();
		int size = loctext.length();
		resetbutton.setEnabled(size > 0);
		reloadbutton.setEnabled(size > 0);
		size = (tableviewer.getSelection().isEmpty() ? 0 : 1);
		enablesbutton.setEnabled(size > 0);
		disablesbutton.setEnabled(size > 0);
	}
	
	/**
	 * Creates or edits the profile and returns it
	 * @return a new {@link IApiProfile} or <code>null</code> if an error was encountered
	 * creating the new profile
	 */
	public IApiProfile finish() throws IOException, CoreException {
		String eeid = eecombo.getText().trim();
		if(!profile.getExecutionEnvironment().equals(eeid)) {
			File eefile = Util.createEEFile(eeid);
			profile.setExecutionEnvironment(eefile);
		}
		profile.setName(nametext.getText().trim());
		profile.setId(idtext.getText().trim());
		profile.setVersion(versiontext.getText().trim());
		return profile;
	}
}
