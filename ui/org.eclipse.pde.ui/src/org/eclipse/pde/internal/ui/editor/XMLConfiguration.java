package org.eclipse.pde.internal.ui.editor;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.jface.text.rules.*;
import org.eclipse.pde.internal.ui.editor.text.*;
import org.eclipse.jface.text.presentation.*;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.source.*;


public class XMLConfiguration extends SourceViewerConfiguration {
	private XMLDoubleClickStrategy doubleClickStrategy;
	private PDETagScanner tagScanner;
	private PDECommentScanner commentScanner;
	private PDEScanner pdeScanner;
	private IColorManager colorManager;

public XMLConfiguration(IColorManager colorManager) {
	this.colorManager = colorManager;
}
	public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
		return new String[] { 
			IDocument.DEFAULT_CONTENT_TYPE, 
			PDEPartitionScanner.XML_COMMENT,
			PDEPartitionScanner.XML_TAG
		};
	}
	public ITextDoubleClickStrategy getDoubleClickStrategy(ISourceViewer sourceViewer, String contentType) {
		if (doubleClickStrategy==null) doubleClickStrategy = new XMLDoubleClickStrategy();
		return doubleClickStrategy;
		//return super.getDoubleClickStrategy(sourceViewer, contentType);
	}
protected PDECommentScanner getPDECommentScanner() {
	if (commentScanner==null) {
		commentScanner = new PDECommentScanner(colorManager);
	}
	return commentScanner;
}
protected PDEScanner getPDEScanner() {
	if (pdeScanner==null) {
		pdeScanner = new PDEScanner(colorManager);
	}
	return pdeScanner;
}
protected PDETagScanner getPDETagScanner() {
	if (tagScanner==null) {
		tagScanner = new PDETagScanner(colorManager);
	}
	return tagScanner;
}
public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
	PresentationReconciler reconciler = new PresentationReconciler();

	RuleBasedDamagerRepairer dr =
		new RuleBasedDamagerRepairer(
			getPDEScanner(),
			new TextAttribute(colorManager.getColor(IPDEColorConstants.DEFAULT)));
	reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
	reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

	dr =
		new RuleBasedDamagerRepairer(
			getPDETagScanner(),
			new TextAttribute(colorManager.getColor(IPDEColorConstants.TAG)));
	reconciler.setDamager(dr, PDEPartitionScanner.XML_TAG);
	reconciler.setRepairer(dr, PDEPartitionScanner.XML_TAG);

	dr =
		new RuleBasedDamagerRepairer(
			null,
			new TextAttribute(colorManager.getColor(IPDEColorConstants.XML_COMMENT)));
	reconciler.setDamager(dr, PDEPartitionScanner.XML_COMMENT);
	reconciler.setRepairer(dr, PDEPartitionScanner.XML_COMMENT);

	return reconciler;
}
}
