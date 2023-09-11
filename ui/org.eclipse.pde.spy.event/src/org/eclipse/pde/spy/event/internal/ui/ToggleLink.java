/*******************************************************************************
 * Copyright (c) 2013, 2022 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.spy.event.internal.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;

public class ToggleLink {
	private final Link link;

	private ClickListener listener;

	private String[] text = { "", "" }; //$NON-NLS-1$ //$NON-NLS-2$

	public interface ClickListener {
		void clicked(boolean toggled);
	}

	public ToggleLink(Composite parent) {
		link = new Link(parent, SWT.NONE);
		link.setSize(SWT.DEFAULT, SWT.DEFAULT);
		link.addListener(SWT.Selection, event -> {
			updateText();
			if (listener != null) {
				listener.clicked(isToggled());
			}
		});
	}

	private void updateText() {
		String textToUpdate = link.getText().contains(text[0]) ? text[1] : text[0];
		setText(textToUpdate);
	}

	private void setText(String text) {
		link.setText(String.format("<a>%s</a>", text)); //$NON-NLS-1$
	}

	public void setClickListener(ClickListener listener) {
		this.listener = listener;
	}

	public void setText(String[] text /* normal text, toggle text */) {
		this.text = text;
		setText(isToggled() ? text[1] : text[0]);
	}

	public Control getControl() {
		return link;
	}

	private boolean isToggled() {
		return link.getText().contains(text[1]);
	}
}
