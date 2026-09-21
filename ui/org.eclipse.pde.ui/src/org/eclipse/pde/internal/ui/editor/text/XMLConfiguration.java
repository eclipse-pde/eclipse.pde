/*******************************************************************************
 *  Copyright (c) 2003, 2026 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.text;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.reconciler.Reconciler;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.internal.core.text.IReconcilingParticipant;
import org.eclipse.pde.internal.core.text.plugin.PluginModel;
import org.eclipse.pde.internal.ui.editor.ISortableContentOutlinePage;
import org.eclipse.pde.internal.ui.editor.PDESourcePage;
import org.eclipse.pde.internal.ui.editor.context.XMLDocumentSetupParticpant;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.SyntaxThemeConstants;
import org.eclipse.ui.themes.IThemeManager;

/**
 * Source viewer configuration for the XML Source editors
 */
public class XMLConfiguration extends ChangeAwareSourceViewerConfiguration {
	private AnnotationHover fAnnotationHover;
	private XMLDoubleClickStrategy fDoubleClickStrategy;
	private XMLTagScanner fTagScanner;
	private XMLScanner fPdeScanner;

	public XMLConfiguration(IColorManager colorManager) {
		this(colorManager, null);
	}

	public XMLConfiguration(IColorManager colorManager, PDESourcePage page) {
		super(page, colorManager);
	}

	@Override
	public String[] getConfiguredContentTypes(ISourceViewer sourceViewer) {
		return new String[] {IDocument.DEFAULT_CONTENT_TYPE, XMLPartitionScanner.XML_COMMENT, XMLPartitionScanner.XML_TAG, XMLStringPartitionScanner.XML_STRING};
	}

	@Override
	public ITextDoubleClickStrategy getDoubleClickStrategy(ISourceViewer sourceViewer, String contentType) {
		if (fDoubleClickStrategy == null) {
			fDoubleClickStrategy = new XMLDoubleClickStrategy();
		}
		return fDoubleClickStrategy;
	}

	protected XMLScanner getPDEScanner() {
		if (fPdeScanner == null) {
			fPdeScanner = new XMLScanner();
		}
		return fPdeScanner;
	}

	protected XMLTagScanner getPDETagScanner() {
		if (fTagScanner == null) {
			fTagScanner = new XMLTagScanner();
		}
		return fTagScanner;
	}

	@Override
	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
		MultilineDamagerRepairer commentDamagerRepairer = new MultilineDamagerRepairer(getPDETagScanner(), createCommentAttribute());
		PresentationReconciler reconciler = new ThemeAwarePresentationReconciler(commentDamagerRepairer);
		reconciler.setDocumentPartitioning(XMLDocumentSetupParticpant.XML_PARTITIONING);

		MultilineDamagerRepairer dr = new MultilineDamagerRepairer(getPDEScanner());
		reconciler.setDamager(dr, IDocument.DEFAULT_CONTENT_TYPE);
		reconciler.setRepairer(dr, IDocument.DEFAULT_CONTENT_TYPE);

		dr = new MultilineDamagerRepairer(getPDETagScanner());
		reconciler.setDamager(dr, XMLPartitionScanner.XML_TAG);
		reconciler.setRepairer(dr, XMLPartitionScanner.XML_TAG);

		reconciler.setDamager(commentDamagerRepairer, XMLPartitionScanner.XML_COMMENT);
		reconciler.setRepairer(commentDamagerRepairer, XMLPartitionScanner.XML_COMMENT);

		return reconciler;
	}

	private static TextAttribute createCommentAttribute() {
		return new TextAttribute(XMLSyntaxColors.get(SyntaxThemeConstants.COMMENT_COLOR));
	}

	private static boolean isSyntaxColorChange(String property) {
		return IThemeManager.CHANGE_CURRENT_THEME.equals(property) || property.equals(SyntaxThemeConstants.TAG_COLOR)
				|| property.equals(SyntaxThemeConstants.ATTRIBUTE_NAME_COLOR) || property.equals(SyntaxThemeConstants.STRING_COLOR)
				|| property.equals(SyntaxThemeConstants.COMMENT_COLOR) || property.equals(SyntaxThemeConstants.DIRECTIVE_COLOR)
				|| property.equals(XMLSyntaxColors.EXTERNALIZED_STRING_COLOR);
	}

	/**
	 * Redraws the viewer with the new colors when the workbench theme or one of
	 * its colors changes.
	 */
	private class ThemeAwarePresentationReconciler extends PresentationReconciler {

		private final MultilineDamagerRepairer fCommentDamagerRepairer;

		private ITextViewer fViewer;

		private final IPropertyChangeListener fThemeListener = this::themeChanged;

		ThemeAwarePresentationReconciler(MultilineDamagerRepairer commentDamagerRepairer) {
			fCommentDamagerRepairer = commentDamagerRepairer;
		}

		private void themeChanged(PropertyChangeEvent event) {
			if (!isSyntaxColorChange(event.getProperty())) {
				return;
			}
			ITextViewer viewer = fViewer;
			if (viewer == null) {
				return;
			}
			StyledText widget = viewer.getTextWidget();
			if (widget == null || widget.isDisposed()) {
				return;
			}
			widget.getDisplay().asyncExec(() -> {
				if (!widget.isDisposed()) {
					getPDEScanner().updateColors();
					getPDETagScanner().updateColors();
					fCommentDamagerRepairer.setDefaultTextAttribute(createCommentAttribute());
					viewer.invalidateTextPresentation();
				}
			});
		}

		@Override
		public void install(ITextViewer viewer) {
			super.install(viewer);
			fViewer = viewer;
			if (PlatformUI.isWorkbenchRunning()) {
				PlatformUI.getWorkbench().getThemeManager().addPropertyChangeListener(fThemeListener);
			}
		}

		@Override
		public void uninstall() {
			if (PlatformUI.isWorkbenchRunning()) {
				PlatformUI.getWorkbench().getThemeManager().removePropertyChangeListener(fThemeListener);
			}
			fViewer = null;
			super.uninstall();
		}
	}

	@Override
	public IAnnotationHover getAnnotationHover(ISourceViewer sourceViewer) {
		if (fAnnotationHover == null) {
			fAnnotationHover = new AnnotationHover();
		}
		return fAnnotationHover;
	}

	@Override
	public void adaptToPreferenceChange(PropertyChangeEvent event) {
		// the XML colors come from the workbench theme, see ThemeAwarePresentationReconciler
	}

	@Override
	public boolean affectsTextPresentation(PropertyChangeEvent event) {
		return false;
	}

	@Override
	public boolean affectsColorPresentation(PropertyChangeEvent event) {
		return false;
	}

	@Override
	protected int getInfoImplementationType() {
		return SourceInformationProvider.F_XML_IMP;
	}

	@Override
	public String getConfiguredDocumentPartitioning(ISourceViewer sourceViewer) {
		return XMLDocumentSetupParticpant.XML_PARTITIONING;
	}

	@Override
	public IReconciler getReconciler(ISourceViewer sourceViewer) {
		if (fSourcePage != null && fReconciler == null) {
			IBaseModel model = fSourcePage.getInputContext().getModel();
			if (model instanceof IReconcilingParticipant) {
				ReconcilingStrategy strategy = new ReconcilingStrategy();
				strategy.addParticipant((IReconcilingParticipant) model);
				ISortableContentOutlinePage outline = fSourcePage.getContentOutline();
				if (outline instanceof IReconcilingParticipant) {
					strategy.addParticipant((IReconcilingParticipant) outline);
				}

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
