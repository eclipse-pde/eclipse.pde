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

package org.eclipse.pde.internal.ua.ui.wizards.cheatsheet;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.pde.internal.core.util.CoreUtility;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.*;
import org.eclipse.pde.internal.ua.core.cheatsheet.simple.text.SimpleCSModel;

/**
 * SimpleCheatSheetCreationOperation
 */
public class SimpleCSCreationOperation extends BaseCSCreationOperation {

	/**
	 * @param file
	 */
	public SimpleCSCreationOperation(IFile file) {
		super(file);
	}

	/**
	 * @param rule
	 */
	public SimpleCSCreationOperation(ISchedulingRule rule) {
		super(rule);
	}

	/**
	 * 
	 */
	protected void createContent() throws CoreException {
		SimpleCSModel model = new SimpleCSModel(CoreUtility.getTextDocument(fFile.getContents()), false);
		model.setUnderlyingResource(fFile);
		initializeCS(model.getSimpleCS());
		model.save();
		model.dispose();
	}

	/**
	 * @param simpleCS
	 */
	protected void initializeCS(ISimpleCS simpleCS) {
		ISimpleCSModelFactory factory = simpleCS.getModel().getFactory();

		// Element: intro
		ISimpleCSIntro intro = factory.createSimpleCSIntro(simpleCS);
		// Element: description
		ISimpleCSDescription description = factory.createSimpleCSDescription(intro);
		description.setContent(formatTextBold(CSWizardMessages.SimpleCSCreationOperation_body));
		intro.setDescription(description);

		// Element: item
		ISimpleCSItem item = factory.createSimpleCSItem(simpleCS);
		item.setTitle(CSWizardMessages.SimpleCSCreationOperation_item);
		// Element: description
		ISimpleCSDescription description2 = factory.createSimpleCSDescription(item);
		description2.setContent(formatTextBold(CSWizardMessages.SimpleCSCreationOperation_body));
		item.setDescription(description2);

		// Attribute: title
		simpleCS.setTitle(CSWizardMessages.SimpleCSCreationOperation_title);
		simpleCS.setIntro(intro);
		simpleCS.addItem(item);

	}
}
