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
import org.eclipse.swt.graphics.Color;
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
		// Use the CSS-themed parent colors; FormToolkit's are OS-based and stay
		// white in the dark theme.
		Color background = parent.getBackground();
		Color foreground = parent.getForeground();
		formText.setBackground(background);
		formText.setForeground(foreground);
		ftext.setBackground(background);
		ftext.setForeground(foreground);
		ftext.marginWidth = 2;
		ftext.marginHeight = 2;
		ftext.setHyperlinkSettings(toolkit.getHyperlinkGroup());
		formText.addDisposeListener(e -> {
			if (toolkit != null) {
				toolkit.dispose();
				toolkit = null;
			}
		});
		if (text != null) {
			formText.setText(text);
		}
	}

	public Control getControl() {
		return container;
	}

	public void setText(String text) {
		this.text = text;
		if (formText != null) {
			formText.setText(text);
		}
	}

	public void setEnabled(boolean enabled) {
		if (formText == null || formText.isDisposed()) {
			return;
		}
		// A disabled FormText paints itself with the non-theme-aware
		// SWT.COLOR_WIDGET_BACKGROUND, which is white in the dark theme. Keep the
		// widgets enabled to preserve the themed background and only grey the text
		// to signal the disabled state; input is blocked via the disabled parent.
		Color foreground = enabled ? container.getForeground()
				: formText.getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW);
		formText.setForeground(foreground);
		if (ftext != null) {
			ftext.setForeground(foreground);
		}
	}
}
