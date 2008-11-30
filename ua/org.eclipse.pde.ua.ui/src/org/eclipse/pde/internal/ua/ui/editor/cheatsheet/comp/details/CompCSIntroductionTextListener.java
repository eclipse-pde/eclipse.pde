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
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSIntro;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSModelFactory;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.ICompCSTaskObject;

/**
 * CompCSEnclosingTextModifyListener
 */
public class CompCSIntroductionTextListener implements IDocumentListener {

	private ICompCSTaskObject fDataTaskObject;

	private boolean fBlockEvents;

	/**
	 * 
	 */
	public CompCSIntroductionTextListener() {
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
		// Determine whether an introduction was specified
		boolean hasText = PDETextHelper.isDefined(text);
		if (hasText) {
			// An introduction was specified, update accordingly
			updateIntroductionText(text);
		} else {
			// No introduction was specified, remove any existing one
			removeIntroductionText(text);
		}
	}

	/**
	 * @param text
	 */
	private void updateIntroductionText(String text) {
		if (fDataTaskObject.getFieldIntro() == null) {
			// Create a new introduction
			addIntroductionText(text);
		} else {
			// Re-use the existing introduction
			modifyIntroductionText(text);
		}
	}

	/**
	 * @param text
	 */
	private void addIntroductionText(String text) {
		ICompCSModelFactory factory = fDataTaskObject.getModel().getFactory();
		ICompCSIntro intro = factory.createCompCSIntro(fDataTaskObject);
		intro.setFieldContent(text);
		fDataTaskObject.setFieldIntro(intro);
	}

	/**
	 * @param text
	 */
	private void modifyIntroductionText(String text) {
		ICompCSIntro intro = fDataTaskObject.getFieldIntro();
		intro.setFieldContent(text);
	}

	/**
	 * @param text
	 */
	private void removeIntroductionText(String text) {
		ICompCSIntro intro = fDataTaskObject.getFieldIntro();
		if (intro != null) {
			fDataTaskObject.setFieldIntro(null);
		}
	}

}
