/**********************************************************************
Copyright (c) 2000, 2002 IBM Corp. and others.
All rights reserved. This program and the accompanying materials
are made available under the terms of the Common Public License v1.0
which accompanies this distribution, and is available at
http://www.eclipse.org/legal/cpl-v10.html

Contributors:
    IBM Corporation - Initial implementation
**********************************************************************/

package org.eclipse.pde.internal.ui.editor.manifest;

import java.util.List;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.pde.internal.core.plugin.DocumentModelChangeEvent;
import org.eclipse.pde.internal.core.plugin.IDocumentModelListener;
import org.eclipse.pde.internal.core.plugin.IDocumentNode;
import org.eclipse.pde.internal.core.plugin.XMLCore;
import org.eclipse.pde.internal.ui.search.PluginSearchActionGroup;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;

/**
 * Content outline page for the XML editor.
 */
public class XMLOutlinePage extends ContentOutlinePage {
	
	
	private IContentProvider fContentProvider;
	private IBaseLabelProvider fLabelProvider;
	private IDocumentModelListener fListener;
	private XMLCore fXMLCore;
	private IDocumentNode fModel;
	
	/**
	 * Creates a new XMLContentOutlinePage.
	 */
	public XMLOutlinePage(XMLCore core) {
		super();
		fXMLCore= core;
	}
	
	public void setContentProvider(IContentProvider contentProvider) {
		fContentProvider= contentProvider;
	}
	
	public void setLabelProvider(IBaseLabelProvider labelProvider) {
		fLabelProvider= labelProvider;
	}
	
	/**
	 * Subclasses can override if different delta mechanism is used.
	 * @return IDocumentModelListener
	 */
	protected IDocumentModelListener createXMLModelChangeListener() {
		return new IDocumentModelListener() {
			public void documentModelChanged(DocumentModelChangeEvent event) {
				if (event.getNode() == fModel) {
					getControl().getDisplay().asyncExec(new Runnable() {
						public void run() {
							getControl().setRedraw(false);
							getTreeViewer().refresh();
							getTreeViewer().expandAll();
							getControl().setRedraw(true);
						}
					});
				}
			}
		};
	}
	
	protected void setViewerInput(Object newInput) {
		TreeViewer tree= getTreeViewer();
		Object oldInput= tree.getInput();
		
		boolean isXMLNode= (newInput instanceof IDocumentNode);
		boolean wasXMLNode= (oldInput instanceof IDocumentNode);
		
		if (isXMLNode && !wasXMLNode) {
			if (fListener == null)
				fListener= createXMLModelChangeListener();
			fXMLCore.addDocumentModelListener(fListener);
		} else if (!isXMLNode && wasXMLNode && fListener != null) {
			fXMLCore.removeDocumentModelListener(fListener);
			fListener= null;
		}
		
		tree.setInput(newInput);
		tree.expandAll();
	}
	
	/**
	 * Sets the input of this page.
	 * @param xmlElement
	 */
	public void setPageInput(IDocumentNode xmlModel) {
		fModel= xmlModel;
		if (getTreeViewer() != null)
			setViewerInput(fModel);
	}
	
	/*
	 * @see org.eclipse.ui.part.IPage#dispose()
	 */
	public void dispose() {
		if (fListener != null) {
			fXMLCore.removeDocumentModelListener(fListener);
			fListener= null;
		}
	}
	
	/**  
	 * Creates the control for this outline page.
	 */
	public void createControl(Composite parent) {
		super.createControl(parent);
	
//		WorkbenchHelp.setHelp(getControl(), IXMLConstants.CONTENT_OUTLINE_PAGE_CONTEXT);
	
		TreeViewer viewer= getTreeViewer();
		viewer.setContentProvider(fContentProvider);
		viewer.setLabelProvider(fLabelProvider);
		
		if (fModel != null)
			setViewerInput(fModel);
			
		createContextMenu();
	}
	
	/**
	 * 
	 */
	private void createContextMenu() {
		MenuManager manager = new MenuManager();
		manager.setRemoveAllWhenShown(true);
		manager.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				contextMenuAboutToShow(manager);
			}
			private void contextMenuAboutToShow(IMenuManager manager) {
				/*IPluginModelBase model = getPlugin().getModel();
				if (model instanceof WorkspacePluginModelBase) {
					manager.add(new UnusedDependenciesAction((WorkspacePluginModelBase)model));
					manager.add(new Separator());
				}*/
				PluginSearchActionGroup actionGroup =
					new PluginSearchActionGroup();
				actionGroup.setContext(new ActionContext(getSelection()));
				actionGroup.fillContextMenu(manager);
			}
		});
		Menu menu = manager.createContextMenu(getControl());
		getControl().setMenu(menu);
		
	}

	/**
	 * Selects the given element in this outline page.
	 * @param xmlElement
	 */
	public void select(Object xmlElement) {
		TreeViewer treeViewer= getTreeViewer();
		ISelection s= treeViewer.getSelection();
		if (s instanceof IStructuredSelection) {
			IStructuredSelection ss= (IStructuredSelection) s;
			List elements= ss.toList();
			if (!elements.contains(xmlElement)) {
				s= (xmlElement == null ? StructuredSelection.EMPTY : new StructuredSelection(xmlElement));
				treeViewer.setSelection(s, true);
			}
		}
	}
}
