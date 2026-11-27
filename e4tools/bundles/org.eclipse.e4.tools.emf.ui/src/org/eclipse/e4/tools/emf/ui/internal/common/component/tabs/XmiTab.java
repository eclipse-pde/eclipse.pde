/*******************************************************************************
 * Copyright (c) 2014, 2017 TwelveTone LLC and others.
 * Copyright (c) 2010-2014 BestSolution.at and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Steven Spungin <steven@spungin.tv> - initial API and implementation, Bug 391089, Bug 437543, Ongoing Maintenance
 *******************************************************************************/

package org.eclipse.e4.tools.emf.ui.internal.common.component.tabs;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.services.nls.Translation;
import org.eclipse.e4.tools.emf.ui.common.ModelEditorPreferences;
import org.eclipse.e4.tools.emf.ui.internal.Messages;
import org.eclipse.e4.tools.emf.ui.internal.common.xml.AnnotationAccess;
import org.eclipse.e4.tools.emf.ui.internal.common.xml.EMFDocumentResourceMediator;
import org.eclipse.e4.tools.emf.ui.internal.common.xml.XMLConfiguration;
import org.eclipse.e4.tools.emf.ui.internal.common.xml.XMLPartitionScanner;
import org.eclipse.e4.tools.services.IResourcePool;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource.Diagnostic;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.rules.FastPartitioner;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.AnnotationModel;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.VerticalRuler;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.AbstractTextEditor;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

public class XmiTab extends Composite implements IAdaptable {

	private static final String ORG_ECLIPSE_E4_TOOLS_MODELEDITOR_FILTEREDTREE_ENABLED_XMITAB_DISABLED = "org.eclipse.e4.tools.modeleditor.filteredtree.enabled.xmitab.disabled";//$NON-NLS-1$
	private static final int VERTICAL_RULER_WIDTH = 20;

	@Inject
	private IEclipseContext context;

	@Optional
	@Inject
	private IProject project;
	@Inject
	private EMFDocumentResourceMediator emfDocumentProvider;
	@Inject
	private IResourcePool resourcePool;
	@Inject
	private IEclipsePreferences preferences;

	@Inject
	@Translation
	protected Messages Messages;

	private SourceViewer sourceViewer;

	@Inject
	public XmiTab(Composite parent) {
		super(parent, SWT.NONE);
		setLayout(new GridLayout(1, false));
	}

	@PostConstruct
	protected void postConstruct() {
		final AnnotationModel model = new AnnotationModel();
		final VerticalRuler verticalRuler = new VerticalRuler(VERTICAL_RULER_WIDTH, new AnnotationAccess(resourcePool));
		final int styles = SWT.V_SCROLL | SWT.H_SCROLL | SWT.MULTI | SWT.BORDER | SWT.FULL_SELECTION;
		sourceViewer = new SourceViewer(this, verticalRuler, styles);
		sourceViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		sourceViewer.configure(new XMLConfiguration());
		sourceViewer.setEditable(project != null);
		sourceViewer.getTextWidget().setFont(JFaceResources.getTextFont());
		applyTextEditorColors(sourceViewer.getTextWidget());

		final IDocument document = emfDocumentProvider.getDocument();
		final IDocumentPartitioner partitioner = new FastPartitioner(new XMLPartitionScanner(), new String[] {
				XMLPartitionScanner.XML_TAG, XMLPartitionScanner.XML_COMMENT });
		partitioner.connect(document);
		document.setDocumentPartitioner(partitioner);
		sourceViewer.setDocument(document);
		verticalRuler.setModel(model);

		emfDocumentProvider.setValidationChangedCallback(() -> {
			model.removeAllAnnotations();

			for (final Diagnostic d : emfDocumentProvider.getErrorList()) {
				final Annotation a = new Annotation("e4xmi.error", false, d.getMessage()); //$NON-NLS-1$
				int l;
				try {
					l = document.getLineOffset(d.getLine() - 1);
					model.addAnnotation(a, new Position(l));
				} catch (final BadLocationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});

		final String property = System
				.getProperty(ORG_ECLIPSE_E4_TOOLS_MODELEDITOR_FILTEREDTREE_ENABLED_XMITAB_DISABLED);
		if (property != null || preferences.getBoolean(ModelEditorPreferences.TAB_FORM_SEARCH_SHOW, true)) {
			sourceViewer.setEditable(false);
		}
	}

	/**
	 * Takes foreground and background from the Text Editors preferences, so that the
	 * tab follows the active theme instead of the widget defaults.
	 */
	private static void applyTextEditorColors(StyledText widget) {
		final IPreferenceStore store = EditorsUI.getPreferenceStore();
		final Color foreground = editorColor(store, AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND,
				AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND_SYSTEM_DEFAULT);
		if (foreground != null) {
			widget.setForeground(foreground);
		}
		final Color background = editorColor(store, AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND,
				AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND_SYSTEM_DEFAULT);
		if (background != null) {
			widget.setBackground(background);
		}
	}

	private static Color editorColor(IPreferenceStore store, String key, String systemDefaultKey) {
		if (store.getBoolean(systemDefaultKey)) {
			return null;
		}
		return new Color(PreferenceConverter.getColor(store, key));
	}

	public IEclipseContext getContext() {
		return context;
	}

	public void gotoEObject(EObject object) {
		// select the entire start tag
		final IRegion region = emfDocumentProvider.findStartTag(object);
		if (region != null) {
			sourceViewer.setSelection(new TextSelection(region.getOffset(), region.getLength()), true);
		} else {
			sourceViewer.setSelection(new TextSelection(0, 0), true);
		}
	}

	public void paste() {
		sourceViewer.getTextWidget().paste();
	}

	public void copy() {
		sourceViewer.getTextWidget().copy();
	}

	public void cut() {
		sourceViewer.getTextWidget().cut();
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == ITextViewer.class) {
			return adapter.cast(sourceViewer);
		}
		if (adapter == IFindReplaceTarget.class && sourceViewer != null) {
			return adapter.cast(sourceViewer.getFindReplaceTarget());
		}
		return null;
	}

}
