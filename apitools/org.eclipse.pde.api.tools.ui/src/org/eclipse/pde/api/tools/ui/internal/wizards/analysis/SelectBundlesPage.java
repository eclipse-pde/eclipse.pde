/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.api.tools.ui.internal.wizards.analysis;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.api.tools.internal.builder.ApiUseAnalyzer;
import org.eclipse.pde.api.tools.internal.builder.ApiUseAnalyzer.CompatibilityResult;
import org.eclipse.pde.api.tools.internal.provisional.ApiPlugin;
import org.eclipse.pde.api.tools.internal.provisional.IApiComponent;
import org.eclipse.pde.api.tools.internal.provisional.IApiProfile;
import org.eclipse.pde.api.tools.ui.internal.ApiToolsLabelProvider;
import org.eclipse.pde.api.tools.ui.internal.ApiUIPlugin;
import org.eclipse.pde.api.tools.ui.internal.SWTFactory;
import org.eclipse.pde.core.plugin.IPluginImport;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.model.WorkbenchViewerComparator;

/**
 * Page to select bundles to analyze for compatible version range.
 * 
 * @since 1.0
 */
public class SelectBundlesPage extends WizardPage implements IPageChangingListener {
	
	private CheckboxTreeViewer fViewer;
	
	/**
	 * Content provider for check box tree viewer. Displays each profile and
	 * API components corresponding to imported (required) bundles
	 */
	private class ContentProvider implements ITreeContentProvider {
		
