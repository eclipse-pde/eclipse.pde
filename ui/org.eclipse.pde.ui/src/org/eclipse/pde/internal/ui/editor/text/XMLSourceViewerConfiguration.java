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

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.reconciler.MonoReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.internal.core.text.IReconcilingParticipant;
import org.eclipse.pde.internal.ui.editor.XMLDoubleClickStrategy;
import org.eclipse.pde.internal.ui.editor.XMLSourcePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;


public class XMLSourceViewerConfiguration extends ChangeAwareSourceViewerConfiguration {
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
		if (fPdeScanner == null)
			fPdeScanner = new XMLScanner(fColorManager);
		return fPdeScanner;
	}
	
	protected XMLTagScanner getPDETagScanner() {
		if (fTagScanner == null) 
			fTagScanner = new XMLTagScanner(fColorManager);
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
	
		fXMLCommentAttr = BasePDEScanner.createTextAttribute(fColorManager, IPDEColorConstants.P_XML_COMMENT);
		fNdr = new NonRuleBasedDamagerRepairer(fXMLCommentAttr);
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
		if (fSourcePage == null)
			return fReconciler;
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
		if (fTagScanner == null)
			return; //property change before the editor is fully created
		
    	fColorManager.handlePropertyChangeEvent(event);
		fTagScanner.adaptToPreferenceChange(fColorManager, event);
		fPdeScanner.adaptToPreferenceChange(fColorManager, event);
		String property= event.getProperty();
		if (property.startsWith(IPDEColorConstants.P_XML_COMMENT)) {
	    	adaptTextAttribute(event);
		} 
	}
	
	private void adaptTextAttribute(PropertyChangeEvent event) {
		String property = event.getProperty();
		if (property.endsWith(IPDEColorConstants.P_BOLD_SUFFIX)) {
			fXMLCommentAttr = adaptToStyleChange(event, SWT.BOLD, fXMLCommentAttr);
		} else if (property.endsWith(IPDEColorConstants.P_ITALIC_SUFFIX)) {
			fXMLCommentAttr = adaptToStyleChange(event, SWT.ITALIC, fXMLCommentAttr);
		} else {
	    	fXMLCommentAttr= new TextAttribute(fColorManager.getColor(event.getProperty()), 
	    									   fXMLCommentAttr.getBackground(), 
	    									   fXMLCommentAttr.getStyle());
		}
		fNdr.setDefaultTextAttribute(fXMLCommentAttr);
	}
	
	private TextAttribute adaptToStyleChange(PropertyChangeEvent event, int styleAttribute, TextAttribute textAttribute) {
		boolean eventValue = false;
		Object value = event.getNewValue();
		if (value instanceof Boolean)
			eventValue = ((Boolean)value).booleanValue();
		
		boolean activeValue = (textAttribute.getStyle() & styleAttribute) == styleAttribute;
		if (activeValue != eventValue) { 
			Color foreground = textAttribute.getForeground();
			Color background = textAttribute.getBackground();
			int style = eventValue ? textAttribute.getStyle() | styleAttribute : textAttribute.getStyle() & ~styleAttribute;
			textAttribute= new TextAttribute(foreground, background, style);
		}	
		return textAttribute;
	}

	
	public boolean affectsTextPresentation(PropertyChangeEvent event) {
		String property = event.getProperty();
		return property.startsWith(IPDEColorConstants.P_DEFAULT) ||
			property.startsWith(IPDEColorConstants.P_PROC_INSTR) ||
			property.startsWith(IPDEColorConstants.P_STRING) || 
			property.startsWith(IPDEColorConstants.P_TAG) || 
			property.startsWith(IPDEColorConstants.P_XML_COMMENT);
	}
	
}
