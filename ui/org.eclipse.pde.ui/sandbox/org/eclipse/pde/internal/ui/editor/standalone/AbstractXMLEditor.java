package org.eclipse.pde.internal.ui.editor.standalone;

import org.eclipse.jface.text.*;
import org.eclipse.jface.text.reconciler.*;
import org.eclipse.jface.text.source.*;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.internal.ui.editor.standalone.parser.*;
import org.eclipse.pde.internal.ui.editor.standalone.text.*;
import org.eclipse.ui.*;
import org.eclipse.ui.editors.text.*;
import org.eclipse.ui.part.*;
import org.eclipse.ui.texteditor.*;
import org.eclipse.ui.views.contentoutline.*;
import org.w3c.dom.*;

public abstract class AbstractXMLEditor extends TextEditor {
	class SelectionChangedListener  implements ISelectionChangedListener {
		public void selectionChanged(SelectionChangedEvent event) {
			doSelectionChanged(event);
		}
	}

	protected DocumentModel fModel;
	protected IDocumentProvider fDocumentProvider;
	private ColorManager fColorManager;
	private SelectionChangedListener selectionChangedListener= new SelectionChangedListener();

	public AbstractXMLEditor() {
		super();
		fColorManager = new ColorManager();
		setSourceViewerConfiguration(new XMLConfiguration2(fColorManager, this));
	}
	
	/**
	 * @param input
	 */
	protected void createDocumentProvider(IEditorInput input) {
		if (input instanceof FileEditorInput)
			fDocumentProvider = new SynchronizedUTF8FileDocumentProvider();
		setDocumentProvider(fDocumentProvider);
		/*else if (input instanceof File)
			fDocumentProvider = new SystemFileDocumentProvider("UTF8");
		else if (input instanceof IStorage)
			fDocumentProvider = new StorageDocumentProvider("UTF8");*/
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.texteditor.AbstractTextEditor#init(org.eclipse.ui.IEditorSite, org.eclipse.ui.IEditorInput)
	 */
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		createDocumentProvider(input);
		super.init(site, input);
	}

	public void dispose() {
		fColorManager.dispose();
		super.dispose();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.editors.text.TextEditor#getAdapter(java.lang.Class)
	 */
	public Object getAdapter(Class adapter) {
		if (IContentOutlinePage.class.equals(adapter))
			return createContentOutlinePage();
		return super.getAdapter(adapter);
	}
	
	public XMLOutlinePage createContentOutlinePage() {
		SourceViewerConfiguration config = getSourceViewerConfiguration();
		IReconciler reconciler = config.getReconciler(getSourceViewer());
		XMLOutlinePage outlinePage = new XMLOutlinePage(reconciler);
		outlinePage.setContentProvider(getOutlinePageContentProvider());
		outlinePage.setLabelProvider(getOutlinePageLabelProvider());
		outlinePage.addSelectionChangedListener(selectionChangedListener);
		outlinePage.setPageInput(fModel);
		return outlinePage;
	}
	
	protected XMLOutlinePageContentProvider2 getOutlinePageContentProvider() {
		return new XMLOutlinePageContentProvider2();
	}
	
	protected XMLOutlinePageLabelProvider2 getOutlinePageLabelProvider() {
		return new XMLOutlinePageLabelProvider2();
	}
	
	public DocumentModel getModel() {
		return fModel;
	}
	
	protected void doSelectionChanged(SelectionChangedEvent event) {
		ISelection selection = event.getSelection();
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection structuredSelection = (IStructuredSelection) selection;
			Object first = structuredSelection.getFirstElement();
			if (first instanceof IDocumentNode) {
				setHighlightRange(((IDocumentNode) first), true);
				return;
			}
		}
		resetHighlightRange();
	}
	
	public void setHighlightRange(IDocumentNode documentNode, boolean moveCursor) {
		ISourceRange sourceRange = documentNode.getSourceRange();
		if (sourceRange == null)
			return;
			
		ISourceViewer sourceViewer= getSourceViewer();
		if (sourceViewer == null)
			return;
		
		IDocument document= sourceViewer.getDocument();
		if (document == null)
			return;
			
		try {
			int offset = sourceRange.getStartOffset(document);
			while (document.getChar(offset) != '<')
				offset += 1;
			int length = sourceRange.getEndOffset(document) - offset;
			offset += 1;
			setHighlightRange(offset, length, moveCursor);
			String tagname = documentNode.getTagName();
			if (documentNode.getContent() instanceof ProcessingInstruction)
				offset += 1;
			sourceViewer.setSelectedRange(offset, tagname.length());
		} catch (BadLocationException e) {
		}
	}
}
