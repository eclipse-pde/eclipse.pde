package org.eclipse.pde.internal.ui.editor.standalone.text;

import org.eclipse.jface.text.*;
import org.eclipse.jface.text.presentation.*;
import org.eclipse.jface.text.reconciler.*;
import org.eclipse.jface.text.rules.*;
import org.eclipse.jface.text.source.*;
import org.eclipse.pde.internal.ui.editor.standalone.*;

public class XMLConfiguration2 extends SourceViewerConfiguration {
	private ColorManager fColorManager;
	private AbstractXMLEditor fEditor;
	private XMLDoubleClickStrategy fDoubleClickStrategy;
	private XMLTagScanner fTagScanner;
	private XMLScanner fScanner;
	private NotifyingReconciler fReconciler;
	private IAnnotationHover fAnnotationHover;

	public XMLConfiguration2(ColorManager colorManager, AbstractXMLEditor editor) {
		this.fColorManager = colorManager;
		fEditor = editor;
	}
	
	public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
		return new String[] {
			IDocument.DEFAULT_CONTENT_TYPE,
			XMLPartitionScanner.XML_COMMENT,
			XMLPartitionScanner.XML_TAG };
	}
	
	public ITextDoubleClickStrategy getDoubleClickStrategy(
		ISourceViewer sourceViewer,
		String contentType) {
		if (fDoubleClickStrategy == null)
			fDoubleClickStrategy = new XMLDoubleClickStrategy();
		return fDoubleClickStrategy;
	}

	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
		PresentationReconciler reconciler = new PresentationReconciler();

		DefaultDamagerRepairer dr = new DefaultDamagerRepairer(getXMLTagScanner());
		reconciler.setDamager(dr, XMLPartitionScanner.XML_TAG);
		reconciler.setRepairer(dr, XMLPartitionScanner.XML_TAG);

		dr = new DefaultDamagerRepairer(getXMLScanner());
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

		NonRuleBasedDamagerRepairer ndr =
			new NonRuleBasedDamagerRepairer(
				new TextAttribute(
					fColorManager.getColor(IXMLColorConstants.XML_COMMENT)));
		reconciler.setDamager(ndr, XMLPartitionScanner.XML_COMMENT);
		reconciler.setRepairer(ndr, XMLPartitionScanner.XML_COMMENT);

		return reconciler;
	}

	public IReconciler getReconciler(ISourceViewer sourceViewer) {
		if (fReconciler == null) {
			fReconciler =
				new NotifyingReconciler(new XMLReconcilingStrategy(fEditor.getModel()), false);
			fReconciler.setDelay(500);
		}
		return fReconciler;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getAnnotationHover(org.eclipse.jface.text.source.ISourceViewer)
	 */
	public IAnnotationHover getAnnotationHover(ISourceViewer sourceViewer) {
		if (fAnnotationHover == null) {
			fAnnotationHover = new XMLAnnotationHover();
		}
		return fAnnotationHover;
	}
	
	protected XMLScanner getXMLScanner() {
		if (fScanner == null) {
			fScanner = new XMLScanner(fColorManager);
			fScanner.setDefaultReturnToken(
				new Token(
					new TextAttribute(
						fColorManager.getColor(IXMLColorConstants.DEFAULT))));
		}
		return fScanner;
	}
	
	protected XMLTagScanner getXMLTagScanner() {
		if (fTagScanner == null) {
			fTagScanner = new XMLTagScanner(fColorManager);
			fTagScanner.setDefaultReturnToken(
				new Token(
					new TextAttribute(fColorManager.getColor(IXMLColorConstants.TAG))));
		}
		return fTagScanner;
	}
	

}