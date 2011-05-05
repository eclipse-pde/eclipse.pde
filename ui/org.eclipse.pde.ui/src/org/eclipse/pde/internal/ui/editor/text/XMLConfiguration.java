/*******************************************************************************
 *  Copyright (c) 2003, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.text;

import org.eclipse.jface.text.*;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.reconciler.Reconciler;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.internal.core.text.IReconcilingParticipant;
import org.eclipse.pde.internal.core.text.plugin.PluginModel;
import org.eclipse.pde.internal.ui.editor.ISortableContentOutlinePage;
import org.eclipse.pde.internal.ui.editor.PDESourcePage;
import org.eclipse.pde.internal.ui.editor.context.XMLDocumentSetupParticpant;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;

/**
 * Source viewer configuration for the XML Source editors
 *
 */
public class XMLConfiguration extends ChangeAwareSourceViewerConfiguration {
	private AnnotationHover fAnnotationHover;
	private XMLDoubleClickStrategy fDoubleClickStrategy;
	private XMLTagScanner fTagScanner;
	private XMLScanner fPdeScanner;

	private TextAttribute fXMLCommentAttr;
	private MultilineDamagerRepairer fDamagerRepairer;

	public XMLConfiguration(IColorManager colorManager) {
		this(colorManager, null);
	}

	public XMLConfiguration(IColorManager colorManager, PDESourcePage page) {
		super(page, colorManager);
	}

	public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
		return new String[] {IDocument.DEFAULT_CONTENT_TYPE, XMLPartitionScanner.XML_COMMENT, XMLPartitionScanner.XML_TAG, XMLStringPartitionScanner.XML_STRING};
	}

	public ITextDoubleClickStrategy getDoubleClickStrategy(ISourceViewer sourceViewer, String contentType) {
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
		reconciler.setDocumentPartitioning(XMLDocumentSetupParticpant.XML_PARTITIONING);

		MultilineDamagerRepairer dr = new MultilineDamagerRepairer(getPDEScanner());
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

		dr = new MultilineDamagerRepairer(getPDETagScanner());
		reconciler.setDamager(dr, XMLPartitionScanner.XML_TAG);
		reconciler.setRepairer(dr, XMLPartitionScanner.XML_TAG);

		fXMLCommentAttr = BasePDEScanner.createTextAttribute(fColorManager, IPDEColorConstants.P_XML_COMMENT);
		fDamagerRepairer = new MultilineDamagerRepairer(null, fXMLCommentAttr);
		reconciler.setDamager(fDamagerRepairer, XMLPartitionScanner.XML_COMMENT);
		reconciler.setRepairer(fDamagerRepairer, XMLPartitionScanner.XML_COMMENT);

		return reconciler;
	}

	public IAnnotationHover getAnnotationHover(ISourceViewer sourceViewer) {
		if (fAnnotationHover == null)
			fAnnotationHover = new AnnotationHover();
		return fAnnotationHover;
	}

	/**
	 * Preference colors or fonts have changed.  
	 * Update the default tokens of the scanners.
	 */
	public void adaptToPreferenceChange(PropertyChangeEvent event) {
		if (fTagScanner == null)
			return; //property change before the editor is fully created
		if (affectsColorPresentation(event))
			fColorManager.handlePropertyChangeEvent(event);
		fTagScanner.adaptToPreferenceChange(event);
		fPdeScanner.adaptToPreferenceChange(event);
		String property = event.getProperty();
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
			fXMLCommentAttr = new TextAttribute(fColorManager.getColor(event.getProperty()), fXMLCommentAttr.getBackground(), fXMLCommentAttr.getStyle());
		}
		fDamagerRepairer.setDefaultTextAttribute(fXMLCommentAttr);
	}

	private TextAttribute adaptToStyleChange(PropertyChangeEvent event, int styleAttribute, TextAttribute textAttribute) {
		boolean eventValue = false;
		Object value = event.getNewValue();
		if (value instanceof Boolean)
			eventValue = ((Boolean) value).booleanValue();

		boolean activeValue = (textAttribute.getStyle() & styleAttribute) == styleAttribute;
		if (activeValue != eventValue) {
			Color foreground = textAttribute.getForeground();
			Color background = textAttribute.getBackground();
			int style = eventValue ? textAttribute.getStyle() | styleAttribute : textAttribute.getStyle() & ~styleAttribute;
			textAttribute = new TextAttribute(foreground, background, style);
		}
		return textAttribute;
	}

	public boolean affectsTextPresentation(PropertyChangeEvent event) {
		String property = event.getProperty();
		return property.startsWith(IPDEColorConstants.P_DEFAULT) || property.startsWith(IPDEColorConstants.P_PROC_INSTR) || property.startsWith(IPDEColorConstants.P_STRING) || property.startsWith(IPDEColorConstants.P_EXTERNALIZED_STRING) || property.startsWith(IPDEColorConstants.P_TAG) || property.startsWith(IPDEColorConstants.P_XML_COMMENT);
	}

	public boolean affectsColorPresentation(PropertyChangeEvent event) {
		String property = event.getProperty();
		return property.equals(IPDEColorConstants.P_DEFAULT) || property.equals(IPDEColorConstants.P_PROC_INSTR) || property.equals(IPDEColorConstants.P_STRING) || property.equals(IPDEColorConstants.P_EXTERNALIZED_STRING) || property.equals(IPDEColorConstants.P_TAG) || property.equals(IPDEColorConstants.P_XML_COMMENT);
	}

	protected int getInfoImplementationType() {
		return SourceInformationProvider.F_XML_IMP;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.source.SourceViewerConfiguration#getConfiguredDocumentPartitioning(org.eclipse.jface.text.source.ISourceViewer)
	 */
	public String getConfiguredDocumentPartitioning(ISourceViewer sourceViewer) {
		return XMLDocumentSetupParticpant.XML_PARTITIONING;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.editor.text.ChangeAwareSourceViewerConfiguration#getReconciler(org.eclipse.jface.text.source.ISourceViewer)
	 */
	public IReconciler getReconciler(ISourceViewer sourceViewer) {
		if (fSourcePage != null && fReconciler == null) {
			IBaseModel model = fSourcePage.getInputContext().getModel();
			if (model instanceof IReconcilingParticipant) {
				ReconcilingStrategy strategy = new ReconcilingStrategy();
				strategy.addParticipant((IReconcilingParticipant) model);
				ISortableContentOutlinePage outline = fSourcePage.getContentOutline();
				if (outline instanceof IReconcilingParticipant)
					strategy.addParticipant((IReconcilingParticipant) outline);

				Reconciler reconciler = new Reconciler();
				reconciler.setReconcilingStrategy(strategy, IDocument.DEFAULT_CONTENT_TYPE);
				if (!(model instanceof PluginModel)) {
					XMLReconcilingStrategy xmlStrategy = new XMLReconcilingStrategy(sourceViewer);
					reconciler.setReconcilingStrategy(xmlStrategy, XMLStringPartitionScanner.XML_STRING);
					reconciler.setReconcilingStrategy(xmlStrategy, XMLStringPartitionScanner.CUSTOM_TAG);
					reconciler.setDocumentPartitioning(XMLStringPartitionScanner.XML_STRING);
				}
				reconciler.setDelay(500);
				fReconciler = reconciler;
			}
		}
		return fReconciler;
	}
}
