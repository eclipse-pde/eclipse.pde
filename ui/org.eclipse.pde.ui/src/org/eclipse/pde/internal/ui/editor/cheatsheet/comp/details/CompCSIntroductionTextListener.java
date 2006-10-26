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

import org.eclipse.pde.internal.core.icheatsheet.comp.ICompCSIntro;
import org.eclipse.pde.internal.core.icheatsheet.comp.ICompCSModelFactory;
import org.eclipse.pde.internal.core.icheatsheet.comp.ICompCSTaskObject;
import org.eclipse.pde.internal.core.util.PDETextHelper;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.widgets.Text;

/**
 * CompCSEnclosingTextModifyListener
 *
 */
public class CompCSIntroductionTextListener implements ModifyListener {

	private ICompCSTaskObject fDataTaskObject;	
	
	/**
	 * 
	 */
	public CompCSIntroductionTextListener(ICompCSTaskObject dataTaskObject) {
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
		ICompCSModelFactory factory = 
			fDataTaskObject.getModel().getFactory(); 
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
