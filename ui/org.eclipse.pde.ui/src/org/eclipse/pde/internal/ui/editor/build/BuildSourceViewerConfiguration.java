/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.build;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.quickassist.IQuickAssistAssistant;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.reconciler.MonoReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IWhitespaceDetector;
import org.eclipse.jface.text.rules.IWordDetector;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WhitespaceRule;
import org.eclipse.jface.text.rules.WordRule;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.internal.core.text.IReconcilingParticipant;
import org.eclipse.pde.internal.ui.editor.text.ArgumentRule;
import org.eclipse.pde.internal.ui.editor.text.BasePDEScanner;
import org.eclipse.pde.internal.ui.editor.text.IColorManager;
import org.eclipse.pde.internal.ui.editor.text.PDEQuickAssistAssistant;
import org.eclipse.pde.internal.ui.editor.text.ReconcilingStrategy;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;


public class BuildSourceViewerConfiguration extends TextSourceViewerConfiguration {

	protected static String PROPERTIES_FILE_PARTITIONING = "___pf_partitioning";  //$NON-NLS-1$
	protected static String COMMENT = "__pf_comment"; //$NON-NLS-1$
	protected static String PROPERTY_VALUE = "__pf_roperty_value"; //$NON-NLS-1$
	protected static String[] PARTITIONS = new String[] { COMMENT, PROPERTY_VALUE };

	private BasePDEScanner fPropertyKeyScanner;
	private BasePDEScanner fCommentScanner;
	private BasePDEScanner fPropertyValueScanner;
	private IColorManager fColorManager;
	
	private MonoReconciler fReconciler;
	private BuildSourcePage fSourcePage;
	private PDEQuickAssistAssistant fQuickAssistant;
	
	private abstract class AbstractJavaScanner extends BasePDEScanner {
		
	    public void adaptToPreferenceChange(PropertyChangeEvent event) {
	    	String property= event.getProperty();
	    	if (affectsTextPresentation(property)) {
	    		Token token = getTokenAffected(event);
	    		if (property.endsWith(PreferenceConstants.EDITOR_BOLD_SUFFIX))
	    			adaptToStyleChange(event, token, SWT.BOLD);
	    		else if (property.endsWith(PreferenceConstants.EDITOR_ITALIC_SUFFIX))
	    			adaptToStyleChange(event, token, SWT.ITALIC);
	    		else if (property.endsWith(PreferenceConstants.EDITOR_STRIKETHROUGH_SUFFIX))
	    			adaptToStyleChange(event, token, TextAttribute.STRIKETHROUGH);
	    		else if (property.endsWith(PreferenceConstants.EDITOR_UNDERLINE_SUFFIX))
	    			adaptToStyleChange(event, token, TextAttribute.UNDERLINE);
	    		else
	    			adaptToColorChange(event, token);
	    	}
	    }
		
		protected TextAttribute createTextAttribute(String property) {
			Color color = fColorManager.getColor(property);
			int style = SWT.NORMAL;
			if (fPreferenceStore.getBoolean(property + PreferenceConstants.EDITOR_BOLD_SUFFIX))
				style |= SWT.BOLD;
			if (fPreferenceStore.getBoolean(property + PreferenceConstants.EDITOR_ITALIC_SUFFIX))
				style |= SWT.ITALIC;
			if (fPreferenceStore.getBoolean(property + PreferenceConstants.EDITOR_STRIKETHROUGH_SUFFIX))
				style |= TextAttribute.STRIKETHROUGH;
			if (fPreferenceStore.getBoolean(property + PreferenceConstants.EDITOR_UNDERLINE_SUFFIX))
				style |= TextAttribute.UNDERLINE;
			return new TextAttribute(color, null, style);
		}
	}

	private class SingleTokenJavaScanner extends AbstractJavaScanner {

		private String fProperty;
		public SingleTokenJavaScanner(String property) {
			fProperty = property;
			setColorManager(fColorManager);
			initialize();
		}

		public boolean affectsTextPresentation(String property) {
			return property.startsWith(fProperty);
		}

		protected Token getTokenAffected(PropertyChangeEvent event) {
			return (Token)fDefaultReturnToken;
		}

		protected void initialize() {
			setDefaultReturnToken(new Token(createTextAttribute(fProperty)));			
		}
	}

	public class PropertyValueScanner extends AbstractJavaScanner {

		public class AssignmentDetector implements IWordDetector {

			public boolean isWordStart(char c) {
				if ('=' != c && ':' != c || fDocument == null)
					return false;

				try {
					// check whether it is the first '='
					IRegion lineInfo = fDocument.getLineInformationOfOffset(fOffset);
					int offset = lineInfo.getOffset();
					String line = fDocument.get(offset, lineInfo.getLength());
					int i = line.indexOf(c);
					return i != -1 && i + lineInfo.getOffset() + 1 == fOffset;
				} catch (BadLocationException ex) {
					return false;
				}
			}

			public boolean isWordPart(char c) {
				return false;
			}
		}	

		private Token fArgumentToken;
		private Token fAssignmentToken;

		public PropertyValueScanner() {
			setColorManager(fColorManager);
			initialize();
		}
		
