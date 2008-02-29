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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.jface.dialogs.PageChangedEvent;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.api.tools.internal.builder.ApiUseAnalyzer.CompatibilityResult;
import org.eclipse.pde.api.tools.internal.provisional.search.IReference;
import org.eclipse.pde.api.tools.ui.internal.ApiToolsLabelProvider;
import org.eclipse.pde.api.tools.ui.internal.SWTFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.ui.model.WorkbenchViewerComparator;

/**
 * Displays results of compatibility analysis.
 * 
 * @since 1.0
 */
public class CompatibilityResultsPage extends WizardPage implements IPageChangedListener {
	
	/**
	 * Viewer
	 */
	private CheckboxTreeViewer fViewer;
	
	/**
	 * Content provider for check box tree viewer. Displays each result and
	 * unresolved references
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
			if (parentElement instanceof List) {
				List list = (List) parentElement;
				return list.toArray();
			}
			if (parentElement instanceof CompatibilityResult) {
				CompatibilityResult result = (CompatibilityResult) parentElement;
				IReference[] references = result.getUnresolvedReferences();
				Set unique = new HashSet();
				for (int i = 0; i < references.length; i++) {
					unique.add(references[i].getReferencedLocation());
				}
				return unique.toArray();
			}
			return new Object[0];
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
		 */
		public Object getParent(Object element) {
			if (element instanceof CompatibilityResult) {
				return fInput; 
			}
			return null;
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
		 */
		public boolean hasChildren(Object element) {
			if (element instanceof CompatibilityResult) {
				return ((CompatibilityResult)element).getUnresolvedReferences().length > 0;
			}
			if (element instanceof List) {
				return !((List)element).isEmpty();
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
	

	public CompatibilityResultsPage() {
		super(Messages.CompatibilityResultsPage_0, Messages.CompatibilityResultsPage_1, null);
		setMessage(Messages.CompatibilityResultsPage_2);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite comp = SWTFactory.createComposite(parent, 1, 1, GridData.FILL_HORIZONTAL);
		SWTFactory.createWrapLabel(comp, Messages.CompatibilityResultsPage_3, 1);
		Tree tree = new Tree(comp, SWT.BORDER | SWT.SINGLE | SWT.CHECK );
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = 300;
		tree.setLayoutData(gd);
		fViewer = new CheckboxTreeViewer(tree);
		fViewer.setLabelProvider(new ApiToolsLabelProvider());
		fViewer.setComparator(new WorkbenchViewerComparator());
		fViewer.setContentProvider(new ContentProvider());
		fViewer.setInput(((CompatibleVersionsWizard)getWizard()).getResults());
		fViewer.expandAll();
		setControl(comp);
		((WizardDialog)getWizard().getContainer()).addPageChangedListener(this);
	}
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IPageChangedListener#pageChanged(org.eclipse.jface.dialogs.PageChangedEvent)
	 */
	public void pageChanged(PageChangedEvent event) {
		if (this.equals(event.getSelectedPage())) {
			fViewer.refresh();
		}	
	}

}
