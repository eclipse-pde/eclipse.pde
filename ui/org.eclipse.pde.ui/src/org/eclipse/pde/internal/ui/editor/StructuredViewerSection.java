/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor;

import org.eclipse.jface.action.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.ui.parts.*;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.widgets.FormToolkit;

/**
 * @version 	1.0
 * @author
 */
public abstract class StructuredViewerSection extends PDESection {
	protected StructuredViewerPart viewerPart;
	/**
	 * Constructor for StructuredViewerSection.
	 * @param formPage
	 */
	public StructuredViewerSection(PDEFormPage formPage, Composite parent, int style, String [] buttonLabels) {
		super(formPage, parent, style);
		viewerPart = createViewerPart(buttonLabels);
		viewerPart.setMinimumSize(50, 50);
		FormToolkit toolkit = formPage.getManagedForm().getToolkit();
		//toolkit.createCompositeSeparator(getSection());
		createClient(getSection(), toolkit);
	}

	protected void createViewerPartControl(Composite parent, int style, int span, FormToolkit toolkit) {
		viewerPart.createControl(parent, style, span, toolkit);
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
	
	protected Composite createClientContainer(Composite parent, int span, FormToolkit toolkit) {
		Composite container = toolkit.createComposite(parent);
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
		ISelection selection = getViewerSelection();
		IStructuredSelection ssel = (IStructuredSelection)selection;
		if (ssel.size()>1) return;
		
		Object target = ssel.getFirstElement();
		
		Clipboard clipboard = getPage().getPDEEditor().getClipboard();
		ModelDataTransfer modelTransfer = ModelDataTransfer.getInstance();
		Object [] objects = (Object[])clipboard.getContents(modelTransfer);
		if (objects!=null) {
			doPaste(target, objects);
		}
	}
	public boolean canPaste(Clipboard clipboard) {
		ISelection selection = getViewerSelection();
		IStructuredSelection ssel = (IStructuredSelection)selection;
		if (ssel.size()>1) return false;
			
		Object target = ssel.getFirstElement();
		ModelDataTransfer modelTransfer = ModelDataTransfer.getInstance();
		Object [] objects = (Object[])clipboard.getContents(modelTransfer);
		if (objects!=null && objects.length>0) {
			return canPaste(target, objects);
		}
		else return false;
	}
	protected ISelection getViewerSelection() {
		return viewerPart.getViewer().getSelection();
	}
	protected void doPaste(Object target, Object[] objects) {
	}
	
	protected boolean canPaste(Object target, Object [] objects) {
		return false;
	}
	public void setFocus() {
		viewerPart.getControl().setFocus();
	}
}
