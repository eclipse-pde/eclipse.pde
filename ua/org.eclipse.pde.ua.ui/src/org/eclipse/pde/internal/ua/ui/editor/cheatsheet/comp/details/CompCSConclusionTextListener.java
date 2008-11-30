/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

/**
 * CompCSEnclosingTextModifyListener
 *
 */
public class CompCSConclusionTextListener implements IDocumentListener {

	private ICompCSTaskObject fDataTaskObject;

	private boolean fBlockEvents;

	/**
	 * 
	 */
	public CompCSConclusionTextListener() {
		fDataTaskObject = null;
		fBlockEvents = false;
	}

	/**
	 * @param block
	 */
	public void setBlockEvents(boolean block) {
		fBlockEvents = block;
	}

	/**
	 * @return
	 */
	public boolean getBlockEvents() {
		return fBlockEvents;
	}

	/**
	 * @param object
	 */
	public void setData(ICompCSTaskObject object) {
		// Set data
		fDataTaskObject = object;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.text.IDocumentListener#documentAboutToBeChanged(org.eclipse.jface.text.DocumentEvent)
	 */
	public void documentAboutToBeChanged(DocumentEvent e) {
		// NO-OP
	}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.events.ModifyListener#modifyText(org.eclipse.swt.events.ModifyEvent)
	 */
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

	/**
	 * @param text
	 */
	private void updateConclusionText(String text) {
		if (fDataTaskObject.getFieldOnCompletion() == null) {
			// Create a new conclusion
			addConclusionText(text);
		} else {
			// Re-use the existing conclusion
			modifyConclusionText(text);
		}
	}

	/**
	 * @param text
	 */
	private void addConclusionText(String text) {
		ICompCSModelFactory factory = fDataTaskObject.getModel().getFactory();
		ICompCSOnCompletion conclusion = factory.createCompCSOnCompletion(fDataTaskObject);
		conclusion.setFieldContent(text);
		fDataTaskObject.setFieldOnCompletion(conclusion);
	}

	/**
	 * @param text
	 */
	private void modifyConclusionText(String text) {
		ICompCSOnCompletion conclusion = fDataTaskObject.getFieldOnCompletion();
		conclusion.setFieldContent(text);
	}

	/**
	 * @param text
	 */
	private void removeConclusionText(String text) {
		ICompCSOnCompletion conclusion = fDataTaskObject.getFieldOnCompletion();
		if (conclusion != null) {
			fDataTaskObject.setFieldOnCompletion(null);
		}
	}

}
