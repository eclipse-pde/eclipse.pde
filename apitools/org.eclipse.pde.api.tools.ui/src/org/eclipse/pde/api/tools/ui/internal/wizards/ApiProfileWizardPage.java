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
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.api.tools.internal.SystemLibraryApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.Factory;
import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.IApiProfile;
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
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.PlatformUI;

import com.ibm.icu.text.MessageFormat;

/**
 * The wizard page allowing a new API profiles to be created
 * or an existing one to be edited
 * @since 1.0.0
 */
public class ApiProfileWizardPage extends WizardPage {
	
	public class EEEntry {
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
	
	class ContentProvider implements ITreeContentProvider {

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
		 */
		public Object[] getChildren(Object parentElement) {
			if(parentElement instanceof IApiComponent) {
				IApiComponent component = (IApiComponent) parentElement;
				String[] ees = component.getExecutionEnvironments();
				ArrayList entries = new ArrayList(ees.length);
				for(int i = 0; i < ees.length; i++) {
					entries.add(new EEEntry(ees[i]));
				}
				return entries.toArray();
			}
			return null;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
		 */
		public boolean hasChildren(Object element) {
			if(element instanceof IApiComponent) {
				IApiComponent component = (IApiComponent) element;
				return component.getExecutionEnvironments().length > 0;
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
		private String location, name, eeid;
	
		/**
		 * Constructor
		 * @param platformPath
		 */
		public ReloadOperation(String name, String eeid, String location) {
			this.location = location;
			this.name = name;
			this.eeid = eeid;
		}
			
		/* (non-Javadoc)
		 * @see org.eclipse.jface.operation.IRunnableWithProgress#run(org.eclipse.core.runtime.IProgressMonitor)
		 */
		public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {	
			monitor.beginTask(WizardMessages.ApiProfileWizardPage_0, 10);
			URL[] urls = PluginPathFinder.getPluginPaths(location);	
			monitor.worked(1);
			try {
				File eeFile = Util.createEEFile(eeid);
				fProfile = Factory.newApiProfile(name, eeFile);
			} catch (CoreException e) {
				throw new InvocationTargetException(e);
			} catch (IOException e) {
				throw new InvocationTargetException(e);
			}
			SubProgressMonitor subMonitor = new SubProgressMonitor(monitor, 8);
			subMonitor.beginTask("", urls.length); //$NON-NLS-1$
			fEEset.clear();
			fRecommendedEE = null;
			List components = new ArrayList();
			String[] ees = null;
			for (int i = 0; i < urls.length; i++) {
				try {
					IApiComponent component = fProfile.newApiComponent(urls[i].getFile());
					if (component != null) {
						components.add(component);
						ees = component.getExecutionEnvironments();
						for(int j = 0; j < ees.length; j++) {
							fEEset.add(ees[j]);
						}
					}
				} catch (CoreException e) {
					fEEset.clear();
					throw new InvocationTargetException(e);
				}
				subMonitor.worked(1);
			}
			subMonitor.done();
			fProfile.addApiComponents((IApiComponent[]) components.toArray(new IApiComponent[components.size()]));
			monitor.worked(1);
			monitor.done();
		}
	}
	
	private final ArrayList fOrderedEEs = new ArrayList(Arrays.asList(new String[] {"JavaSE-1.6", "J2SE-1.5", "J2SE-1.4", "J2SE-1.3", "J2SE-1.2", "JRE-1.1", "OSGi/Minimum-1.1", "OSGi/Minimum-1.0", "CDC-1.1/Foundation-1.1", "CDC-1.0/Foundation-1.0"})); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$ //$NON-NLS-8$ //$NON-NLS-9$ //$NON-NLS-10$
	private HashSet fEEset = new HashSet();
	private IApiProfile fProfile = null;
	private String fRecommendedEE = null;
	
	/**
	 * widgets
	 */
	private Text nametext = null;
	private TreeViewer treeviewer = null;
	private Combo eecombo = null,
				  locationcombo = null;
	private Button browsebutton = null,
				   reloadbutton = null;
	/**
	 * Constructor
	 * @param profile
	 */
	protected ApiProfileWizardPage(IApiProfile profile) {
		super(WizardMessages.ApiProfileWizardPage_1);
		this.fProfile = profile;
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
		IExecutionEnvironment[] envs = JavaRuntime.getExecutionEnvironmentsManager().getExecutionEnvironments();
		String[] items = new String[envs.length];
		for(int i = 0; i < envs.length; i++) {
			if(envs[i].getCompatibleVMs().length > 0) {
				items[i] = envs[i].getId();
			}
		}
		Arrays.sort(items, new Comparator() {
			public int compare(Object o1, Object o2) {
				return ((String)o1).compareTo(o2);
			}
		});		
		
		SWTFactory.createWrapLabel(ncomp, WizardMessages.ApiProfileWizardPage_8, 1, 100);
		eecombo = SWTFactory.createCombo(ncomp, SWT.READ_ONLY | SWT.BORDER | SWT.SINGLE | SWT.DROP_DOWN, 1, GridData.FILL_HORIZONTAL, items);
		eecombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				pageValid();
			}
		});
		
