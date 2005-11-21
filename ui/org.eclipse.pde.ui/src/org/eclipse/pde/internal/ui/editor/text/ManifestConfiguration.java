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
import org.eclipse.pde.internal.core.ICoreConstants;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.osgi.framework.Constants;

public class ManifestConfiguration extends ChangeAwareSourceViewerConfiguration {

	private IAnnotationHover fAnnotationHover;
	private IColorManager fColorManager;
	private BasePDEScanner fPropertyKeyScanner;
	private BasePDEScanner fPropertyValueScanner;
	
	class ManifestHeaderScanner extends BasePDEScanner {
		
		private Token fToken;

		public ManifestHeaderScanner() {
			super(fColorManager);
		}

		public boolean affectsTextPresentation(String property) {
			return property.startsWith(IPDEColorConstants.P_HEADER_KEY)
					|| property.startsWith(IPDEColorConstants.P_HEADER_OSGI);
		}
		
		protected Token getTokenAffected(PropertyChangeEvent event) {
			if (event.getProperty().startsWith(IPDEColorConstants.P_HEADER_OSGI))
				return fToken;
			return (Token)fDefaultReturnToken;
		}
		
		protected void initialize() {
			fToken = new Token(createTextAttribute(IPDEColorConstants.P_HEADER_OSGI));
			WordRule rule = new WordRule(new KeywordDetector());
			rule.addWord(Constants.BUNDLE_ACTIVATOR, fToken);
			rule.addWord(Constants.BUNDLE_CATEGORY, fToken);
			rule.addWord(Constants.BUNDLE_CLASSPATH, fToken);
			rule.addWord(Constants.BUNDLE_CONTACTADDRESS, fToken);
			rule.addWord(Constants.BUNDLE_COPYRIGHT, fToken);
			rule.addWord(Constants.BUNDLE_DESCRIPTION, fToken);
			rule.addWord(Constants.BUNDLE_DOCURL, fToken);
			rule.addWord(Constants.BUNDLE_LOCALIZATION, fToken);
			rule.addWord(Constants.BUNDLE_MANIFESTVERSION, fToken);
			rule.addWord(Constants.BUNDLE_NAME, fToken);
			rule.addWord(Constants.BUNDLE_NATIVECODE, fToken);
			rule.addWord(Constants.BUNDLE_REQUIREDEXECUTIONENVIRONMENT, fToken);
			rule.addWord(Constants.BUNDLE_SYMBOLICNAME, fToken);
			rule.addWord(Constants.BUNDLE_UPDATELOCATION, fToken);
			rule.addWord(Constants.BUNDLE_VENDOR, fToken);
			rule.addWord(Constants.BUNDLE_VERSION, fToken);
			rule.addWord(Constants.REQUIRE_BUNDLE, fToken);
			rule.addWord(Constants.DYNAMICIMPORT_PACKAGE, fToken);
			rule.addWord(Constants.EXPORT_PACKAGE, fToken);
			rule.addWord(Constants.EXPORT_SERVICE, fToken);
			rule.addWord(Constants.FRAGMENT_HOST, fToken);
			rule.addWord(Constants.IMPORT_PACKAGE, fToken);
			rule.addWord(Constants.IMPORT_SERVICE, fToken);
			rule.addWord(ICoreConstants.PROVIDE_PACKAGE, fToken);
			setRules(new IRule[] {rule});
			setDefaultReturnToken(new Token(createTextAttribute(IPDEColorConstants.P_HEADER_KEY)));
		}	
	}
	
	class ManifestValueScanner extends BasePDEScanner {
		
		private Token fAssignmentToken;
		private Token fAttributeToken;

		public ManifestValueScanner() {
			super(fColorManager);
		}

		public boolean affectsTextPresentation(String property) {
			return property.startsWith(IPDEColorConstants.P_HEADER_ASSIGNMENT)
					|| property.startsWith(IPDEColorConstants.P_HEADER_VALUE)
					|| property.startsWith(IPDEColorConstants.P_HEADER_ATTRIBUTES);
		}

