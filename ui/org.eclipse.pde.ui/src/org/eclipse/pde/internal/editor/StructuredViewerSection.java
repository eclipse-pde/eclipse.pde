/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.editor;

import org.eclipse.update.ui.forms.internal.FormWidgetFactory;
import org.eclipse.pde.internal.parts.*;
import org.eclipse.jface.action.*;
import org.eclipse.swt.widgets.*;

/**
 * @version 	1.0
 * @author
 */
public abstract class StructuredViewerSection extends PDEFormSection {
	protected StructuredViewerPart viewerPart;
	/**
	 * Constructor for StructuredViewerSection.
	 * @param formPage
	 */
	public StructuredViewerSection(PDEFormPage formPage, String [] buttonLabels) {
		super(formPage);
		viewerPart = createViewerPart(buttonLabels);
	}

	protected void createViewerPartControl(Composite parent, int style, int span, FormWidgetFactory factory) {
		viewerPart.createControl(parent, style, span, factory);
		MenuManager popupMenuManager = new MenuManager();
		IMenuListener listener = new IMenuListener() {
			public void menuAboutToShow(IMenuManager mng) {
				fillContextMenu(mng);
			}
		};
		popupMenuManager.addMenuListener(listener);
		popupMenuManager.setRemoveAllWhenShown(true);
		Control control = viewerPart.getControl();
		Menu menu = popupMenuManager.createContextMenu(control);
		control.setMenu(menu);
	}
	
	protected abstract StructuredViewerPart createViewerPart(String [] buttonLabels);
	
	protected void fillContextMenu(IMenuManager manager) {
	}
	
	protected void buttonSelected(int index) {
	}
}