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
import java.util.Iterator;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.StatusDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.pde.api.tools.internal.provisional.IApiProblemFilter;
import org.eclipse.pde.api.tools.internal.util.Util;
import org.eclipse.pde.api.tools.ui.internal.IApiToolsConstants;
import org.eclipse.pde.api.tools.ui.internal.IApiToolsHelpContextIds;
import org.eclipse.pde.api.tools.ui.internal.SWTFactory;
import org.eclipse.pde.api.tools.ui.internal.properties.ApiFiltersPropertyPage.ApiKindDescription;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;

/**
 * Dialog used to add / remove kinds from a filter
 * 
 * @since 1.0.0
 */
public class EditApiFilterDialog extends StatusDialog {

	/**
	 * The backing filter the edit with this dialog
	 */
	private IApiProblemFilter fBackingFilter = null;
	private TableViewer fViewer = null;
	private Button fAddButton = null, fRemoveButton = null;
	private Text fDescription = null;
	private ArrayList fInputKinds = null;
	
	private ArrayList fRemovedKinds = new ArrayList();
	private ArrayList fAddedKinds = new ArrayList();
	
	/**
	 * Constructor
	 * @param shell
	 */
	protected EditApiFilterDialog(Shell shell, IApiProblemFilter filter) {
		super(shell);
		setShellStyle(getShellStyle() | SWT.RESIZE);
		setTitle(PropertiesMessages.EditApiFilterDialog_0);
		fBackingFilter = filter;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createContents(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createContents(Composite parent) {
		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, IApiToolsHelpContextIds.APITOOLS_FILTERS_EDIT_DIALOG);
		return super.createContents(parent);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#createDialogArea(org.eclipse.swt.widgets.Composite)
	 */
	protected Control createDialogArea(Composite parent) {
		Composite tcomp = SWTFactory.createComposite(parent, 2, 1, GridData.FILL_HORIZONTAL, 10, 5);
		SWTFactory.createVerticalSpacer(tcomp, 1);
		SWTFactory.createWrapLabel(tcomp, PropertiesMessages.EditApiFilterDialog_1, 1);
		Text text = SWTFactory.createText(tcomp, SWT.SINGLE | SWT.BORDER | SWT.READ_ONLY, 1);
		text.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		String name = Util.getFormattedFilterName(fBackingFilter);
		if(name != null) {
			text.setText(name);
		}
		SWTFactory.createVerticalSpacer(tcomp, 1);
		Composite bodycomp = SWTFactory.createComposite(parent, 2, 1, GridData.FILL_BOTH, 10, 5);
		SWTFactory.createWrapLabel(bodycomp, PropertiesMessages.EditApiFilterDialog_2, 2);
		Table table = new Table(bodycomp, SWT.BORDER | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.widthHint = 250;
		gd.heightHint = 100;
		table.setLayoutData(gd);
		fViewer = new TableViewer(table);
		fViewer.setContentProvider(new ArrayContentProvider());
		fViewer.setLabelProvider(new KindLabelProvider());
		fInputKinds = new ArrayList();
		String[] kinds = fBackingFilter.getKinds();
		ApiKindDescription desc = null;
		for(int i = 0; i < kinds.length; i++) {
			desc = ApiFiltersPropertyPage.getDescription(kinds[i]);
			if(desc != null) {
				fInputKinds.add(desc);
			}
		}
		fViewer.setInput(fInputKinds);
		fViewer.addSelectionChangedListener(new ISelectionChangedListener() {
			public void selectionChanged(SelectionChangedEvent event) {
				IStructuredSelection ss = (IStructuredSelection) event.getSelection();
				fRemoveButton.setEnabled(ss.size() > 0);
				ApiKindDescription desc = (ApiKindDescription)ss.getFirstElement();
				if(desc != null) {
					fDescription.setText(desc.description);
				}
			}
		});
		
		Composite bcomp = SWTFactory.createComposite(bodycomp, 1, 1, GridData.FILL_VERTICAL, 0, 0);
		fAddButton = SWTFactory.createPushButton(bcomp, PropertiesMessages.EditApiFilterDialog_3, null);
		fAddButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				KindSelectionDialog dialog = new KindSelectionDialog(getShell(), fBackingFilter);
				if(dialog.open() == IDialogConstants.OK_ID) {
					Object[] result = dialog.getResult();
					if(result != null) {
						ApiKindDescription desc = null;
						for(int i = 0; i < result.length; i++) {
							desc = (ApiKindDescription) result[i];
							fAddedKinds.add(desc);
							fRemovedKinds.remove(desc);
							fInputKinds.add(desc);
						}
						fViewer.refresh();
					}
				}
			}
		});
		fRemoveButton = SWTFactory.createPushButton(bcomp, PropertiesMessages.EditApiFilterDialog_4, null);
		fRemoveButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				IStructuredSelection ss = (IStructuredSelection) fViewer.getSelection();
				ApiKindDescription desc = null;
				for(Iterator iter = ss.iterator(); iter.hasNext();) {
					desc = (ApiKindDescription) iter.next();
					fAddedKinds.remove(desc);
					fRemovedKinds.add(desc);
					fInputKinds.remove(desc);
					fViewer.refresh();
					fDescription.setText(IApiToolsConstants.EMPTY_STRING);
					fDescription.update();
				}
			}
		});
		fRemoveButton.setEnabled(false);

		SWTFactory.createVerticalSpacer(bodycomp, 1);
		SWTFactory.createWrapLabel(bodycomp, PropertiesMessages.EditApiFilterDialog_5, 2);
		fDescription = SWTFactory.createText(bodycomp, SWT.WRAP | SWT.READ_ONLY | SWT.BORDER, 2, GridData.FILL_HORIZONTAL);
		gd = (GridData) fDescription.getLayoutData();
		gd.heightHint = 50;
		return parent;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#okPressed()
	 */
	protected void okPressed() {
		ApiKindDescription desc = null;
		for(int i = 0; i < fRemovedKinds.size(); i++) {
			desc = (ApiKindDescription) fRemovedKinds.get(i);
			fBackingFilter.removeKind(desc.kind);
		}
		for(int i = 0; i < fAddedKinds.size(); i++) {
			desc = (ApiKindDescription) fAddedKinds.get(i);
			fBackingFilter.addKind(desc.kind);
		}
		super.okPressed();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.Dialog#cancelPressed()
	 */
	protected void cancelPressed() {
		fRemovedKinds.clear();
		fAddedKinds.clear();
		super.cancelPressed();
	}
	
	/**
	 * @return the edited filter
	 */
	public IApiProblemFilter getFilter() {
		return fBackingFilter;
	}
}
