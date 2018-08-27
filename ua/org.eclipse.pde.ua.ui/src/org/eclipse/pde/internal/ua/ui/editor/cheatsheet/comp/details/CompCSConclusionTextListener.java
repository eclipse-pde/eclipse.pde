/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ua.ui.editor.cheatsheet.comp.details;

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.pde.internal.core.util.PDETextHelper;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSModelFactory;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSOnCompletion;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSTaskObject;

public class CompCSConclusionTextListener implements IDocumentListener {

	private ICompCSTaskObject fDataTaskObject;

	private boolean fBlockEvents;

	public CompCSConclusionTextListener() {
		fDataTaskObject = null;
		fBlockEvents = false;
	}

	public void setBlockEvents(boolean block) {
		fBlockEvents = block;
	}

	public boolean getBlockEvents() {
		return fBlockEvents;
	}

	public void setData(ICompCSTaskObject object) {
		// Set data
		fDataTaskObject = object;
	}

	@Override
	public void documentAboutToBeChanged(DocumentEvent e) {
		// NO-OP
	}

	@Override
	public void documentChanged(DocumentEvent event) {
		// Check whether to handle this event
		if (fBlockEvents) {
			return;
		}
		// Ensure the task object is defined
		if (fDataTaskObject == null) {
			return;
		}
		// Get the text from the event
		IDocument document = event.getDocument();
		if (document == null) {
			return;
		}
		String text = document.get().trim();
		// Determine whether a conclusion was specified
		boolean hasText = PDETextHelper.isDefined(text);
		if (hasText) {
			// A conclusion was specified, update accordingly
			updateConclusionText(text);
		} else {
			// No conclusion was specified, remove any existing one
			removeConclusionText(text);
		}
	}

	private void updateConclusionText(String text) {
		if (fDataTaskObject.getFieldOnCompletion() == null) {
			// Create a new conclusion
			addConclusionText(text);
		} else {
			// Re-use the existing conclusion
			modifyConclusionText(text);
		}
	}

	private void addConclusionText(String text) {
		ICompCSModelFactory factory = fDataTaskObject.getModel().getFactory();
		ICompCSOnCompletion conclusion = factory.createCompCSOnCompletion(fDataTaskObject);
		conclusion.setFieldContent(text);
		fDataTaskObject.setFieldOnCompletion(conclusion);
	}

	private void modifyConclusionText(String text) {
		ICompCSOnCompletion conclusion = fDataTaskObject.getFieldOnCompletion();
		conclusion.setFieldContent(text);
	}

	private void removeConclusionText(String text) {
		ICompCSOnCompletion conclusion = fDataTaskObject.getFieldOnCompletion();
		if (conclusion != null) {
			fDataTaskObject.setFieldOnCompletion(null);
		}
	}

}
