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
package org.eclipse.pde.internal.ui.editor;

import org.eclipse.jface.text.*;
import org.eclipse.jface.text.presentation.*;
import org.eclipse.jface.text.rules.*;
import org.eclipse.jface.text.source.*;
import org.eclipse.pde.internal.ui.editor.text.*;

public class XMLConfiguration extends SourceViewerConfiguration {
	private XMLDoubleClickStrategy doubleClickStrategy;
	private PDETagScanner tagScanner;
	private PDEScanner pdeScanner;
	private IColorManager colorManager;

	public XMLConfiguration(IColorManager colorManager) {
		setColorManager(colorManager);
	}
	
	public void setColorManager(IColorManager colorManager) {
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
		if (pdeScanner == null) {
			pdeScanner = new PDEScanner(colorManager);
			pdeScanner.setDefaultReturnToken(
				new Token(
					new TextAttribute(colorManager.getColor(IPDEColorConstants.P_DEFAULT))));
		}
		return pdeScanner;
	}
	protected PDETagScanner getPDETagScanner() {
		if (tagScanner == null) {
			tagScanner = new PDETagScanner(colorManager);
			tagScanner.setDefaultReturnToken(
				new Token(new TextAttribute(colorManager.getColor(IPDEColorConstants.P_TAG))));
		}
		return tagScanner;
	}
	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
		PresentationReconciler reconciler = new PresentationReconciler();

		DefaultDamagerRepairer dr = new DefaultDamagerRepairer(getPDEScanner());
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

		dr = new DefaultDamagerRepairer(getPDETagScanner());
		reconciler.setDamager(dr, PDEPartitionScanner.XML_TAG);
		reconciler.setRepairer(dr, PDEPartitionScanner.XML_TAG);

		NonRuleBasedDamagerRepairer ndr =
			new NonRuleBasedDamagerRepairer(
				new TextAttribute(colorManager.getColor(IPDEColorConstants.P_XML_COMMENT)));
		reconciler.setDamager(ndr, PDEPartitionScanner.XML_COMMENT);
		reconciler.setRepairer(ndr, PDEPartitionScanner.XML_COMMENT);

		return reconciler;
	}
	
	public IAnnotationHover getAnnotationHover(ISourceViewer sourceViewer) {
		return new AnnotationHover();
	}
	
	public IColorManager getColorManager(){
		return colorManager;
	
	}
}
