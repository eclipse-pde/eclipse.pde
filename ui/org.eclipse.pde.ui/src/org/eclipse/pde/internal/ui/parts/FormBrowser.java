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

import org.eclipse.update.ui.forms.internal.engine.FormEngine;
import org.eclipse.swt.widgets.*;
import org.eclipse.update.ui.forms.internal.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.*;

public class FormBrowser {
	ScrolledComposite scomp;
	FormEngine engine;
	String text;
	FormWidgetFactory factory;
	int style;
	
	public FormBrowser(int style) {
		this.style = style;
	}

	public void createControl(Composite parent) {
		factory = new FormWidgetFactory(parent.getDisplay());
		scomp = new ScrolledComposite(parent, style);
		scomp.setBackground(factory.getBackgroundColor());
		engine = factory.createFormEngine(scomp);
		engine.setMarginWidth(2);
		engine.setMarginHeight(2);
		engine.setHyperlinkSettings(factory.getHyperlinkHandler());
		scomp.setContent(engine);
     	scomp.addListener (SWT.Resize,  new Listener () {
			public void handleEvent (Event e) {
				updateSize();
			}
		});
		if (text!=null) loadText(text);
	}
	
	public Control getControl() {
		return scomp;
	}
	
	public void setText(String text) {
		this.text = text;
		loadText(text);
	}
	
	private void loadText(String text) {
		if (engine!=null) {
			String markup = "<form>"+text+"</form>";
			engine.load(markup, true, false);
			updateSize();
			engine.redraw();
			scomp.layout();
		}
	}
	private void updateSize() {
		Rectangle ssize = scomp.getClientArea();
		int swidth = ssize.width;
		ScrollBar vbar = scomp.getVerticalBar();
		if (vbar!=null) {
			swidth -= vbar.getSize().x;
		}
		Point size = engine.computeSize(swidth, SWT.DEFAULT, true);
		engine.setSize(size);
	}
}