		protected Token getTokenAffected(PropertyChangeEvent event) {
			String property = event.getProperty();
			if (property.startsWith(IPDEColorConstants.P_HEADER_ASSIGNMENT))
				return fAssignmentToken;
			if (property.startsWith(IPDEColorConstants.P_HEADER_ATTRIBUTES))
				return fAttributeToken;
			return (Token)fDefaultReturnToken;
		}

		protected void initialize() {
			IRule[] rules = new IRule[2];
			fAssignmentToken = new Token(createTextAttribute(IPDEColorConstants.P_HEADER_ASSIGNMENT));
			rules[0] = new WordRule(new AssignmentDetector(), fAssignmentToken);

			fAttributeToken = new Token(createTextAttribute(IPDEColorConstants.P_HEADER_ATTRIBUTES));
			WordRule rule = new WordRule(new KeywordDetector());
			rule.addWord(Constants.BUNDLE_NATIVECODE_LANGUAGE, fAttributeToken);
			rule.addWord(Constants.BUNDLE_NATIVECODE_OSNAME, fAttributeToken);
			rule.addWord(Constants.BUNDLE_NATIVECODE_OSVERSION, fAttributeToken);
			rule.addWord(Constants.BUNDLE_NATIVECODE_PROCESSOR, fAttributeToken);
			rule.addWord(Constants.BUNDLE_SYMBOLICNAME_ATTRIBUTE, fAttributeToken);
			rule.addWord(Constants.BUNDLE_VERSION_ATTRIBUTE, fAttributeToken);
			rule.addWord(Constants.EXCLUDE_DIRECTIVE, fAttributeToken);
			rule.addWord(Constants.FRAGMENT_ATTACHMENT_DIRECTIVE, fAttributeToken);
			rule.addWord(Constants.INCLUDE_DIRECTIVE, fAttributeToken);
			rule.addWord(Constants.MANDATORY_DIRECTIVE, fAttributeToken);
			rule.addWord(Constants.RESOLUTION_DIRECTIVE, fAttributeToken);
			rule.addWord(Constants.SINGLETON_DIRECTIVE, fAttributeToken);
			rule.addWord(Constants.USES_DIRECTIVE, fAttributeToken);
			rule.addWord(Constants.VERSION_ATTRIBUTE, fAttributeToken);
			rule.addWord(Constants.VISIBILITY_DIRECTIVE, fAttributeToken);
			rule.addWord(ICoreConstants.FRIENDS_DIRECTIVE, fAttributeToken);
			rule.addWord(ICoreConstants.INTERNAL_DIRECTIVE, fAttributeToken);
			rules[1] = rule;
			
			setRules(rules);
			setDefaultReturnToken(new Token(createTextAttribute(IPDEColorConstants.P_HEADER_VALUE)));
		}	
	}
	
	class AssignmentDetector implements IWordDetector {
		public boolean isWordStart(char c) {
			return c == ':' || c == '=';
		}

		public boolean isWordPart(char c) {
			return false;
		}	
	}

	class KeywordDetector implements IWordDetector {
		public boolean isWordStart(char c) {
			return Character.isJavaIdentifierStart(c);
		}

		public boolean isWordPart(char c) {
			return c != ':' && c != '=' && !Character.isSpaceChar(c);
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
			property.startsWith(IPDEColorConstants.P_HEADER_OSGI) ||
			property.startsWith(IPDEColorConstants.P_HEADER_VALUE) || 
			property.startsWith(IPDEColorConstants.P_HEADER_ATTRIBUTES) ||
			property.startsWith(IPDEColorConstants.P_HEADER_ASSIGNMENT);
	}

	public boolean affectsColorPresentation(PropertyChangeEvent event) {
		String property = event.getProperty();
		return property.equals(IPDEColorConstants.P_HEADER_KEY) ||
			property.equals(IPDEColorConstants.P_HEADER_OSGI) ||
			property.equals(IPDEColorConstants.P_HEADER_VALUE) ||
			property.equals(IPDEColorConstants.P_HEADER_ATTRIBUTES) ||
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
