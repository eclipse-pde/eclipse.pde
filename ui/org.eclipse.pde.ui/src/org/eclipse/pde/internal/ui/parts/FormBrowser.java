/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.parts;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.widgets.*;

public class FormBrowser {
	FormToolkit toolkit;
	ScrolledFormText formText;
	String text;
	int style;
	
	public FormBrowser(int style) {
		this.style = style;
	}

	public void createControl(Composite parent) {
		toolkit = new FormToolkit(parent.getDisplay());
		formText = new ScrolledFormText(parent, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER, false);
		FormText ftext = toolkit.createFormText(formText, false);
		formText.setFormText(ftext);
		formText.setExpandHorizontal(true);
		formText.setExpandVertical(true);
		formText.setBackground(toolkit.getColors().getBackground());
		formText.setForeground(toolkit.getColors().getForeground());
		ftext.marginWidth =2;
		ftext.marginHeight =2;
		ftext.setHyperlinkSettings(toolkit.getHyperlinkGroup());
     	formText.addDisposeListener(new DisposeListener() {
     		public void widgetDisposed(DisposeEvent e) {
     			if (toolkit!=null) {
     				toolkit.dispose();
     				toolkit = null;
     			}
     		}
     	});
     	if (text!=null)
     		formText.setText(text);
	}

	public Control getControl() {
		return formText;
	}
	
	public void setText(String text) {
		this.text = text;
		if (formText!=null) formText.setText(text);
	}
}