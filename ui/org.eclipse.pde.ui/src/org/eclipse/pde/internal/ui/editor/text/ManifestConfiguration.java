/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
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
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.IRule;
import org.eclipse.jface.text.rules.IWordDetector;
import org.eclipse.jface.text.rules.Token;
import org.eclipse.jface.text.rules.WordRule;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.pde.internal.ui.PDEPlugin;

public class ManifestConfiguration extends ChangeAwareSourceViewerConfiguration {

	private IAnnotationHover fAnnotationHover;
	private IColorManager fColorManager;
	private BasePDEScanner fPropertyKeyScanner;
	private BasePDEScanner fPropertyValueScanner;
	
	class ManifestHeaderScanner extends BasePDEScanner {
		
		public ManifestHeaderScanner() {
			super(fColorManager);
		}

		public boolean affectsTextPresentation(String property) {
			return property.startsWith(IPDEColorConstants.P_HEADER_KEY);
		}
		
		protected Token getTokenAffected(PropertyChangeEvent event) {
			return (Token)fDefaultReturnToken;
		}
		
		protected void initialize() {
			setDefaultReturnToken(new Token(createTextAttribute(IPDEColorConstants.P_HEADER_KEY)));
		}	
	}
	
	class ManifestValueScanner extends BasePDEScanner {
		
		private Token fAssignmentToken;

		public ManifestValueScanner() {
			super(fColorManager);
		}

		public boolean affectsTextPresentation(String property) {
			return property.startsWith(IPDEColorConstants.P_HEADER_ASSIGNMENT)
					|| property.startsWith(IPDEColorConstants.P_HEADER_VALUE);
		}

		protected Token getTokenAffected(PropertyChangeEvent event) {
			String property = event.getProperty();
			if (property.startsWith(IPDEColorConstants.P_HEADER_ASSIGNMENT))
				return fAssignmentToken;
			return (Token)fDefaultReturnToken;
		}

		protected void initialize() {
			IRule[] rules = new IRule[1];
			fAssignmentToken = new Token(createTextAttribute(IPDEColorConstants.P_HEADER_ASSIGNMENT));
			rules[0] = new WordRule(new AssignmentDetector(), fAssignmentToken);
			setRules(rules);
			setDefaultReturnToken(new Token(createTextAttribute(IPDEColorConstants.P_HEADER_VALUE)));
		}	
	}
	
	class AssignmentDetector implements IWordDetector {
		public boolean isWordStart(char c) {
			return c == ':' || c == '=' || c == ';' || c == ',';
		}

		public boolean isWordPart(char c) {
			return false;
		}	
	}

	public ManifestConfiguration(IColorManager manager) {
		super(PDEPlugin.getDefault().getPreferenceStore());
		fColorManager = manager;
		fPropertyKeyScanner = new ManifestHeaderScanner();
		fPropertyValueScanner = new ManifestValueScanner();
	}
	
	public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
		String[] partitions = ManifestPartitionScanner.PARTITIONS;
		String[] all = new String[partitions.length + 1];
		all[0] = IDocument.DEFAULT_CONTENT_TYPE;
		System.arraycopy(partitions, 0, all, 1, partitions.length);
		return all;
	}
	
	public IAnnotationHover getAnnotationHover(ISourceViewer sourceViewer) {
		if (fAnnotationHover == null)
			fAnnotationHover = new AnnotationHover();
		return fAnnotationHover;
	}
	
	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
		PresentationReconciler reconciler = new PresentationReconciler();
		reconciler.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));

		DefaultDamagerRepairer dr = new DefaultDamagerRepairer(fPropertyKeyScanner);
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

		dr = new DefaultDamagerRepairer(fPropertyValueScanner);
		reconciler.setDamager(dr, ManifestPartitionScanner.MANIFEST_HEADER_VALUE);
		reconciler.setRepairer(dr, ManifestPartitionScanner.MANIFEST_HEADER_VALUE);

		return reconciler;
	}
	
	public boolean affectsTextPresentation(PropertyChangeEvent event) {
		String property = event.getProperty();
		return property.startsWith(IPDEColorConstants.P_HEADER_KEY) ||
			property.startsWith(IPDEColorConstants.P_HEADER_VALUE) || 
			property.startsWith(IPDEColorConstants.P_HEADER_ASSIGNMENT);
	}

	public boolean affectsColorPresentation(PropertyChangeEvent event) {
		String property = event.getProperty();
		return property.equals(IPDEColorConstants.P_HEADER_KEY) ||
			property.equals(IPDEColorConstants.P_HEADER_VALUE) ||
			property.equals(IPDEColorConstants.P_HEADER_ASSIGNMENT);
	}

	public void adaptToPreferenceChange(PropertyChangeEvent event) {
		if (affectsColorPresentation(event))
			fColorManager.handlePropertyChangeEvent(event);
		fPropertyKeyScanner.adaptToPreferenceChange(event);
		fPropertyValueScanner.adaptToPreferenceChange(event);
	}
	
	public void handlePropertyChangeEvent(PropertyChangeEvent event) {
		if (affectsColorPresentation(event))
			fColorManager.handlePropertyChangeEvent(event);
		fPropertyKeyScanner.adaptToPreferenceChange(event);
		fPropertyValueScanner.adaptToPreferenceChange(event);
	}


}
