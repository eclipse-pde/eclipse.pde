package org.eclipse.pde.internal.ui.editor.site;

import org.eclipse.jface.text.*;
import org.eclipse.jface.text.presentation.*;
import org.eclipse.jface.text.rules.*;
import org.eclipse.jface.text.source.*;
import org.eclipse.pde.internal.ui.editor.text.*;

public class XMLConfiguration extends SourceViewerConfiguration {
	private XMLDoubleClickStrategy doubleClickStrategy;
	private PDETagScanner tagScanner;
	private PDEScanner scanner;
	private ColorManager colorManager;

	public XMLConfiguration(ColorManager colorManager) {
		this.colorManager = colorManager;
	}
	public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
		return new String[] {
			IDocument.DEFAULT_CONTENT_TYPE,
			PDEPartitionScanner.XML_COMMENT,
			PDEPartitionScanner.XML_TAG };
	}
	public ITextDoubleClickStrategy getDoubleClickStrategy(
		ISourceViewer sourceViewer,
		String contentType) {
		if (doubleClickStrategy == null)
			doubleClickStrategy = new XMLDoubleClickStrategy();
		return doubleClickStrategy;
	}

	protected PDEScanner getPDEScanner() {
		if (scanner == null) {
			scanner = new PDEScanner(colorManager);
			scanner.setDefaultReturnToken(
				new Token(
					new TextAttribute(
						colorManager.getColor(IPDEColorConstants.P_DEFAULT))));
		}
		return scanner;
	}
	protected PDETagScanner getPDETagScanner() {
		if (tagScanner == null) {
			tagScanner = new PDETagScanner(colorManager);
			tagScanner.setDefaultReturnToken(
				new Token(
					new TextAttribute(
						colorManager.getColor(IPDEColorConstants.P_TAG))));
		}
		return tagScanner;
	}

	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
		PresentationReconciler reconciler = new PresentationReconciler();

		DefaultDamagerRepairer dr =
			new DefaultDamagerRepairer(getPDETagScanner());
		reconciler.setDamager(dr, PDEPartitionScanner.XML_TAG);
		reconciler.setRepairer(dr, PDEPartitionScanner.XML_TAG);

		dr = new DefaultDamagerRepairer(getPDEScanner());
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

		NonRuleBasedDamagerRepairer ndr =
			new NonRuleBasedDamagerRepairer(
				new TextAttribute(
					colorManager.getColor(IPDEColorConstants.P_XML_COMMENT)));
		reconciler.setDamager(ndr, PDEPartitionScanner.XML_COMMENT);
		reconciler.setRepairer(ndr, PDEPartitionScanner.XML_COMMENT);

		return reconciler;
	}

}