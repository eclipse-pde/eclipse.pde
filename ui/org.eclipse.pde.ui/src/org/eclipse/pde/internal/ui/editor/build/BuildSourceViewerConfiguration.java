/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.build;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.ui.PreferenceConstants;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.reconciler.MonoReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.IWhitespaceDetector;
import org.eclipse.jface.text.rules.IWordDetector;
import org.eclipse.jface.text.rules.WhitespaceRule;
import org.eclipse.jface.text.rules.WordRule;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.internal.core.text.IReconcilingParticipant;
import org.eclipse.pde.internal.ui.editor.text.AbstractJavaScanner;
import org.eclipse.pde.internal.ui.editor.text.ColorManager;
import org.eclipse.pde.internal.ui.editor.text.IColorManager;
import org.eclipse.pde.internal.ui.editor.text.ReconcilingStrategy;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;


public class BuildSourceViewerConfiguration extends TextSourceViewerConfiguration {

	protected static String PROPERTIES_FILE_PARTITIONING = "___pf_partitioning";  //$NON-NLS-1$
	protected static String COMMENT = "__pf_comment"; //$NON-NLS-1$
	protected static String PROPERTY_VALUE = "__pf_roperty_value"; //$NON-NLS-1$
	protected static String[] PARTITIONS = new String[] { COMMENT, PROPERTY_VALUE };

	private AbstractJavaScanner fPropertyKeyScanner;
	private AbstractJavaScanner fCommentScanner;
	private AbstractJavaScanner fPropertyValueScanner;
	private IColorManager fColorManager;
	private MonoReconciler fReconciler;
	private BuildSourcePage fSourcePage;

	private class SingleTokenJavaScanner extends AbstractJavaScanner{

		private String[] fProperty;

		public SingleTokenJavaScanner(IColorManager manager, IPreferenceStore store, String property) {
			super(manager, store);
			fProperty = new String[] { property };
			initialize();
		}

		protected String[] getTokenProperties() {
			return fProperty;
		}

		protected List createRules() {
			setDefaultReturnToken(getToken(fProperty[0]));
			return null;
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

		public PropertyValueScanner(IColorManager manager, IPreferenceStore store) {
			super(manager, store);
			initialize();
		}

		protected String[] getTokenProperties() {
			return new String[] {
					PreferenceConstants.PROPERTIES_FILE_COLORING_VALUE,
					PreferenceConstants.PROPERTIES_FILE_COLORING_ARGUMENT,
					PreferenceConstants.PROPERTIES_FILE_COLORING_ASSIGNMENT
			};
		}

		protected List createRules() {
			setDefaultReturnToken(getToken(PreferenceConstants.PROPERTIES_FILE_COLORING_VALUE));
			List rules = new ArrayList();
			rules.add(new WordRule(new AssignmentDetector(), getToken(PreferenceConstants.PROPERTIES_FILE_COLORING_ASSIGNMENT)));
			rules.add(new WhitespaceRule(new IWhitespaceDetector() {
				public boolean isWhitespace(char c) {
					return Character.isWhitespace(c);
				}
				
			}));
			return rules;
		}
	}
	
	public BuildSourceViewerConfiguration(IColorManager colorManager, IPreferenceStore store, BuildSourcePage sourcePage) {
		super(store);
		fColorManager = colorManager;
		fSourcePage = sourcePage;
		initializeScanners();
	}


	private void initializeScanners() {
		fPropertyKeyScanner = new SingleTokenJavaScanner(fColorManager, fPreferenceStore, PreferenceConstants.PROPERTIES_FILE_COLORING_KEY);
		fPropertyValueScanner = new PropertyValueScanner(fColorManager, fPreferenceStore);
		fCommentScanner = new SingleTokenJavaScanner(fColorManager, fPreferenceStore, PreferenceConstants.PROPERTIES_FILE_COLORING_COMMENT);
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
		((ColorManager)fColorManager).handlePropertyChangeEvent(event);
		if (fPropertyKeyScanner.affectsBehavior(event))
			fPropertyKeyScanner.adaptToPreferenceChange(event);
		if (fCommentScanner.affectsBehavior(event))
			fCommentScanner.adaptToPreferenceChange(event);
		if (fPropertyValueScanner.affectsBehavior(event))
			fPropertyValueScanner.adaptToPreferenceChange(event);
	}
	
	public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
		int length = PARTITIONS.length;
		String[] contentTypes = new String[length + 1];
		contentTypes[0] = IDocument.DEFAULT_CONTENT_TYPE;
		for (int i= 0; i < length; i++)
			contentTypes[i+1] = PARTITIONS[i];

		return contentTypes;
	}

	public String getConfiguredDocumentPartitioning(ISourceViewer sourceViewer) {
		return PROPERTIES_FILE_PARTITIONING;
	}
	
	public boolean affectsTextPresentation(PropertyChangeEvent event) {
		return  fPropertyKeyScanner.affectsBehavior(event)
			|| fCommentScanner.affectsBehavior(event)
			|| fPropertyValueScanner.affectsBehavior(event);
	}

}

