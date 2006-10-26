/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.editor.cheatsheet.comp.details;

import org.eclipse.pde.internal.core.icheatsheet.comp.ICompCSModelFactory;
import org.eclipse.pde.internal.core.icheatsheet.comp.ICompCSOnCompletion;
import org.eclipse.pde.internal.core.icheatsheet.comp.ICompCSTaskObject;
import org.eclipse.pde.internal.core.util.PDETextHelper;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Text;

/**
 * CompCSEnclosingTextModifyListener
 *
 */
public class CompCSConclusionTextListener implements ModifyListener {

	private ICompCSTaskObject fDataTaskObject;	
	
	/**
	 * 
	 */
	public CompCSConclusionTextListener(ICompCSTaskObject dataTaskObject) {
		fDataTaskObject = dataTaskObject;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.swt.events.ModifyListener#modifyText(org.eclipse.swt.events.ModifyEvent)
	 */
	public void modifyText(ModifyEvent event) {
		// Get the text from the event
		if ((event.widget == null) ||
				(event.widget instanceof Text) == false) {
			return;
		}
		String text = ((Text)event.widget).getText().trim();
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
		if (fDataTaskObject.getFieldIntro() == null) {
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
		ICompCSModelFactory factory = 
			fDataTaskObject.getModel().getFactory(); 
		ICompCSOnCompletion conclusion = 
			factory.createCompCSOnCompletion(fDataTaskObject); 
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
			fDataTaskObject.setFieldIntro(null);
		}
	}	
	
}
