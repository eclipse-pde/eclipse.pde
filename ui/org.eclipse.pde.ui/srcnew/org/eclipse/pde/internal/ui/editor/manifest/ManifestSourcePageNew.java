package org.eclipse.pde.internal.ui.editor.manifest;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.pde.internal.core.plugin.AbstractPluginModelBase;
import org.eclipse.pde.internal.core.plugin.IDocumentNode;
import org.eclipse.pde.internal.core.plugin.ISourceRange;
import org.eclipse.pde.internal.core.plugin.TicketManager;
import org.eclipse.pde.internal.core.plugin.XMLCore;
import org.eclipse.pde.internal.ui.editor.PDEFormPage;
import org.eclipse.ui.IPartService;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.texteditor.DefaultRangeIndicator;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.update.ui.forms.internal.IFormPage;

public class ManifestSourcePageNew extends ManifestSourcePage {
	class SelectionChangedListener  implements ISelectionChangedListener {
		public void selectionChanged(SelectionChangedEvent event) {
			doSelectionChanged(event);
		}
	};

	private Object modelNeedsUpdatingLock;
	private SelectionChangedListener selectionChangedListener= new SelectionChangedListener();

	public ManifestSourcePageNew(ManifestEditor editor) {
		super(editor);
	}
	
	protected void initializeViewerConfiguration() {
		modelNeedsUpdatingLock= new Object();
		setRangeIndicator(new DefaultRangeIndicator());
		setSourceViewerConfiguration(new XMLViewerConfiguration(this, colorManager));
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
		((ManifestEditor)getEditor()).updateModel();

		ensureModelFinishedUpdating();

		if (containsError()) {
			warnErrorsInSource();
			return false;
		}
		getSite().setSelectionProvider(getEditor());
		return true;
	}
	
	public void becomesVisible(IFormPage oldPage) {
		((ManifestEditor)getEditor()).updateModel();

		if (oldPage instanceof PDEFormPage) {
			selectObjectRange(((PDEFormPage)oldPage).getSelection());
		}
		getSite().setSelectionProvider(getSelectionProvider());
	}
	
	public boolean containsError() {
		return !((AbstractPluginModelBase)getEditor().getModel()).isLoaded(); //updated with syncExec() by the reconciler
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
		ISelection selection= event.getSelection();
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection= (IStructuredSelection) selection;
			Object first= structuredSelection.getFirstElement();
			if (first instanceof IDocumentNode) {
				ISourceRange sourceRange= ((IDocumentNode) first).getSourceRange();
				if (sourceRange != null) {
					boolean moveCursor= !isActivePart();
					setHighlightRange(sourceRange, moveCursor);
					return;
				}
			}
		}
		
		resetHighlightRange();
	}
	
	public void setHighlightRange(ISourceRange sourceRange, boolean moveCursor) {
		ISourceViewer sourceViewer= getSourceViewer();
		if (sourceViewer == null)
			return;
		
		IDocument document= sourceViewer.getDocument();
		if (document == null)
			return;
			
		int offset, length;
		try {
			offset = sourceRange.getStartOffset(document);
			length= sourceRange.getEndOffset(document) - offset;
			setHighlightRange(offset, length, moveCursor);
			sourceViewer.setSelectedRange(offset, length);
		} catch (BadLocationException e) {
			return;
		}
	}
	
	protected boolean isActivePart() {
		IWorkbenchWindow window= getSite().getWorkbenchWindow();
		IPartService service= window.getPartService();
		IWorkbenchPart part= service.getActivePart();
		return part != null && part.equals(this);
	}
	
}
