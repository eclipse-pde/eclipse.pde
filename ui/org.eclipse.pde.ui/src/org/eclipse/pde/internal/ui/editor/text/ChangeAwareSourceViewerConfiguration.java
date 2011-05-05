/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.text;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.text.information.*;
import org.eclipse.jface.text.quickassist.IQuickAssistAssistant;
import org.eclipse.jface.text.reconciler.*;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.internal.core.text.IReconcilingParticipant;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.ISortableContentOutlinePage;
import org.eclipse.pde.internal.ui.editor.PDESourcePage;
import org.eclipse.pde.internal.ui.editor.actions.PDEActionConstants;
import org.eclipse.pde.internal.ui.editor.outline.QuickOutlinePopupDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;
import org.eclipse.ui.texteditor.ChainedPreferenceStore;

public abstract class ChangeAwareSourceViewerConfiguration extends TextSourceViewerConfiguration {

	protected PDESourcePage fSourcePage;
	protected IColorManager fColorManager;
	protected AbstractReconciler fReconciler;
	private InformationPresenter fInfoPresenter;
	private InformationPresenter fOutlinePresenter;
	private PDEQuickAssistAssistant fQuickAssistant;

	/**
	 * @param page
	 * @param manager - an IColorManager, clients must dispose this themselves.
	 * @param store
	 */
	public ChangeAwareSourceViewerConfiguration(PDESourcePage page, IColorManager manager, IPreferenceStore store) {
		super(store);
		fColorManager = manager;
		fSourcePage = page;
	}

	public ChangeAwareSourceViewerConfiguration(PDESourcePage page, IColorManager manager) {
		this(page, manager, new ChainedPreferenceStore(new IPreferenceStore[] {PDEPlugin.getDefault().getPreferenceStore(), EditorsUI.getPreferenceStore() // general text editor store
				}));
	}

	public IReconciler getReconciler(ISourceViewer sourceViewer) {
		if (fSourcePage != null && fReconciler == null) {
			IBaseModel model = fSourcePage.getInputContext().getModel();
			if (model instanceof IReconcilingParticipant) {
				ReconcilingStrategy strategy = new ReconcilingStrategy();
				strategy.addParticipant((IReconcilingParticipant) model);
				ISortableContentOutlinePage outline = fSourcePage.getContentOutline();
				if (outline instanceof IReconcilingParticipant)
					strategy.addParticipant((IReconcilingParticipant) outline);
				fReconciler = new MonoReconciler(strategy, false);
				fReconciler.setDelay(500);
			}
		}
		return fReconciler;
	}

	public IInformationPresenter getInformationPresenter(ISourceViewer sourceViewer) {
		if (fSourcePage == null)
			return null;
		if (fInfoPresenter == null && getInfoImplementationType() != SourceInformationProvider.F_NO_IMP) {
			IInformationControlCreator icc = getInformationControlCreator(false);
			fInfoPresenter = new InformationPresenter(icc);
			fInfoPresenter.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));

			// Register information provider
			IInformationProvider provider = new SourceInformationProvider(fSourcePage, icc, getInfoImplementationType());
			String[] contentTypes = getConfiguredContentTypes(sourceViewer);
			for (int i = 0; i < contentTypes.length; i++)
				fInfoPresenter.setInformationProvider(provider, contentTypes[i]);

			fInfoPresenter.setSizeConstraints(60, 10, true, true);
		}
		return fInfoPresenter;
	}

	public IInformationPresenter getOutlinePresenter(ISourceViewer sourceViewer) {
		// Ensure the source page is defined
		if (fSourcePage == null) {
			return null;
		}
		// Reuse the old outline presenter
		if (fOutlinePresenter != null) {
			return fOutlinePresenter;
		}
		// Define a new outline presenter
		fOutlinePresenter = new InformationPresenter(getOutlinePresenterControlCreator(sourceViewer, PDEActionConstants.COMMAND_ID_QUICK_OUTLINE));
		fOutlinePresenter.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));
		fOutlinePresenter.setAnchor(AbstractInformationControlManager.ANCHOR_GLOBAL);
		// Define a new outline provider
		IInformationProvider provider = new PDESourceInfoProvider(fSourcePage);
		// Set the provider on all defined content types
		String[] contentTypes = getConfiguredContentTypes(sourceViewer);
		for (int i = 0; i < contentTypes.length; i++) {
			fOutlinePresenter.setInformationProvider(provider, contentTypes[i]);
		}
		// Set the presenter size constraints
		fOutlinePresenter.setSizeConstraints(50, 20, true, false);

		return fOutlinePresenter;
	}

	/**
	 * Returns the outline presenter control creator. The creator is a 
	 * factory creating outline presenter controls for the given source viewer. 
	 *
	 * @param sourceViewer the source viewer to be configured by this configuration
	 * @param commandId the ID of the command that opens this control
	 * @return an information control creator
	 */
	private IInformationControlCreator getOutlinePresenterControlCreator(ISourceViewer sourceViewer, final String commandId) {
		return new IInformationControlCreator() {
			public IInformationControl createInformationControl(Shell parent) {
				int shellStyle = SWT.RESIZE;
				QuickOutlinePopupDialog dialog = new QuickOutlinePopupDialog(parent, shellStyle, fSourcePage, fSourcePage);
				return dialog;
			}
		};
	}

	protected int getInfoImplementationType() {
		return SourceInformationProvider.F_NO_IMP;
	}

	protected IInformationControlCreator getInformationControlCreator(final boolean cutDown) {
		return new IInformationControlCreator() {
			public IInformationControl createInformationControl(Shell parent) {
				return new DefaultInformationControl(parent, !cutDown);
			}
		};
	}

	public IHyperlinkDetector[] getHyperlinkDetectors(ISourceViewer sourceViewer) {
		IHyperlinkDetector[] registeredDetectors = super.getHyperlinkDetectors(sourceViewer);
		if (registeredDetectors == null)
			return null;

		if (fSourcePage == null)
			return registeredDetectors;

		IHyperlinkDetector additionalDetector = (IHyperlinkDetector) fSourcePage.getAdapter(IHyperlinkDetector.class);
		if (additionalDetector == null)
			return registeredDetectors;

		IHyperlinkDetector[] allDetectors = new IHyperlinkDetector[registeredDetectors.length + 1];
		System.arraycopy(registeredDetectors, 0, allDetectors, 0, registeredDetectors.length);
		allDetectors[registeredDetectors.length] = additionalDetector;
		return allDetectors;
	}

	public IQuickAssistAssistant getQuickAssistAssistant(ISourceViewer sourceViewer) {
		if (sourceViewer.isEditable()) {
			if (fQuickAssistant == null) {
				fQuickAssistant = new PDEQuickAssistAssistant();
				fQuickAssistant.setRestoreCompletionProposalSize(PDEPlugin.getDefault().getDialogSettingsSection("quick_assist_proposal_size")); //$NON-NLS-1$
			}
			return fQuickAssistant;
		}
		return null;
	}

	public abstract boolean affectsTextPresentation(PropertyChangeEvent event);

	public abstract boolean affectsColorPresentation(PropertyChangeEvent event);

	public abstract void adaptToPreferenceChange(PropertyChangeEvent event);

	public void dispose() {
		if (fQuickAssistant != null)
			fQuickAssistant.dispose();
	}

}