		SWTFactory.createVerticalSpacer(comp, 1);
		
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
					doReload();
				}
			}
		});
		
		reloadbutton = SWTFactory.createPushButton(tcomp, WizardMessages.ApiProfileWizardPage_12, null);
		reloadbutton.setEnabled(locationcombo.getText().trim().length() > 0);
		reloadbutton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				doReload();
			}
		});
		
		SWTFactory.createWrapLabel(comp, WizardMessages.ApiProfileWizardPage_13, 2);
		Tree tree = new Tree(comp, SWT.BORDER | SWT.MULTI | SWT.FULL_SELECTION);
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.heightHint = 250;
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
				return !(element instanceof SystemLibraryApiComponent);
			}
		});
	
		setControl(comp);
		setPageComplete(fProfile != null);
		initialize();
		PlatformUI.getWorkbench().getHelpSystem().setHelp(comp, IApiToolsHelpContextIds.APIPROFILES_WIZARD_PAGE);
	}
	
	/**
	 * Initializes the controls of the page if the profile is not <code>null</code>
	 */
	protected void initialize() {
		if(fProfile != null) {
			nametext.setText(fProfile.getName());
			eecombo.setText(fProfile.getExecutionEnvironment());
			IApiComponent[] components = fProfile.getApiComponents();
			HashSet locations = new HashSet();
			IPath location = null;
			for(int i = 0; i < components.length; i++) {
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
		ReloadOperation op = new ReloadOperation(nametext.getText().trim(), eecombo.getText().trim(), locationcombo.getText().trim());
		try {
			getContainer().run(false, true, op);
			fRecommendedEE = resolveEE();
			int idx = eecombo.indexOf(fRecommendedEE);
			eecombo.select((idx > -1 ? idx : 0));
			treeviewer.setInput(getCurrentComponents());
			treeviewer.refresh();
		} 
		catch (InvocationTargetException ite) {} 
		catch (InterruptedException ie) {}
	}
	
	/**
	 * Returns the smallest ee that covers all of the required ee's for all of the {@link IApiComponent}s for this profile
	 * @return the ee to cover all of the components of this profile
	 */
	private String resolveEE() {
		if(fEEset.size() == 1) {
			return (String) fEEset.iterator().next();
		}
		else if(fEEset.size() > 1) {
			for(int i = 0; i < fOrderedEEs.size(); i++) {
				if(fEEset.contains(fOrderedEEs.get(i))) {
					return (String) fOrderedEEs.get(i);
				}
			}
		}
		return Util.getDefaultEEId();
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
		String ee = eecombo.getText();
		if(fRecommendedEE != null && fOrderedEEs.indexOf(ee) > fOrderedEEs.indexOf(fRecommendedEE)) {
			setErrorMessage(MessageFormat.format(WizardMessages.ApiProfileWizardPage_ee_of_X_required, new String[] {fRecommendedEE}));
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
	public IApiProfile finish() throws IOException, CoreException {
		String eeid = eecombo.getText().trim();
		if(!fProfile.getExecutionEnvironment().equals(eeid)) {
			File eefile = Util.createEEFile(eeid);
			fProfile.setExecutionEnvironment(eefile);
		}
		fProfile.setName(nametext.getText().trim());
		return fProfile;
	}
}
