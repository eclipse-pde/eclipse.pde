/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.reconciler.IReconciler;
import org.eclipse.jface.text.reconciler.MonoReconciler;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.pde.core.IBaseModel;
import org.eclipse.pde.internal.core.text.IReconcilingParticipant;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.pde.internal.ui.editor.text.ColorManager;
import org.eclipse.pde.internal.ui.editor.text.IColorManager;
import org.eclipse.pde.internal.ui.editor.text.IPDEColorConstants;
import org.eclipse.pde.internal.ui.editor.text.ReconcilingStrategy;
import org.eclipse.pde.internal.ui.editor.text.XMLConfiguration;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.texteditor.DefaultRangeIndicator;

public abstract class XMLSourcePage extends PDESourcePage {
	
	class XMLSourceViewerConfiguration extends XMLConfiguration {
		private MonoReconciler fReconciler;

		public XMLSourceViewerConfiguration(IColorManager colorManager) {
			super(colorManager);
		}
		
		public IReconciler getReconciler(ISourceViewer sourceViewer) {
			if (fReconciler == null) {
				IBaseModel model = getInputContext().getModel();
				if (model instanceof IReconcilingParticipant) {
					ReconcilingStrategy strategy = new ReconcilingStrategy();
					strategy.addParticipant((IReconcilingParticipant)model);
					ISortableContentOutlinePage outline = getContentOutline();
					if (outline instanceof IReconcilingParticipant)
						strategy.addParticipant((IReconcilingParticipant)outline);
					fReconciler = new MonoReconciler(strategy, false);
					fReconciler.setDelay(500);
				}
			}
			return fReconciler;
		}
	}
	
	protected IColorManager fColorManager;

	public XMLSourcePage(PDEFormEditor editor, String id, String title) {
		super(editor, id, title);
		fColorManager = ColorManager.getDefault();
		setSourceViewerConfiguration(new XMLSourceViewerConfiguration(fColorManager));
		setRangeIndicator(new DefaultRangeIndicator());
	}
	
	public void dispose() {
		super.dispose();
		fColorManager.dispose();
	}
	
	public boolean canLeaveThePage() {
		boolean cleanModel = getInputContext().isModelCorrect();
		if (!cleanModel) {
			Display.getCurrent().beep();
			String title = getEditor().getSite().getRegisteredName();
			MessageDialog.openError(
				PDEPlugin.getActiveWorkbenchShell(),
				title,
				PDEUIMessages.SourcePage_errorMessage);
		}
		return cleanModel;
	}

	protected boolean affectsTextPresentation(PropertyChangeEvent event){
		String property = event.getProperty();
		return property.startsWith(IPDEColorConstants.P_DEFAULT) 
					|| property.startsWith(IPDEColorConstants.P_PROC_INSTR) 
					|| property.startsWith(IPDEColorConstants.P_STRING) 
					|| property.startsWith(IPDEColorConstants.P_TAG) 
					|| property.startsWith(IPDEColorConstants.P_XML_COMMENT);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.texteditor.AbstractDecoratedTextEditor#handlePreferenceStoreChanged(org.eclipse.jface.util.PropertyChangeEvent)
	 */
	protected void handlePreferenceStoreChanged(PropertyChangeEvent event) {
		XMLSourceViewerConfiguration sourceViewerConfiguration= (XMLSourceViewerConfiguration)getSourceViewerConfiguration();
		if (affectsTextPresentation(event)) {
			sourceViewerConfiguration.adaptToPreferenceChange(event);
			setSourceViewerConfiguration(sourceViewerConfiguration);
		}						
		super.handlePreferenceStoreChanged(event);
	}
	
	protected String[] collectContextMenuPreferencePages() {
		String[] ids= super.collectContextMenuPreferencePages();
		String[] more= new String[ids.length + 1];
		more[0]= "org.eclipse.pde.ui.EditorPreferencePage"; //$NON-NLS-1$
		System.arraycopy(ids, 0, more, 1, ids.length);
		return more;
	}

}
