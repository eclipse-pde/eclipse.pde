package org.eclipse.pde.internal.ui.neweditor.text;

import org.eclipse.jface.text.*;
import org.eclipse.jface.text.presentation.*;
import org.eclipse.jface.text.reconciler.*;
import org.eclipse.jface.text.rules.*;
import org.eclipse.jface.text.source.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.editor.text.*;
import org.eclipse.pde.internal.ui.neweditor.XMLSourcePage;

/**
 * @author melhem
 *
 */
public class XMLSourceViewerConfiguration extends SourceViewerConfiguration {
	private AnnotationHover fAnnotationHover;
	private XMLDoubleClickStrategy fDoubleClickStrategy;
	private XMLTagScanner fTagScanner;
	private XMLScanner fPdeScanner;
	private IColorManager fColorManager;
	private XMLSourcePage fSourcePage;
	private MonoReconciler fReconciler;

	public XMLSourceViewerConfiguration(XMLSourcePage page, IColorManager colorManager) {
		fSourcePage = page;
		setColorManager(colorManager);
	}
	
	public void setColorManager(IColorManager colorManager) {
		fColorManager = colorManager;
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
	
	protected XMLScanner getPDEScanner() {
		if (fPdeScanner == null) {
			fPdeScanner = new XMLScanner(fColorManager);
			fPdeScanner.setDefaultReturnToken(
				new Token(
					new TextAttribute(fColorManager.getColor(IPDEColorConstants.P_DEFAULT))));
		}
		return fPdeScanner;
	}
	
	protected XMLTagScanner getPDETagScanner() {
		if (fTagScanner == null) {
			fTagScanner = new XMLTagScanner(fColorManager);
			fTagScanner.setDefaultReturnToken(
				new Token(new TextAttribute(fColorManager.getColor(IPDEColorConstants.P_TAG))));
		}
		return fTagScanner;
	}
	
	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
		PresentationReconciler reconciler = new PresentationReconciler();

		DefaultDamagerRepairer dr = new DefaultDamagerRepairer(getPDEScanner());
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

		dr = new DefaultDamagerRepairer(getPDETagScanner());
		reconciler.setDamager(dr, XMLPartitionScanner.XML_TAG);
		reconciler.setRepairer(dr, XMLPartitionScanner.XML_TAG);

		NonRuleBasedDamagerRepairer ndr =
			new NonRuleBasedDamagerRepairer(
				new TextAttribute(fColorManager.getColor(IPDEColorConstants.P_XML_COMMENT)));
		reconciler.setDamager(ndr, XMLPartitionScanner.XML_COMMENT);
		reconciler.setRepairer(ndr, XMLPartitionScanner.XML_COMMENT);

		return reconciler;
	}
	
	public IAnnotationHover getAnnotationHover(ISourceViewer sourceViewer) {
		if (fAnnotationHover == null)
			fAnnotationHover = new AnnotationHover();
		return fAnnotationHover;
	}
	
	public IReconciler getReconciler(ISourceViewer sourceViewer) {
		if (fReconciler == null) {
			IBaseModel model = fSourcePage.getInputContext().getModel();
			if (model instanceof IReconcilingParticipant) {
				ReconcilingStrategy strategy = new ReconcilingStrategy();
				strategy.addParticipant((IReconcilingParticipant)model);
				fReconciler = new MonoReconciler(strategy, false);
				fReconciler.setDelay(500);
			}
		}
		return fReconciler;
	}

	public IColorManager getColorManager(){
		return fColorManager;	
	}
}