		public boolean affectsTextPresentation(String property) {
			return property.startsWith(PreferenceConstants.PROPERTIES_FILE_COLORING_VALUE)
					|| property.startsWith(PreferenceConstants.PROPERTIES_FILE_COLORING_ARGUMENT) 
					|| property.startsWith(PreferenceConstants.PROPERTIES_FILE_COLORING_ASSIGNMENT);
		}

		protected Token getTokenAffected(PropertyChangeEvent event) {
			String property = event.getProperty();
			if (property.startsWith(PreferenceConstants.PROPERTIES_FILE_COLORING_ARGUMENT))
				return fArgumentToken;
			if (property.startsWith(PreferenceConstants.PROPERTIES_FILE_COLORING_ASSIGNMENT))
				return fAssignmentToken;
			return (Token)fDefaultReturnToken;
		}

		protected void initialize() {
			IRule[] rules = new IRule[3];
			fArgumentToken = new Token(createTextAttribute(PreferenceConstants.PROPERTIES_FILE_COLORING_ARGUMENT));			
			rules[0] = new ArgumentRule(fArgumentToken);
			
			fAssignmentToken = new Token(createTextAttribute(PreferenceConstants.PROPERTIES_FILE_COLORING_ASSIGNMENT));
			rules[1] = new WordRule(new AssignmentDetector(), fAssignmentToken);
			
			rules[2] = new WhitespaceRule(new IWhitespaceDetector() {
				public boolean isWhitespace(char c) {
					return Character.isWhitespace(c);
				}			
			});
			setRules(rules);
			setDefaultReturnToken(new Token(createTextAttribute(PreferenceConstants.PROPERTIES_FILE_COLORING_VALUE)));
		}
	}
	
	public BuildSourceViewerConfiguration(IColorManager colorManager, IPreferenceStore store, BuildSourcePage sourcePage) {
		super(store);
		fColorManager = colorManager;
		fSourcePage = sourcePage;
		initializeScanners();
	}


	private void initializeScanners() {
		fPropertyKeyScanner = new SingleTokenJavaScanner(PreferenceConstants.PROPERTIES_FILE_COLORING_KEY);
		fPropertyValueScanner = new PropertyValueScanner();
		fCommentScanner = new SingleTokenJavaScanner(PreferenceConstants.PROPERTIES_FILE_COLORING_COMMENT);
	}

	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
		PresentationReconciler reconciler = new PresentationReconciler();
		reconciler.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));

		DefaultDamagerRepairer dr = new DefaultDamagerRepairer(fPropertyKeyScanner);
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

		dr = new DefaultDamagerRepairer(fCommentScanner);
		reconciler.setDamager(dr, COMMENT);
		reconciler.setRepairer(dr, COMMENT);

		dr = new DefaultDamagerRepairer(fPropertyValueScanner);
		reconciler.setDamager(dr, PROPERTY_VALUE);
		reconciler.setRepairer(dr, PROPERTY_VALUE);

		return reconciler;
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
	
	public void handlePropertyChangeEvent(PropertyChangeEvent event) {
		if (affectsColorPresentation(event.getProperty()))
			fColorManager.handlePropertyChangeEvent(event);
		fPropertyKeyScanner.adaptToPreferenceChange(event);
		fCommentScanner.adaptToPreferenceChange(event);
		fPropertyValueScanner.adaptToPreferenceChange(event);
	}
	
	public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
		int length = PARTITIONS.length;
		String[] contentTypes = new String[length + 1];
		contentTypes[0] = IDocument.DEFAULT_CONTENT_TYPE;
		for (int i = 0; i < length; i++)
			contentTypes[i+1] = PARTITIONS[i];

		return contentTypes;
	}

	public String getConfiguredDocumentPartitioning(ISourceViewer sourceViewer) {
		return PROPERTIES_FILE_PARTITIONING;
	}
	
	public boolean affectsTextPresentation(PropertyChangeEvent event) {
		String property = event.getProperty();
		return fCommentScanner.affectsTextPresentation(property)
				|| fPropertyKeyScanner.affectsTextPresentation(property)
				|| fPropertyValueScanner.affectsTextPresentation(property);
	}
	
	private boolean affectsColorPresentation(String property) {
		 return property.equals(PreferenceConstants.PROPERTIES_FILE_COLORING_VALUE)
					|| property.equals(PreferenceConstants.PROPERTIES_FILE_COLORING_ARGUMENT) 
					|| property.equals(PreferenceConstants.PROPERTIES_FILE_COLORING_ASSIGNMENT)
	 				|| property.equals(PreferenceConstants.PROPERTIES_FILE_COLORING_KEY)
	 				|| property.equals(PreferenceConstants.PROPERTIES_FILE_COLORING_COMMENT);
	}
	
	public IQuickAssistAssistant getQuickAssistAssistant(ISourceViewer sourceViewer) {
		if (sourceViewer.isEditable()) {
			if (fQuickAssistant == null)
				fQuickAssistant = new PDEQuickAssistAssistant();
			return fQuickAssistant;
		}
		return null;
	}
	
	public void dispose() {
		if (fQuickAssistant != null)
			fQuickAssistant.dispose();
	}
}

