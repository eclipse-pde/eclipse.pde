/*******************************************************************************
 *  Copyright (c) 2006, 2015 IBM Corporation and others.
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
package org.eclipse.pde.ui.templates;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import org.eclipse.core.runtime.Assert;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;

/**
 * Implementation of the AbstractTemplateOption that allows users to choose a value from
 * the fixed set of options using radio buttons.
 *
 * @since 3.2
 */
public class RadioChoiceOption extends AbstractChoiceOption {

	private Button[] fButtons;
	private Label fLabel;

	/**
	 * Constructor for RadioChoiceOption.
	 * Number of choices must be 2, otherwise an assertion will fail.
	 *
	 * @param section
	 *            the parent section.
	 * @param name
	 *            the unique name
	 * @param label
	 *            the presentable label
	 * @param choices
	 *            the list of choices from which the value can be chosen. This
	 *            list must be of size 2.
	 *            Each array entry should be an array of size 2, where position 0
	 *            will be interpeted as the choice unique name, and position 1
	 *            as the choice presentable label.
	 */
	public RadioChoiceOption(BaseOptionTemplateSection section, String name, String label, String[][] choices) {
		super(section, name, label, choices);
		Assert.isTrue(choices.length == 2);
	}

	private Button createRadioButton(Composite parent, int span, String[] choice) {
		Button button = new Button(parent, SWT.RADIO);
		button.setData(choice[0]);
		button.setText(choice[1]);
		GridData gd = fill(button, span);
		gd.horizontalIndent = 10;
		return button;
	}

	@Override
	public void createControl(Composite parent, int span) {

		fLabel = createLabel(parent, 1);
		fLabel.setEnabled(isEnabled());
		fill(fLabel, span);

		Composite radioComp = createComposite(parent, span);
		GridData gd = fill(radioComp, span);
		gd.horizontalIndent = 10;
		GridLayout layout = new GridLayout(fChoices.length, true);
		layout.marginWidth = layout.marginHeight = 0;
		radioComp.setLayout(layout);

		fButtons = new Button[fChoices.length];

		SelectionListener listener = widgetSelectedAdapter(e -> {
			Button b = (Button) e.widget;
			if (isBlocked())
				return;
			if (b.getSelection()) {
				setValue(b.getData().toString());
				getSection().validateOptions(RadioChoiceOption.this);
			}
		});

		for (int i = 0; i < fChoices.length; i++) {
			fButtons[i] = createRadioButton(radioComp, 1, fChoices[i]);
			fButtons[i].addSelectionListener(listener);
			fButtons[i].setEnabled(isEnabled());
		}

		if (getChoice() != null)
			selectChoice(getChoice());
	}

	@Override
	protected void setOptionValue(Object value) {
		if (fButtons != null && value != null) {
			selectChoice(value.toString());
		}
	}

	@Override
	protected void setOptionEnabled(boolean enabled) {
		if (fLabel != null) {
			fLabel.setEnabled(enabled);
			for (Button button : fButtons) {
				button.setEnabled(enabled);
			}
		}
	}

	@Override
	protected void selectOptionChoice(String choice) {
		for (Button button : fButtons) {
			String bname = button.getData().toString();
			if (bname.equals(choice)) {
				button.setSelection(true);
			} else {
				button.setSelection(false);
			}
		}
	}
}
