/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.templates;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 * Implementation of the AbstractTemplateOption that allows users to choose a value from
 * the fixed set of options using a combo box.
 * 
 * @since 3.2M5
 */
public class ComboChoiceOption extends AbstractChoiceOption {
	
	private Combo fCombo;
	private Label fLabel;
	
	public ComboChoiceOption(BaseOptionTemplateSection section, String name, String label, String[][] choices) {
		super(section, name, label, choices);
	}
	
	public void createControl(Composite parent, int span) {
		fLabel = createLabel(parent, 1);
		fLabel.setEnabled(isEnabled());
		fill(fLabel, 1);
		
		fCombo = new Combo(parent, SWT.READ_ONLY);
		fill(fCombo, 1);
		for (int i = 0; i < fChoices.length; i++) {
			String[] choice = fChoices[i];
			fCombo.add(choice[1], i);
			fCombo.setEnabled(isEnabled());
		}
		fCombo.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (isBlocked())
					return;
				if (fCombo.getSelectionIndex() != -1) {
					String[] choice = fChoices[fCombo.getSelectionIndex()];
					setValue(choice[0]);
					getSection().validateOptions(ComboChoiceOption.this);
				}
			}
		});
		
		if (getChoice() != null)
			selectChoice(getChoice());
	}
	
	protected void setOptionValue(Object value) {
		if (fCombo != null && value != null) {
			selectChoice(value.toString());
		}
	}
	
	protected void setOptionEnabled(boolean enabled) {
		if (fLabel != null) {
			fLabel.setEnabled(enabled);
			fCombo.setEnabled(enabled);
		}
	}
	
	protected void selectOptionChoice(String choice) {
		fCombo.setText(choice);
		if (fCombo.getSelectionIndex() == -1)
			fCombo.select(0);
	}
}
