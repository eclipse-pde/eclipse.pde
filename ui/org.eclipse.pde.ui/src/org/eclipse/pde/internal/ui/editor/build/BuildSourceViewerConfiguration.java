/*******************************************************************************
 *  Copyright (c) 2000, 2010 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.build;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.*;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.pde.internal.ui.editor.PDESourcePage;
import org.eclipse.pde.internal.ui.editor.text.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;

public class BuildSourceViewerConfiguration extends ChangeAwareSourceViewerConfiguration {

	protected static String PROPERTIES_FILE_PARTITIONING = "___pf_partitioning"; //$NON-NLS-1$
	protected static String COMMENT = "__pf_comment"; //$NON-NLS-1$
	protected static String PROPERTY_VALUE = "__pf_roperty_value"; //$NON-NLS-1$
	protected static String[] PARTITIONS = new String[] {COMMENT, PROPERTY_VALUE};

	private BasePDEScanner fPropertyKeyScanner;
	private BasePDEScanner fCommentScanner;
	private BasePDEScanner fPropertyValueScanner;

	private abstract class AbstractJavaScanner extends BasePDEScanner {

		public void adaptToPreferenceChange(PropertyChangeEvent event) {
			String property = event.getProperty();
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
			return (Token) fDefaultReturnToken;
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
			return property.startsWith(PreferenceConstants.PROPERTIES_FILE_COLORING_VALUE) || property.startsWith(PreferenceConstants.PROPERTIES_FILE_COLORING_ARGUMENT) || property.startsWith(PreferenceConstants.PROPERTIES_FILE_COLORING_ASSIGNMENT);
		}

		protected Token getTokenAffected(PropertyChangeEvent event) {
			String property = event.getProperty();
			if (property.startsWith(PreferenceConstants.PROPERTIES_FILE_COLORING_ARGUMENT))
				return fArgumentToken;
			if (property.startsWith(PreferenceConstants.PROPERTIES_FILE_COLORING_ASSIGNMENT))
				return fAssignmentToken;
			return (Token) fDefaultReturnToken;
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

	public BuildSourceViewerConfiguration(IColorManager colorManager, IPreferenceStore store, PDESourcePage sourcePage) {
		super(sourcePage, colorManager, store);
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

	public void adaptToPreferenceChange(PropertyChangeEvent event) {
		if (affectsColorPresentation(event))
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
			contentTypes[i + 1] = PARTITIONS[i];

		return contentTypes;
	}

	public String getConfiguredDocumentPartitioning(ISourceViewer sourceViewer) {
		return PROPERTIES_FILE_PARTITIONING;
	}

	public boolean affectsTextPresentation(PropertyChangeEvent event) {
		String property = event.getProperty();
		return fCommentScanner.affectsTextPresentation(property) || fPropertyKeyScanner.affectsTextPresentation(property) || fPropertyValueScanner.affectsTextPresentation(property);
	}

	public boolean affectsColorPresentation(PropertyChangeEvent event) {
		String property = event.getProperty();
		return property.equals(PreferenceConstants.PROPERTIES_FILE_COLORING_VALUE) || property.equals(PreferenceConstants.PROPERTIES_FILE_COLORING_ARGUMENT) || property.equals(PreferenceConstants.PROPERTIES_FILE_COLORING_ASSIGNMENT) || property.equals(PreferenceConstants.PROPERTIES_FILE_COLORING_KEY) || property.equals(PreferenceConstants.PROPERTIES_FILE_COLORING_COMMENT);
	}
}
