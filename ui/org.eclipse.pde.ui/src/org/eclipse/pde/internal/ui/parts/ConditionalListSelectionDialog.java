/*******************************************************************************
 *  Copyright (c) 2005, 2015 IBM Corporation and others.
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
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.eclipse.ui.dialogs.FilteredList;
import org.eclipse.ui.dialogs.FilteredList.FilterMatcher;
import org.eclipse.ui.internal.misc.StringMatcher;

public class ConditionalListSelectionDialog extends ElementListSelectionDialog {

	private String fButtonText;
	private Object[] fElements;
	private Object[] fConditionalElements;

	public ConditionalListSelectionDialog(Shell parent, ILabelProvider renderer, String buttonText) {
		super(parent, renderer);
		fButtonText = buttonText;
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
		return comp;
	}

	@Override
	public void setElements(Object[] elements) {
		super.setElements(elements);
		fElements = elements;
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

}
