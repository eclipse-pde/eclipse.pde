/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.parts;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.forms.widgets.FormToolkit;


public abstract class SharedPartWithButtons extends SharedPart {
	private String[] fButtonLabels;
	private Control[] fControls;
	protected Composite fButtonContainer;
	private class SelectionHandler implements SelectionListener {
		public void widgetSelected(SelectionEvent e) {
			buttonSelected(e);
		}
		public void widgetDefaultSelected(SelectionEvent e) {
			buttonSelected(e);
		}
		private void buttonSelected(SelectionEvent e) {
			Integer index = (Integer) e.widget.getData();
			SharedPartWithButtons.this.buttonSelected((Button) e.widget, index
					.intValue());
		}
	}
	public SharedPartWithButtons(String[] buttonLabels) {
		fButtonLabels = buttonLabels;
	}
	public void setButtonEnabled(int index, boolean enabled) {
		if (fControls != null && index >= 0 && fControls.length > index) {
			Control c = fControls[index];
			if (c instanceof Button)
				c.setEnabled(enabled);
		}
	}
	protected abstract void createMainControl(Composite parent, int style,
			int span, FormToolkit toolkit);
	protected abstract void buttonSelected(Button button, int index);
	/*
	 * @see SharedPart#createControl(Composite, FormWidgetFactory)
	 */
	public void createControl(Composite parent, int style, int span,
			FormToolkit toolkit) {
		createMainLabel(parent, span, toolkit);
		createMainControl(parent, style, span - 1, toolkit);
		createButtons(parent, toolkit);
	}
	protected void createButtons(Composite parent, FormToolkit toolkit) {
		if (fButtonLabels != null && fButtonLabels.length > 0) {
			fButtonContainer = createComposite(parent, toolkit);
			GridData gd = new GridData(GridData.FILL_VERTICAL);
			fButtonContainer.setLayoutData(gd);
			fButtonContainer.setLayout(createButtonsLayout());
			fControls = new Control[fButtonLabels.length];
			SelectionHandler listener = new SelectionHandler();
			for (int i = 0; i < fButtonLabels.length; i++) {
				String label = fButtonLabels[i];
				if (label != null) {
					Button button = createButton(fButtonContainer, label, i,
							toolkit);
					button.addSelectionListener(listener);
					fControls[i] = button;
				} else {
					createEmptySpace(fButtonContainer, 1, toolkit);
				}
			}
		}		
	}
	protected GridLayout createButtonsLayout() {
		GridLayout layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 0;
		return layout;
	}
	protected Button createButton(Composite parent, String label, int index,
			FormToolkit toolkit) {
		Button button;
		if (toolkit != null)
			button = toolkit.createButton(parent, label, SWT.PUSH);
		else {
			button = new Button(parent, SWT.PUSH);
			button.setText(label);
		}
		GridData gd = new GridData(GridData.FILL_HORIZONTAL
				| GridData.VERTICAL_ALIGN_BEGINNING);
		button.setLayoutData(gd);
		button.setData(new Integer(index));
		return button;
	}
	protected void updateEnabledState() {
		for (int i = 0; i < fControls.length; i++) {
			Control c = fControls[i];
			if (c instanceof Button)
				c.setEnabled(isEnabled());
		}
	}
	protected void createMainLabel(Composite parent, int span,
			FormToolkit toolkit) {
	}
}