/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.ui.internal.wizards;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.service.datalocation.Location;
import org.eclipse.pde.api.tools.internal.ApiBaselineManager;
import org.eclipse.pde.api.tools.internal.model.ApiModelFactory;
import org.eclipse.pde.api.tools.internal.model.SystemLibraryApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiBaseline;
import org.eclipse.pde.api.tools.internal.provisional.model.IApiComponent;
import org.eclipse.pde.api.tools.ui.internal.ApiToolsLabelProvider;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;
import org.eclipse.pde.api.tools.ui.internal.IApiToolsConstants;
import org.eclipse.pde.api.tools.ui.internal.IApiToolsHelpContextIds;
import org.eclipse.pde.api.tools.ui.internal.SWTFactory;
import org.eclipse.pde.api.tools.ui.internal.preferences.ApiBaselinePreferencePage;
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
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.PlatformUI;

/**
 * The wizard page allowing a new API profiles to be created
 * or an existing one to be edited
 * @since 1.0.0
 */
public class ApiBaselineWizardPage extends WizardPage {
	
	/**
	 * an EE entry (child of an api component in the viewer)
	 */
	public static class EEEntry {
		String name = null;
		/**
		 * Constructor
		 */
		public EEEntry(String name) {
			this.name = name;
		}
		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return this.name;
		}
	}
	
	/**
	 * Content provider for the viewer
	 */
	static class ContentProvider implements ITreeContentProvider {

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
		 */
		public Object[] getChildren(Object parentElement) {
			if(parentElement instanceof IApiComponent) {
				try {
					IApiComponent component = (IApiComponent) parentElement;
					String[] ees = component.getExecutionEnvironments();
					ArrayList entries = new ArrayList(ees.length);
					for(int i = 0; i < ees.length; i++) {
						entries.add(new EEEntry(ees[i]));
					}
					return entries.toArray();
				} catch (CoreException e) {
					ApiPlugin.log(e);
				}
			}
			return null;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
		 */
		public boolean hasChildren(Object element) {
			if(element instanceof IApiComponent) {
				try {
					IApiComponent component = (IApiComponent) element;
					return component.getExecutionEnvironments().length > 0;
				} catch (CoreException e) {
					ApiPlugin.log(e);
				}
			}
			return false;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
		 */
		public Object[] getElements(Object inputElement) {
			if(inputElement instanceof IApiComponent[]) {
				return (Object[]) inputElement;
			}
			return new Object[0];
		}
		public void dispose() {}
		public Object getParent(Object element) {return null;}
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
	}
	
	/**
	 * Resets the baseline contents based on current settings and a location from which
	 * to read plug-ins.
	 */
	class ReloadOperation implements IRunnableWithProgress {
		private String location, name;
	
		/**
		 * Constructor
		 * @param platformPath
		 */
		public ReloadOperation(String name, String location) {
			this.location = location;
			this.name = name;
		}
			
		/* (non-Javadoc)
		 * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core.runtime.IProgressMonitor)
		 */
		public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
			monitor.beginTask(WizardMessages.ApiProfileWizardPage_0, 10);
			try {
				fProfile = ApiModelFactory.newApiBaseline(name, location);
				ApiModelFactory.addComponents(fProfile, location, monitor);
				ApiBaselineWizardPage.this.contentchange = true;
			} catch (CoreException e) {
				ApiPlugin.log(e);
			}
			finally {
				monitor.done();
			}
		}
		
	}
	
	/**
	 * Operation that creates a new working copy for an {@link IApiProfile} that is being edited
	 */
	static class WorkingCopyOperation implements IRunnableWithProgress {
		
		IApiBaseline original = null, 
					workingcopy = null;
		
		/**
		 * Constructor
		 * @param original
		 */
		public WorkingCopyOperation(IApiBaseline original) {
			this.original = original;
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core.runtime.IProgressMonitor)
		 */
		public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
			try {
				IApiComponent[] components = original.getApiComponents();
				IProgressMonitor localmonitor = SubMonitor.convert(monitor, WizardMessages.ApiProfileWizardPage_create_working_copy, components.length + 1);
				localmonitor.subTask(WizardMessages.ApiProfileWizardPage_copy_profile_attribs);
				workingcopy = ApiModelFactory.newApiBaseline(original.getName(), original.getLocation());
				localmonitor.worked(1);
				localmonitor.subTask(WizardMessages.ApiProfileWizardPage_copy_api_components);
				ArrayList comps = new ArrayList();
				IApiComponent comp = null;
				for(int i = 0; i < components.length; i++) {
					comp = ApiModelFactory.newApiComponent(workingcopy, components[i].getLocation());
					if(comp != null) {
						comps.add(comp);
					}
					localmonitor.worked(1);
				}
				workingcopy.addApiComponents((IApiComponent[]) comps.toArray(new IApiComponent[comps.size()]));
			}
			catch(CoreException ce) {
				ApiUIPlugin.log(ce);
			}
		}
		
		/**
		 * Returns the newly created {@link IApiProfile} working copy or <code>null</code>
		 * @return the working copy or <code>null</code>
		 */
		public IApiBaseline getWorkingCopy() {
			return workingcopy;
		}
	}
	
	IApiBaseline fProfile = null;
	private String originalname = null;
	/**
	 * Flag to know if the baselines' content has actually changed, or just some other attribute
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=267875
	 */
	boolean contentchange = false;
	
	/**
	 * We need to know if we are initializing the page to not respond to changed events
	 * causing validation when the wizard opens.
	 * 
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=266597
	 */
	private boolean initializing = false;
	
	/**
	 * widgets
	 */
	private Text nametext = null;
	private TreeViewer treeviewer = null;
	Combo locationcombo = null;
	private Button browsebutton = null,
				   reloadbutton = null;
	/**
	 * Constructor
	 * @param profile
	 */
	protected ApiBaselineWizardPage(IApiBaseline profile) {
		super(WizardMessages.ApiProfileWizardPage_1);
		this.fProfile = profile;
		setTitle(WizardMessages.ApiProfileWizardPage_1);
		if(profile == null) {
			setMessage(WizardMessages.ApiProfileWizardPage_3);
		}
		else {
			setMessage(WizardMessages.ApiProfileWizardPage_4);
		}
		setImageDescriptor(ApiUIPlugin.getImageDescriptor(IApiToolsConstants.IMG_WIZBAN_PROFILE));
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite comp = SWTFactory.createComposite(parent, 4, 1, GridData.FILL_HORIZONTAL);
		SWTFactory.createWrapLabel(comp, WizardMessages.ApiProfileWizardPage_5, 1);
		nametext = SWTFactory.createText(comp, SWT.BORDER | SWT.SINGLE, 3, GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL | GridData.BEGINNING);
		nametext.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				setPageComplete(pageValid());
			}
		});
		
		IExecutionEnvironment[] envs = JavaRuntime.getExecutionEnvironmentsManager().getExecutionEnvironments();
		ArrayList items = new ArrayList();
		for(int i = 0; i < envs.length; i++) {
			if(envs[i].getCompatibleVMs().length > 0) {
				items.add(envs[i].getId());
			}
		}
		Collections.sort(items, new Comparator() {
			public int compare(Object o1, Object o2) {
				return ((String) o1).compareTo((String) o2);
			}
		});		
				
		SWTFactory.createVerticalSpacer(comp, 1);
		
		SWTFactory.createWrapLabel(comp, WizardMessages.ApiProfileWizardPage_9, 1);
		locationcombo = SWTFactory.createCombo(comp, SWT.BORDER | SWT.SINGLE, 1, GridData.GRAB_HORIZONTAL | GridData.FILL_HORIZONTAL | GridData.BEGINNING, null);
		locationcombo.addModifyListener(new ModifyListener() {
			public void modifyText(ModifyEvent e) {
				setPageComplete(pageValid());
				updateButtons();
			}
		});
		browsebutton = SWTFactory.createPushButton(comp, WizardMessages.ApiProfileWizardPage_10, null);
		browsebutton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				DirectoryDialog dialog = new DirectoryDialog(getShell());
				dialog.setMessage(WizardMessages.ApiProfileWizardPage_11);
				String loctext = locationcombo.getText().trim();
				if (loctext.length() > 0) {
					dialog.setFilterPath(loctext);
				}
				String newPath = dialog.open();
				if (newPath != null && (!new Path(loctext).equals(new Path(newPath))
						|| getCurrentComponents().length == 0)) {
					/*
					 * If the path is identical, but there is no component loaded, we still
					 * want to reload. This might be the case if the combo is initialized by
					 * copy/paste with a path that points to a plugin directory
					 */
					locationcombo.setText(newPath);
					setErrorMessage(null);
					doReload();
				}
			}
		});
		
		reloadbutton = SWTFactory.createPushButton(comp, WizardMessages.ApiProfileWizardPage_12, null);
		reloadbutton.setEnabled(locationcombo.getText().trim().length() > 0);
		reloadbutton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				doReload();
			}
		});
		
		SWTFactory.createWrapLabel(comp, WizardMessages.ApiProfileWizardPage_13, 4);
		Tree tree = new Tree(comp, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 250;
		gd.horizontalSpan = 4;
		tree.setLayoutData(gd);
		treeviewer = new TreeViewer(tree);
		treeviewer.setLabelProvider(new ApiToolsLabelProvider());
		treeviewer.setContentProvider(new ContentProvider());
		treeviewer.setComparator(new ViewerComparator());
		treeviewer.setInput(getCurrentComponents());
		treeviewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				updateButtons();
			}
		});
		treeviewer.addFilter(new ViewerFilter() {
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				if(element instanceof IApiComponent) {
					IApiComponent component = (IApiComponent) element;
					try {
						if(component.isSourceComponent() || component.isSystemComponent()) {
							return false;
						}
					} catch (CoreException e) {
						ApiPlugin.log(e);
					}
					return true;
				}
				return !(element instanceof SystemLibraryApiComponent);
			}
		});
	
		setControl(comp);
		setPageComplete(fProfile != null);
		initialize();
		PlatformUI.getWorkbench().getHelpSystem().setHelp(comp, IApiToolsHelpContextIds.APIPROFILES_WIZARD_PAGE);
		Dialog.applyDialogFont(comp);
	}
	
	/**
	 * Initializes the controls of the page if the profile is not <code>null</code>
	 */
	protected void initialize() {
		initializing = true;
		try {
			if(fProfile != null) {
				originalname = fProfile.getName();
				WorkingCopyOperation op = new WorkingCopyOperation(fProfile);
				try {
					getContainer().run(false, false, op);
				} catch (InvocationTargetException e) {
					ApiUIPlugin.log(e);
				} catch (InterruptedException e) {
					ApiUIPlugin.log(e);
				}
				fProfile = op.getWorkingCopy();
				nametext.setText(fProfile.getName());
				IApiComponent[] components = fProfile.getApiComponents();
				HashSet locations = new HashSet();
				String loc = fProfile.getLocation();
				IPath location = null;
				if (loc != null) {
					location = new Path(loc);
					// check if the location is a file
					if(location.toFile().isDirectory()) {
						locations.add(location.removeTrailingSeparator().toOSString());
					}
				} else {
					for(int i = 0; i < components.length; i++) {
						if(!components[i].isSystemComponent()) {
							location = new Path(components[i].getLocation()).removeLastSegments(1);
							if(location.toFile().isDirectory()) {
								locations.add(location.removeTrailingSeparator().toOSString());
							}
						}
					}
				}
				if(locations.size() > 0) {
					locationcombo.setItems((String[]) locations.toArray(new String[locations.size()]));
					locationcombo.select(0);
				}
			}
			else {
				//try to set the default location to be the current install directory
				//https://bugs.eclipse.org/bugs/show_bug.cgi?id=258969
				Location location = Platform.getInstallLocation();
				if(location != null) {
					URL url = location.getURL();
					IPath path = new Path(url.getFile()).removeTrailingSeparator();
					if(path.toFile().exists()) {
						locationcombo.add(path.toOSString());
						locationcombo.select(0);
					}
				}
			}
		}
		finally {
			initializing = false;
		}
	}
	
	/**
	 * Reloads all of the plugins from the location specified in the location text field.
	 */
	protected void doReload() {
		ReloadOperation op = new ReloadOperation(nametext.getText().trim(), locationcombo.getText().trim());
		try {
			getContainer().run(true, true, op);
			treeviewer.setInput(getCurrentComponents());
			treeviewer.refresh();
			setPageComplete(pageValid());
		}
		catch (InvocationTargetException ite) {}
		catch (InterruptedException ie) {}
	}
	
	/**
	 * @return if the page is valid, such that it is considered complete and can be 'finished'
	 */
	protected boolean pageValid() {
		if(initializing) {
			return false;
		}
		setErrorMessage(null);
		if(!isNameValid(nametext.getText().trim())) {
			return false;
		}
		String text = locationcombo.getText().trim();
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
		if(fProfile != null) {	
			if (fProfile.getApiComponents().length == 0) {
				setErrorMessage(WizardMessages.ApiProfileWizardPage_2);
				return false;
			}
			IStatus status = fProfile.getExecutionEnvironmentStatus();
			if (status.getSeverity() == IStatus.ERROR) {
				setErrorMessage(status.getMessage());
				return false;
			}
		}
		else {
			setErrorMessage(WizardMessages.ApiProfileWizardPage_location_needs_reset);
			return false;
		}
		return true;
	}
	
	/**
	 * @param name
	 * @return
	 */
	private boolean isNameValid(String name) {
		if(name.length() < 1) {
			setErrorMessage(WizardMessages.ApiProfileWizardPage_20);
			return false;
		}
		if(!name.equals(originalname) && (((ApiBaselineManager)ApiPlugin.getDefault().getApiBaselineManager()).isExistingProfileName(name) &&
				!ApiBaselinePreferencePage.isRemovedBaseline(name))) {
			setErrorMessage(WizardMessages.ApiProfileWizardPage_profile_with_that_name_exists);
			return false;
		}
		IStatus status = ResourcesPlugin.getWorkspace().validateName(name, IResource.FILE);
		if(!status.isOK()) {
			setErrorMessage(status.getMessage());
			return false;
		}
		return true;
	}
	
	/**
	 * Returns the current API components in the baseline or an empty collection if none.
	 * 
	 * @return the current API components in the baseline or an empty collection if none
	 */
	protected IApiComponent[] getCurrentComponents() {
		if (fProfile != null) {
			return fProfile.getApiComponents();
		}
		return new IApiComponent[0];
	}
	
	/**
	 * Updates the state of a variety of buttons on this page
	 */
	protected void updateButtons() {
		String loctext = locationcombo.getText().trim();
		reloadbutton.setEnabled(loctext.length() > 0);
	}
	
	/**
	 * Creates or edits the profile and returns it
	 * @return a new {@link IApiProfile} or <code>null</code> if an error was encountered
	 * creating the new profile
	 */
	public IApiBaseline finish() throws IOException, CoreException {
		if(fProfile != null) {
			fProfile.setName(nametext.getText().trim());
		}	
		return fProfile;
	}
	
	/**
	 * @return if the actual content of the base line has changed and not just some
	 * other attribute
	 */
	public boolean contentChanged() {
		return this.contentchange;
	}
	
	/**
	 * Cleans up the working copy if the page is canceled
	 */
	public void cancel() {
		if(fProfile != null) {
			fProfile.dispose();
		}
	}
}
