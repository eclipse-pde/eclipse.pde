/*******************************************************************************
 *  Copyright (c) 2006, 2008 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.text;

import java.util.Iterator;
import java.util.ResourceBundle;
import org.eclipse.jface.text.*;
import org.eclipse.jface.text.source.*;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.texteditor.*;

public class PDESelectAnnotationRulerAction extends SelectMarkerRulerAction {

	private boolean fIsEditable;
	private ITextEditor fTextEditor;
	private Position fPosition;
	private ResourceBundle fBundle;
	private String fPrefix;

	public PDESelectAnnotationRulerAction(ResourceBundle bundle, String prefix, ITextEditor editor, IVerticalRulerInfo ruler) {
		super(bundle, prefix, editor, ruler);
		fTextEditor = editor;
		fBundle = bundle;
		fPrefix = prefix;
	}

	public void run() {
		runWithEvent(null);
	}

	/*
	 * @see org.eclipse.jface.action.IAction#runWithEvent(org.eclipse.swt.widgets.Event)
	 * @since 3.2
	 */
	public void runWithEvent(Event event) {
		if (fIsEditable) {
			ITextOperationTarget operation = (ITextOperationTarget) fTextEditor.getAdapter(ITextOperationTarget.class);
			final int opCode = ISourceViewer.QUICK_ASSIST;
			if (operation != null && operation.canDoOperation(opCode)) {
				fTextEditor.selectAndReveal(fPosition.getOffset(), fPosition.getLength());
				operation.doOperation(opCode);
			}
			return;
		}

		super.run();
	}

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

		Iterator iter = model.getAnnotationIterator();
		int layer = Integer.MIN_VALUE;

		while (iter.hasNext()) {
			Annotation annotation = (Annotation) iter.next();
			if (annotation.isMarkedDeleted())
				continue;

			int annotationLayer = annotationAccess.getLayer(annotation);
			if (annotationAccess != null)
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
