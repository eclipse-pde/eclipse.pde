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
package org.eclipse.pde.internal.ui.editor.manifest;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.source.*;
import org.eclipse.jface.util.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.core.plugin.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.text.*;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.*;
import org.eclipse.ui.texteditor.*;
import org.eclipse.ui.views.contentoutline.*;
import org.eclipse.update.ui.forms.internal.*;

public class ManifestSourcePageNew extends ManifestSourcePage implements IPDEColorConstants {
	class SelectionChangedListener  implements ISelectionChangedListener {
		public void selectionChanged(SelectionChangedEvent event) {
			doSelectionChanged(event);
		}
	}

	private Object modelNeedsUpdatingLock;
	private Object dynamicReconcilingLock;
	private boolean dynamicReconciling;
	private SelectionChangedListener selectionChangedListener= new SelectionChangedListener();

	public ManifestSourcePageNew(ManifestEditor editor) {
		super(editor);
		dynamicReconcilingLock= new Object();
		modelNeedsUpdatingLock= new Object();
		setRangeIndicator(new DefaultRangeIndicator());
	}
	
	public IContentOutlinePage createContentOutlinePage() {
		XMLOutlinePage outlinePage = new XMLOutlinePage(XMLCore.getDefault());
		outlinePage.setContentProvider(new XMLOutlinePageContentProvider());
		outlinePage.setLabelProvider(new ManifestSourceOutlinePageLabelProvider());
		outlinePage.addSelectionChangedListener(selectionChangedListener);
		outlinePage.setPageInput(((AbstractPluginModelBase)getEditor().getModel()).getDocumentModel());
		return outlinePage;
	}
	
	public boolean becomesInvisible(IFormPage newPage) {
		setDynamicReconciling(false);
		((ManifestEditor)getEditor()).updateModel();

		ensureModelFinishedUpdating();

		if (containsError()) {
			warnErrorsInSource();
			return false;
		}
		unregisterGlobalActions();
		//getSite().setSelectionProvider(getEditor());
		return true;
	}
	
	public void becomesVisible(IFormPage oldPage) {
		((ManifestEditor)getEditor()).updateModel();
		setDynamicReconciling(true);

		if (oldPage instanceof PDEFormPage) {
			selectObjectRange(((PDEFormPage)oldPage).getSelection());
		}
		registerGlobalActions();
		//getSite().setSelectionProvider(getSelectionProvider());
	}
	
	protected void setModelNeedsUpdating(boolean modelNeedsUpdating) {
		synchronized (modelNeedsUpdatingLock) {
			super.setModelNeedsUpdating(modelNeedsUpdating);
		}
	}
	
	protected boolean isModelNeedsUpdating() {
		synchronized (modelNeedsUpdatingLock) {
			return super.isModelNeedsUpdating();
		}
	}

	boolean tryGetModelUpdatingTicket() {
		synchronized (modelNeedsUpdatingLock) {
			boolean result= isModelNeedsUpdating();
			if (result) {
				setModelNeedsUpdating(false);
				((AbstractPluginModelBase)getEditor().getModel()).getDocumentModel().getTicketManager().buyTicket();
			}
			return result;
		}
	}
	
	private void ensureModelFinishedUpdating() {
		TicketManager ticketManager= ((AbstractPluginModelBase)getEditor().getModel()).getDocumentModel().getTicketManager();
		if (!ticketManager.isAllTicketsUsed()) {
			//the main thread (assumingly the current thread) 'would' have to update the model -> force update here
			ticketManager.buyTicket();
			((ManifestEditor)getEditor()).updateModel();
		}
	}
	
	protected void initializeDocumentListener() {
		setDocumentListener(new IDocumentListener() {
			public void documentAboutToBeChanged(DocumentEvent event) {
				setModelNeedsUpdating(true);
			}
			public void documentChanged(DocumentEvent event) {}
		});
	}

	protected void doSelectionChanged(SelectionChangedEvent event) {
		if (isActivePart())
			return;
		
		ISelection selection= event.getSelection();
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection= (IStructuredSelection) selection;
			Object first= structuredSelection.getFirstElement();
			if (first instanceof IDocumentNode) {
				IDocumentNode node = (IDocumentNode)first;
				ISourceRange sourceRange= node.getSourceRange();
				if (sourceRange != null) {
					setHighlightRange(
						sourceRange,
						((PluginDocumentNode) node).getDOMNode().getNodeName(),
						true);
				}
			}
		}
	}
	
	public void setHighlightRange(ISourceRange sourceRange, String nodeName, boolean moveCursor) {
		ISourceViewer sourceViewer = getSourceViewer();
		if (sourceViewer == null)
			return;

		IDocument document = sourceViewer.getDocument();
		if (document == null)
			return;

		int offset = sourceRange.getOffset();
		setHighlightRange(offset, sourceRange.getLength(), moveCursor);
		sourceViewer.setSelectedRange(offset + 1, nodeName.length());
	}
	
	protected boolean isActivePart() {
		IWorkbenchWindow window= getSite().getWorkbenchWindow();
		IPartService service= window.getPartService();
		IWorkbenchPart part= service.getActivePart();
		return part != null && part.equals(getEditor());
	}
	
	private void setDynamicReconciling(boolean enabled) {
		synchronized (dynamicReconcilingLock) {
			dynamicReconciling= enabled;
		}
	}
	
	boolean isDynamicReconciling() {
		synchronized (dynamicReconcilingLock) {
			return dynamicReconciling;
		}
	}
	
	public void createPartControl(Composite parent) {
		setModelNeedsUpdating(true);
		super.createPartControl(parent);
	}
	
	public boolean containsError() {
		return getEditor().containsError();
	}
	
	protected void handlePreferenceStoreChanged(PropertyChangeEvent event) {
		if (isColorProperty(event.getProperty())) {
			colorManager.dispose();
			colorManager = new ColorManager();
			setSourceViewerConfiguration(new XMLViewerConfiguration(this, colorManager));
			if (getSourceViewer()!=null)
				getSourceViewer().configure(getSourceViewerConfiguration());
			try {
				doSetInput(getEditorInput());
			} catch (CoreException e) {
			}
		}
		super.handlePreferenceStoreChanged(event);
	}
	
	private boolean isColorProperty(String property) {
		return property.equals(P_DEFAULT) || property.equals(P_PROC_INSTR) || property.equals(P_STRING) || property.equals(P_TAG) || property.equals(XML_COMMENT);
	}

}
