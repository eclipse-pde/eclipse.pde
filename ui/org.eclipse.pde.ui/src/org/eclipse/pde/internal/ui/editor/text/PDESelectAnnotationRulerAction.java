/*******************************************************************************
 *  Copyright (c) 2006, 2016 IBM Corporation and others.
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
 *     Martin Karpisek <martin.karpisek@gmail.com> - Bug 507831
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.text;

import java.util.Iterator;
import java.util.ResourceBundle;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationAccessExtension;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.texteditor.AbstractMarkerAnnotationModel;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorExtension;
import org.eclipse.ui.texteditor.SelectMarkerRulerAction;

public class PDESelectAnnotationRulerAction extends SelectMarkerRulerAction {

	private boolean fIsEditable;
	private final ITextEditor fTextEditor;
	private Position fPosition;
	private final ResourceBundle fBundle;
	private final String fPrefix;

	public PDESelectAnnotationRulerAction(ResourceBundle bundle, String prefix, ITextEditor editor, IVerticalRulerInfo ruler) {
		super(bundle, prefix, editor, ruler);
		fTextEditor = editor;
		fBundle = bundle;
		fPrefix = prefix;
	}

	@Override
	public void run() {
		runWithEvent(null);
	}

	/*
	 * @see org.eclipse.jface.action.IAction#runWithEvent(org.eclipse.swt.widgets.Event)
	 * @since 3.2
	 */
	@Override
	public void runWithEvent(Event event) {
		if (fIsEditable) {
			ITextOperationTarget operation = fTextEditor.getAdapter(ITextOperationTarget.class);
			final int opCode = ISourceViewer.QUICK_ASSIST;
			if (operation != null && operation.canDoOperation(opCode)) {
				fTextEditor.selectAndReveal(fPosition.getOffset(), fPosition.getLength());
				operation.doOperation(opCode);
			}
			return;
		}

		super.run();
	}

	@Override
	public void update() {
		checkReadOnly();

		if (fIsEditable) {
			initialize(fBundle, fPrefix + "QuickFix."); //$NON-NLS-1$
		}

		super.update();
	}

	private void checkReadOnly() {
		fPosition = null;
		fIsEditable = false;

		AbstractMarkerAnnotationModel model = getAnnotationModel();
		IAnnotationAccessExtension annotationAccess = getAnnotationAccessExtension();

		IDocument document = getDocument();
		if (model == null)
			return;

		Iterator<Annotation> iter = model.getAnnotationIterator();
		int layer = Integer.MIN_VALUE;

		while (iter.hasNext()) {
			Annotation annotation = iter.next();
			if (annotation.isMarkedDeleted())
				continue;

			int annotationLayer = annotationAccess.getLayer(annotation);
			if (annotationLayer < layer)
				continue;

			Position position = model.getPosition(annotation);
			if (!includesRulerLine(position, document))
				continue;

			boolean isReadOnly = fTextEditor instanceof ITextEditorExtension && ((ITextEditorExtension) fTextEditor).isEditorInputReadOnly();
			if (!isReadOnly) {
				fPosition = position;
				fIsEditable = true;
				layer = annotationLayer;
				continue;
			}
		}
	}
}
