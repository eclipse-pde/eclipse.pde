/*******************************************************************************
 *  Copyright (c) 2000, 2018 IBM Corporation and others.
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

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledFormText;

public class FormBrowser {
	FormToolkit toolkit;
	Composite container;
	ScrolledFormText formText;
	FormText ftext;
	String text;
	int style;

	public FormBrowser(int style) {
		this.style = style;
	}

	public void createControl(Composite parent) {
		toolkit = new FormToolkit(parent.getDisplay());
		int borderStyle = toolkit.getBorderStyle() == SWT.BORDER ? SWT.NULL : SWT.BORDER;
		container = new Composite(parent, borderStyle);
		FillLayout flayout = new FillLayout();
		flayout.marginWidth = 1;
		flayout.marginHeight = 1;
		container.setLayout(flayout);
		formText = new ScrolledFormText(container, SWT.V_SCROLL | SWT.H_SCROLL, false);
		if (borderStyle == SWT.NULL) {
			formText.setData(FormToolkit.KEY_DRAW_BORDER, FormToolkit.TREE_BORDER);
			toolkit.paintBordersFor(container);
		}
		ftext = toolkit.createFormText(formText, false);
		formText.setFormText(ftext);
		formText.setExpandHorizontal(true);
		formText.setExpandVertical(true);
		formText.setBackground(toolkit.getColors().getBackground());
		formText.setForeground(toolkit.getColors().getForeground());
		ftext.marginWidth = 2;
		ftext.marginHeight = 2;
		ftext.setHyperlinkSettings(toolkit.getHyperlinkGroup());
		formText.addDisposeListener(e -> {
			if (toolkit != null) {
				toolkit.dispose();
				toolkit = null;
			}
		});
		if (text != null)
			formText.setText(text);
	}

	public Control getControl() {
		return container;
	}

	public void setText(String text) {
		this.text = text;
		if (formText != null)
			formText.setText(text);
	}

	public void setEnabled(boolean enabled) {
		if (formText != null)
			formText.setEnabled(enabled);
		if (ftext != null)
			ftext.setEnabled(enabled);
	}
}
