/*******************************************************************************
 * Copyright (c) 2003, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.text;

import org.eclipse.jface.text.*;
import org.eclipse.jface.text.presentation.*;
import org.eclipse.jface.text.reconciler.*;
import org.eclipse.jface.text.rules.*;
import org.eclipse.jface.text.source.*;
import org.eclipse.jface.util.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;


public class XMLSourceViewerConfiguration extends TextSourceViewerConfiguration {
	private AnnotationHover fAnnotationHover;
	private XMLDoubleClickStrategy fDoubleClickStrategy;
	private XMLTagScanner fTagScanner;
	private XMLScanner fPdeScanner;
	private IColorManager fColorManager;
	private XMLSourcePage fSourcePage;
	private MonoReconciler fReconciler;
	private NonRuleBasedDamagerRepairer fNdr; 

	private TextAttribute fXMLCommentAttr;
	
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
	
	public void applyColorPreferenceChange(){
		fPdeScanner = new XMLScanner(fColorManager);
		fPdeScanner.setDefaultReturnToken(
				new Token(
						new TextAttribute(fColorManager.getColor(IPDEColorConstants.P_DEFAULT))));
		fTagScanner = new XMLTagScanner(fColorManager);
		fTagScanner.setDefaultReturnToken(
				new Token(new TextAttribute(fColorManager.getColor(IPDEColorConstants.P_TAG))));
		
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

		DefaultDamagerRepairer fDr = new DefaultDamagerRepairer(getPDEScanner());
		reconciler.setDamager(fDr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(fDr, IDocument.DEFAULT_CONTENT_TYPE);

		fDr = new DefaultDamagerRepairer(getPDETagScanner());
		reconciler.setDamager(fDr, XMLPartitionScanner.XML_TAG);
		reconciler.setRepairer(fDr, XMLPartitionScanner.XML_TAG);

		fXMLCommentAttr = new TextAttribute(fColorManager.getColor(IPDEColorConstants.P_XML_COMMENT));
		fNdr =
			new NonRuleBasedDamagerRepairer(fXMLCommentAttr);
		reconciler.setDamager(fNdr, XMLPartitionScanner.XML_COMMENT);
		reconciler.setRepairer(fNdr, XMLPartitionScanner.XML_COMMENT);

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
				if (fSourcePage.getContentOutline() instanceof IReconcilingParticipant)
					strategy.addParticipant((IReconcilingParticipant)fSourcePage.getContentOutline());
				fReconciler = new MonoReconciler(strategy, false);
				fReconciler.setDelay(500);
			}
		}
		return fReconciler;
	}

	public IColorManager getColorManager(){
		return fColorManager;	
	}
	
	/**
	 * Preference colors have changed.  
	 * Update the default tokens of the scanners.
	 */
	public void adaptToPreferenceChange(PropertyChangeEvent event) {
		if (fTagScanner == null) {
			return; //property change before the editor is fully created
		}
		fTagScanner.adaptToPreferenceChange((ColorManager)fColorManager, event);
		fPdeScanner.adaptToPreferenceChange((ColorManager)fColorManager, event);
		String property= event.getProperty();
		if (property.startsWith(IPDEColorConstants.P_XML_COMMENT)) {
			adaptToColorChange(event);
			fNdr.setDefaultTextAttribute(fXMLCommentAttr);
		} 
	}
	
	 /**
     * Update the text attributes associated with the tokens of this scanner as a color preference has been changed. 
     */
    private void adaptToColorChange(PropertyChangeEvent event) {
    	((ColorManager)fColorManager).updateProperty(event.getProperty()); 
    	fXMLCommentAttr= new TextAttribute(((ColorManager)fColorManager).getColor(event.getProperty()), fXMLCommentAttr.getBackground(), fXMLCommentAttr.getStyle());
    }
 
	
	
}
