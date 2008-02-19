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
package org.eclipse.pde.api.tools.ui.internal.properties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.pde.api.tools.internal.provisional.IApiProblemFilter;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.ui.internal.IApiToolsConstants;
import org.eclipse.pde.api.tools.ui.internal.IApiToolsHelpContextIds;
import org.eclipse.pde.api.tools.ui.internal.SWTFactory;
import org.eclipse.pde.api.tools.ui.internal.properties.ApiFiltersPropertyPage.ApiKindDescription;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.ui.model.WorkbenchViewerComparator;

/**
 * Dialog used to select new {@link ApiKindDescription}s to add to an existing {@link IApiProblemFilter}
 * 
 * @since 1.0.0
 */
public class KindSelectionDialog extends SelectionDialog {

	/**
	 * Content provider for {@link ApiKindDescription}s
	 */
	class KindContentProvider implements IStructuredContentProvider {

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
		 */
		public Object[] getElements(Object inputElement) {
			ArrayList input = new ArrayList((Collection)inputElement);
			ArrayList kinds = new ArrayList();
			IElementDescriptor element = fFilter.getElement();
			input.removeAll(Arrays.asList(fFilter.getKinds()));
			if(element.getElementType() == IElementDescriptor.T_PACKAGE) {
				return input.toArray(new ApiKindDescription[input.size()]);
			}
			ApiKindDescription desc = null;
			for(int i = 0; i < input.size(); i++) {
				desc = (ApiKindDescription) input.get(i);
				if(desc.appliesTo(element)) {
					kinds.add(desc);
				}
			}
			return kinds.toArray(new ApiKindDescription[kinds.size()]);
		}
		public void dispose() {}
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {}
	}
	
	private CheckboxTableViewer fViewer = null;
	private Text fDescription = null;
	private IApiProblemFilter fFilter = null;

	/**
	 * Constructor
	 * @param parentShell
	 * @param element
	 */
	protected KindSelectionDialog(Shell parentShell, IApiProblemFilter filter) {
		super(parentShell);
		fFilter = filter;
		setTitle(PropertiesMessages.KindSelectionDialog_0);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createDialogArea(Composite parent) {
		Composite comp = (Composite) super.createDialogArea(parent);
		SWTFactory.createWrapLabel(comp, PropertiesMessages.KindSelectionDialog_1, 1);
		fViewer = CheckboxTableViewer.newCheckList(comp, SWT.BORDER | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL);
		fViewer.setLabelProvider(new KindLabelProvider());
		fViewer.setContentProvider(new KindContentProvider());
		fViewer.setInput(ApiFiltersPropertyPage.getAllKindDescriptions());
		fViewer.setComparator(new WorkbenchViewerComparator());
		fViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection ss = (IStructuredSelection) event.getSelection();
				ApiKindDescription desc = (ApiKindDescription) ss.getFirstElement();
				if(desc != null) {
					fDescription.setText(desc.description);
				}
				else {
					fDescription.setText(IApiToolsConstants.EMPTY_STRING);
				}
			}
		});
		fViewer.addCheckStateListener(new ICheckStateListener() {
			public void checkStateChanged(CheckStateChangedEvent event) {
				getOkButton().setEnabled(fViewer.getCheckedElements().length > 0);
			}
		});
		Table table = fViewer.getTable();
		table.setLayout(new GridLayout());
		GridData gd = new GridData(GridData.FILL_BOTH);
		gd.widthHint = 300;
		gd.heightHint = 200;
		table.setLayoutData(gd);
		addSelectionButtons(comp);
		SWTFactory.createVerticalSpacer(comp, 1);
		SWTFactory.createWrapLabel(comp, PropertiesMessages.KindSelectionDialog_2, 1);
		fDescription = SWTFactory.createText(comp, SWT.BORDER | SWT.WRAP | SWT.READ_ONLY, 1);
		gd = (GridData) fDescription.getLayoutData();
		gd.heightHint = 45;
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, IApiToolsHelpContextIds.APITOOLS_KIND_SELECTION_DIALOG);
		return super.createDialogArea(parent);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.dialogs.SelectionDialog#createButtonsForButtonBar(org.eclipse.swt.widgets.Composite)
	 */
	protected void createButtonsForButtonBar(Composite parent) {
		super.createButtonsForButtonBar(parent);
		getOkButton().setEnabled(false);
	}
	
	/**
     * Add the selection and de-selection buttons to the dialog.
     * @param composite 
     */
    private void addSelectionButtons(Composite parent) {
        Composite buttonComposite = SWTFactory.createComposite(parent, 1, 1, GridData.END, 0, 0);
        buttonComposite.setLayoutData(new GridData(SWT.END, SWT.TOP, true, false));

        Button selectButton = createButton(buttonComposite, IDialogConstants.SELECT_ALL_ID, PropertiesMessages.KindSelectionDialog_3, false);
        selectButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                fViewer.setAllChecked(true);
                getOkButton().setEnabled(true);
            }
        });

        Button deselectButton = createButton(buttonComposite, IDialogConstants.DESELECT_ALL_ID, PropertiesMessages.KindSelectionDialog_4, false);
        deselectButton.addSelectionListener(new SelectionAdapter() {
            public void widgetSelected(SelectionEvent e) {
                fViewer.setAllChecked(false);
                getOkButton().setEnabled(false);
            }
        });
    }
	
    /* (non-Javadoc)
     * @see org.eclipse.jface.dialogs.Dialog#okPressed()
     */
    protected void okPressed() {
    	setSelectionResult(fViewer.getCheckedElements());
    	super.okPressed();
    }
}