		/**
		 * Viewer input (workspace API profiles array)
		 */
		private Object fInput;

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
		 */
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof IApiProfile[]) {
				return (IApiProfile[])parentElement;
			}
			if (parentElement instanceof IApiProfile) {
				IPluginImport[] imports = getPluginModel().getPluginBase().getImports();
				String[] ids = new String[imports.length];
				for (int i = 0; i < imports.length; i++) {
					ids[i] = imports[i].getId();
				}
				final List components = new ArrayList();
				IApiProfile profile = (IApiProfile) parentElement;
				for (int i = 0; i < ids.length; i++) {
					String id = ids[i];
					IApiComponent component = profile.getApiComponent(id);
					if (component != null) {
						components.add(component);
					}
				}
				return (IApiComponent[]) components.toArray(new IApiComponent[components.size()]);
			}
			return new Object[0];
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
		 */
		public Object getParent(Object element) {
			if (element instanceof IApiComponent) {
				return ((IApiComponent)element).getProfile(); 
			}
			return fInput;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
		 */
		public boolean hasChildren(Object element) {
			if (element instanceof IApiProfile) {
				return true;
			}
			if (element instanceof IApiProfile[]) {
				return true;
			}
			return false;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
		 */
		public Object[] getElements(Object inputElement) {
			return getChildren(inputElement);
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
		 */
		public void dispose() {
			fInput = null;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
		 */
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			fInput = newInput;
		}
		
	}

	/**
	 * Constructs a new page
	 */
	public SelectBundlesPage() {
		super(Messages.SelectBundlesPage_0, Messages.SelectBundlesPage_1, null);
		setMessage(Messages.SelectBundlesPage_2);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite comp = SWTFactory.createComposite(parent, 1, 1, GridData.FILL_HORIZONTAL);
		SWTFactory.createWrapLabel(comp, Messages.SelectBundlesPage_3, 1);
		Tree tree = new Tree(comp, SWT.BORDER | SWT.SINGLE | SWT.CHECK );
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 300;
		tree.setLayoutData(gd);
		fViewer = new CheckboxTreeViewer(tree);
		fViewer.setLabelProvider(new ApiToolsLabelProvider());
		fViewer.setComparator(new WorkbenchViewerComparator());
		fViewer.setContentProvider(new ContentProvider());
		fViewer.setInput(ApiPlugin.getDefault().getApiProfileManager().getApiProfiles());
		fViewer.expandAll();
		fViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				Object element = event.getElement();
				if (element instanceof IApiProfile) {
					fViewer.setSubtreeChecked(element, event.getChecked());
				} else if (element instanceof IApiComponent) {
					IApiComponent component = (IApiComponent) element;
					ITreeContentProvider provider = (ITreeContentProvider) fViewer.getContentProvider();
					Object[] children = provider.getChildren(component.getProfile());
					int count = 0;
					for (int i = 0; i < children.length; i++) {
						if (fViewer.getChecked(children[i])) {
							count++;
						}	
					}
					if (count == 0) {
						fViewer.setSubtreeChecked(component.getProfile(), false);
					} else if (count == children.length) {
						fViewer.setGrayChecked(component.getProfile(), false);
						fViewer.setSubtreeChecked(component.getProfile(), true);
					} else {
						fViewer.setGrayChecked(component.getProfile(), true);
					}
				}
				updatePageComplete();
			}
		});
		Composite buttonComp = SWTFactory.createComposite(comp, 2, 1, GridData.FILL_HORIZONTAL);
		Button all = SWTFactory.createPushButton(buttonComp, Messages.SelectBundlesPage_4, null);
		all.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				IApiProfile[] profiles = ApiPlugin.getDefault().getApiProfileManager().getApiProfiles();
				for (int i = 0; i < profiles.length; i++) {
					fViewer.setSubtreeChecked(profiles[i], true);
				}
				updatePageComplete();
			}
		});
		Button none = SWTFactory.createPushButton(buttonComp, Messages.SelectBundlesPage_5, null);
		none.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				IApiProfile[] profiles = ApiPlugin.getDefault().getApiProfileManager().getApiProfiles();
				for (int i = 0; i < profiles.length; i++) {
					fViewer.setSubtreeChecked(profiles[i], false);
				}
				updatePageComplete();
			}
		});
		setControl(comp);
		updatePageComplete();
		((WizardDialog)getWizard().getContainer()).addPageChangingListener(this);
	}
	
	
	
	/**
	 * Returns the plug-in being analyzed.
	 * 
	 * @return plug-in being analyzed
	 */
	protected IPluginModelBase getPluginModel() {
		return ((CompatibleVersionsWizard)getWizard()).getPlugin();
	}

	/** 
	 * Updates if the page is complete
	 */
	protected void updatePageComplete() {
		Object[] elements = fViewer.getCheckedElements();
		for (int i = 0; i < elements.length; i++) {
			Object object = elements[i];
			if (object instanceof IApiComponent) {
				setPageComplete(true);
				return;
			}
		}
		setPageComplete(false);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IPageChangingListener#handlePageChanging(org.eclipse.jface.dialogs.PageChangingEvent)
	 */
	public void handlePageChanging(PageChangingEvent event) {
		if (!event.getCurrentPage().equals(this)) {
			return;
		}
		// Do the analysis
		final List components = new ArrayList();
		Object[] elements = fViewer.getCheckedElements();
		for (int i = 0; i < elements.length; i++) {
			if (elements[i] instanceof IApiComponent) {
				components.add(elements[i]);
			}
		}
		IRunnableWithProgress runnable = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				((CompatibleVersionsWizard)getWizard()).setResults(null);
				ApiUseAnalyzer analyzer = new ApiUseAnalyzer();
				IApiProfile workspaceProfile = ApiPlugin.getDefault().getApiProfileManager().getWorkspaceProfile();
				IApiComponent base = workspaceProfile.getApiComponent(getPluginModel().getPluginBase().getId());
				try {
					CompatibilityResult[] results = analyzer.analyzeCompatibility(base, (IApiComponent[]) components.toArray(new IApiComponent[components.size()]), monitor);
					((CompatibleVersionsWizard)getWizard()).setResults(results);
					if (results == null) {
						throw new InterruptedException();
					}
				} catch (CoreException e) {		
					throw new InvocationTargetException(e);
				}
			}
		};
		try {
			getWizard().getContainer().run(true, true, runnable);
		} catch (InvocationTargetException e) {
			Throwable exception = e.getTargetException();
			if (exception instanceof CoreException) {
				CoreException ce = (CoreException) e.getTargetException();
				ApiUIPlugin.log(ce.getStatus());
				setMessage(ce.getStatus().getMessage(), DialogPage.ERROR);
			} else {
				ApiUIPlugin.log(exception);
				setMessage(exception.getMessage(), DialogPage.ERROR);
			}
			event.doit = false;
		} catch (InterruptedException e) {
			event.doit = false;
		}
		
	}

	
}
