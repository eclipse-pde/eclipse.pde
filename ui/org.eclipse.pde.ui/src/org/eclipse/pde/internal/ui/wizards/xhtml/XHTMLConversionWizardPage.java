/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.wizards.xhtml;

import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.wizards.xhtml.TocReplaceTable.TocReplaceEntry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;
import org.eclipse.ui.model.WorkbenchLabelProvider;


public class XHTMLConversionWizardPage extends WizardPage {

	private TocReplaceTable fTable;
	private ContainerCheckedTreeViewer fInputViewer;
	
	private class CP implements ITreeContentProvider {

		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof IFile)
				return fTable.getToBeConverted((IFile)parentElement);
			return null;
		}
		public Object getParent(Object element) {
			if (element instanceof TocReplaceEntry)
				return ((TocReplaceEntry)element).getTocFile();
			return null;
		}
		public boolean hasChildren(Object element) {
			return element instanceof IFile;
		}
		public Object[] getElements(Object inputElement) {
			return fTable.getTocs();
		}
		public void dispose() {
		}
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
	}
	
	protected XHTMLConversionWizardPage(TocReplaceTable table) {
		super("convert"); //$NON-NLS-1$
		setTitle(PDEUIMessages.XHTMLConversionWizardPage_title);
		setDescription(PDEUIMessages.XHTMLConversionWizardPage_desc);
		fTable = table;
	}

	public void createControl(Composite parent) {
		Composite columns = createComposite(parent, false, 2, false);
		columns.setLayoutData(new GridData(GridData.FILL_BOTH));
		
		Composite valid = createComposite(columns, true, 1, false);
		Label label = new Label(valid, SWT.NONE);
		label.setText(PDEUIMessages.XHTMLConversionWizardPage_viewerLabel);
		fInputViewer = new ContainerCheckedTreeViewer(valid, SWT.V_SCROLL | SWT.H_SCROLL | SWT.SINGLE | SWT.BORDER);
		fInputViewer.setContentProvider(new CP());
		fInputViewer.setLabelProvider(new WorkbenchLabelProvider());
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 400;
		gd.heightHint = 170;
		fInputViewer.getTree().setLayoutData(gd);
		fInputViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				setPageComplete(fInputViewer.getCheckedElements().length > 0);
			}
		});
		fInputViewer.setInput(new Object());
		fInputViewer.setAllChecked(true);
		
		Composite buttonComp = createComposite(columns, true, 1, true);
		Label blankLabel = new Label(buttonComp, SWT.NONE);
		blankLabel.setText(""); //$NON-NLS-1$
		Button selectAll = new Button(buttonComp, SWT.PUSH);
		selectAll.setText(PDEUIMessages.XHTMLConversionWizardPage_selectAll);
		selectAll.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		selectAll.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fInputViewer.setAllChecked(true);
				setPageComplete(true);
			}
		});
		Button deselectAll = new Button(buttonComp, SWT.PUSH);
		deselectAll.setText(PDEUIMessages.XHTMLConversionWizardPage_deselectAll);
		deselectAll.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		deselectAll.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				fInputViewer.setAllChecked(false);
				setPageComplete(false);
			}
		});
		
		setControl(columns);
		Dialog.applyDialogFont(columns);
	}
	
	protected Composite createComposite(Composite parent, boolean noMargin, int cols, boolean valignTop) {
		Composite comp = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(cols, false);
		if (noMargin)
			layout.marginHeight = layout.marginWidth = 0;
		comp.setLayout(layout);
		if (valignTop)
			comp.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
		else
			comp.setLayoutData(new GridData(GridData.FILL_BOTH));
		return comp;
	}

	protected TocReplaceEntry[] getCheckedEntries() {
		ArrayList list = new ArrayList();
		Object[] entries = fInputViewer.getCheckedElements();
		for (int i = 0; i < entries.length; i++) {
			if (entries[i] instanceof TocReplaceEntry)
				list.add(entries[i]);
		}
		return (TocReplaceEntry[]) list.toArray(new TocReplaceEntry[list.size()]);
	}
}
