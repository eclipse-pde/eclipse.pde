/*
 * (c) Copyright 2001 MyCorporation.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal.editor;

import org.eclipse.update.ui.forms.internal.FormWidgetFactory;
import org.eclipse.pde.internal.parts.*;
import org.eclipse.jface.action.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.dnd.*;
import org.eclipse.pde.internal.model.ModelDataTransfer;

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
	
	protected Composite createClientContainer(Composite parent, int span, FormWidgetFactory factory) {
		Composite container = factory.createComposite(parent);
		GridLayout layout = new GridLayout();
		layout.marginWidth = layout.marginHeight = 2;
		layout.numColumns = span;
		container.setLayout(layout);
		return container;
	}
	
	protected abstract StructuredViewerPart createViewerPart(String [] buttonLabels);
	
	protected void fillContextMenu(IMenuManager manager) {
	}
	
	protected void buttonSelected(int index) {
	}

	protected void doPaste() {
		ISelection selection = viewerPart.getViewer().getSelection();
		IStructuredSelection ssel = (IStructuredSelection)selection;
		if (ssel.size()>1) return;
		
		Object target = ssel.getFirstElement();
		
		Clipboard clipboard = getFormPage().getEditor().getClipboard();
		ModelDataTransfer modelTransfer = ModelDataTransfer.getInstance();
		Object [] objects = (Object[])clipboard.getContents(modelTransfer);
		if (objects!=null) {
			doPaste(target, objects);
		}
	}
	protected void doPaste(Object target, Object[] objects) {
	}
}