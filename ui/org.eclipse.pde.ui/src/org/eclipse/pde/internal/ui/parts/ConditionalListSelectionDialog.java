/*******************************************************************************
 *  Copyright (c) 2005, 2023 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.parts;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.text.StringMatcher;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.pde.internal.core.text.bundle.ExportPackageHeader;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.plugin.ImportPackageSection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.dialogs.FilteredList;
import org.eclipse.ui.dialogs.FilteredList.FilterMatcher;

public class ConditionalListSelectionDialog extends ElementListSelectionDialog {

	private final String fButtonText;
	private Object[] fElements;
	private Object[] fConditionalElements;
	private ExportPackageHeader exportPackageHeader;

	public ConditionalListSelectionDialog(Shell parent, ILabelProvider renderer, String buttonText) {
		super(parent, renderer);
		fButtonText = buttonText;
	}

	public ConditionalListSelectionDialog(Shell activeWorkbenchShell, ILabelProvider labelProvider,
			String exportPackageSection_dialogButtonLabel, ExportPackageHeader fHeader) {
		this(activeWorkbenchShell, labelProvider, exportPackageSection_dialogButtonLabel);
		exportPackageHeader = fHeader;
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite comp = (Composite) super.createDialogArea(parent);
		int size = ((fElements != null) ? fElements.length : 0) + ((fConditionalElements != null) ? fConditionalElements.length : 0);
		final Object[] allElements = new Object[size];
		int conditionalStart = 0;
		if (fElements != null) {
			System.arraycopy(fElements, 0, allElements, 0, fElements.length);
			conditionalStart = fElements.length;
		}
		if (fConditionalElements != null)
			System.arraycopy(fConditionalElements, 0, allElements, conditionalStart, fConditionalElements.length);

		final Button button = new Button(comp, SWT.CHECK);
		Assert.isNotNull(fButtonText);
		button.setText(fButtonText);
		button.addSelectionListener(widgetSelectedAdapter(e -> {
			if (button.getSelection())
				setListElements(allElements);
			else
				setListElements(fElements);
		}));
		button.setSelection(true);
		return comp;
	}

	@Override
	public void setElements(Object[] elements) {
		super.setElements(elements);
		fElements = elements;
	}

	@Override
	protected void updateOkState() {
		super.updateOkState();
		Button ok = getOkButton();
		if (ok != null && !ok.isEnabled() && fElements != null) {
			ok.setEnabled(fElements.length != 0);
		}
	}

	public void setConditionalElements(Object[] elements) {
		fConditionalElements = elements;
	}

	@Override
	protected FilteredList createFilteredList(Composite parent) {
		final FilteredList list = super.createFilteredList(parent);

		list.setFilterMatcher(new FilterMatcher() {
			private StringMatcher fMatcher;

			@Override
			public void setFilter(String pattern, boolean ignoreCase, boolean ignoreWildCards) {
				fMatcher = new StringMatcher('*' + pattern + '*', ignoreCase, ignoreWildCards);
			}

			@Override
			public boolean match(Object element) {
				return fMatcher.match(list.getLabelProvider().getText(element));
			}
		});

		return list;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		createButton(parent, IDialogConstants.OK_ID, PDEUIMessages.ManifestEditor_addActionText, true);
		createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
	}

	@Override
	protected void updateButtonsEnableState(IStatus status) {
		super.updateButtonsEnableState(status);
		Button okButton = getOkButton();
		Object[] currentSelection = fFilteredList.getSelection();
		if(currentSelection.length>0)
			okButton.setEnabled(false);
		for (Object selection : currentSelection) {
			if (selection instanceof ImportPackageSection.ImportItemWrapper) {
				if (!((ImportPackageSection.ImportItemWrapper) selection).isAlreadyImported()) {
				okButton.setEnabled(true);
				break;
			}
			}
			else if (selection instanceof IPackageFragment) {
				if (exportPackageHeader == null || exportPackageHeader.getPackageNames().isEmpty()) {
					okButton.setEnabled(true);
					break;
				}
				else if (!exportPackageHeader.getPackageNames()
								.contains(((IPackageFragment) selection).getElementName())) {
				okButton.setEnabled(true);
				break;
			}
		}	
		}
	}

}
