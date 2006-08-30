/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
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
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.text.information.IInformationPresenter;
import org.eclipse.jface.text.information.IInformationProvider;
import org.eclipse.jface.text.information.InformationPresenter;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.reconciler.MonoReconciler;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.internal.core.text.IReconcilingParticipant;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.ISortableContentOutlinePage;
import org.eclipse.pde.internal.ui.editor.PDESourcePage;
import org.eclipse.pde.internal.ui.editor.contentassist.display.HTMLTextPresenter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;
import org.eclipse.ui.texteditor.ChainedPreferenceStore;

public abstract class ChangeAwareSourceViewerConfiguration extends TextSourceViewerConfiguration {

	protected PDESourcePage fSourcePage;
	protected IColorManager fColorManager;
	private MonoReconciler fReconciler;
	private InformationPresenter fInfoPresenter;

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
		this(page, manager, new ChainedPreferenceStore(new IPreferenceStore[] {
				PDEPlugin.getDefault().getPreferenceStore(),
				EditorsUI.getPreferenceStore() // general text editor store
				}));
	}
	
	public IReconciler getReconciler(ISourceViewer sourceViewer) {
		if (fSourcePage != null && fReconciler == null) {
			IBaseModel model = fSourcePage.getInputContext().getModel();
			if (model instanceof IReconcilingParticipant) {
				ReconcilingStrategy strategy = new ReconcilingStrategy();
				strategy.addParticipant((IReconcilingParticipant)model);
				ISortableContentOutlinePage outline = fSourcePage.getContentOutline();
				if (outline instanceof IReconcilingParticipant)
					strategy.addParticipant((IReconcilingParticipant)outline);
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
			for (int i= 0; i < contentTypes.length; i++)
				fInfoPresenter.setInformationProvider(provider, contentTypes[i]);
			
			fInfoPresenter.setSizeConstraints(60, 10, true, true);
		}
		return fInfoPresenter;
	}
	
	protected int getInfoImplementationType() {
		return SourceInformationProvider.F_NO_IMP;
	}
	
	protected IInformationControlCreator getInformationControlCreator(final boolean cutDown) {
		return new IInformationControlCreator() {
			public IInformationControl createInformationControl(Shell parent) {
				int style = cutDown ? SWT.RESIZE : (SWT.V_SCROLL | SWT.H_SCROLL | SWT.RESIZE);
				return new DefaultInformationControl(parent, style,	new HTMLTextPresenter(cutDown));
			}
		};
	}
	
 	public IHyperlinkDetector[] getHyperlinkDetectors(ISourceViewer sourceViewer) {
 		if (fSourcePage != null)
 			return new IHyperlinkDetector[] { (IHyperlinkDetector)fSourcePage.getAdapter(IHyperlinkDetector.class) };
		return super.getHyperlinkDetectors(sourceViewer);
	}

	public abstract boolean affectsTextPresentation(PropertyChangeEvent event);
 	
 	public abstract boolean affectsColorPresentation(PropertyChangeEvent event);
 	
	public abstract void adaptToPreferenceChange(PropertyChangeEvent event);
	
	public abstract void dispose();

}
