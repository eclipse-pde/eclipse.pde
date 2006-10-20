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

package org.eclipse.pde.internal.ui.wizards.cheatsheet;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.pde.internal.core.cheatsheet.comp.CompCSWorkspaceModel;
import org.eclipse.pde.internal.core.icheatsheet.comp.ICompCS;
import org.eclipse.pde.internal.core.icheatsheet.comp.ICompCSConstants;
import org.eclipse.pde.internal.core.icheatsheet.comp.ICompCSIntro;
import org.eclipse.pde.internal.core.icheatsheet.comp.ICompCSModelFactory;
import org.eclipse.pde.internal.core.icheatsheet.comp.ICompCSOnCompletion;
import org.eclipse.pde.internal.core.icheatsheet.comp.ICompCSTask;
import org.eclipse.pde.internal.core.icheatsheet.comp.ICompCSTaskGroup;
import org.eclipse.pde.internal.ui.PDEUIMessages;

/**
 * CompCSCreationOperation
 *
 */
public class CompCSCreationOperation extends BaseCheatSheetCreationOperation
		implements IRunnableWithProgress {

	/**
	 * @param file
	 */
	public CompCSCreationOperation(IFile file) {
		super(file);
	}

	/**
	 * @param rule
	 */
	public CompCSCreationOperation(ISchedulingRule rule) {
		super(rule);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.pde.internal.ui.wizards.cheatsheet.BaseCheatSheetCreationOperation#createContent()
	 */
	protected void createContent() {
        CompCSWorkspaceModel model = new CompCSWorkspaceModel(fFile, false);
        initializeCS(model.getCompCS());
        model.save();
        model.dispose();
	}

	/**
	 * @param compCS
	 */
	private void initializeCS(ICompCS compCS) {
		
		ICompCSModelFactory factory = compCS.getModel().getFactory();
		
		// Create Task Group
		// Element: taskGroup
		ICompCSTaskGroup taskGroup = factory.createCompCSTaskGroup(compCS);

		// Create Task
		// Element: task
		ICompCSTask task = factory.createCompCSTask(taskGroup);

		// Configure Task
		// Element: intro
		ICompCSIntro taskIntro = factory.createCompCSIntro(task);
		taskIntro.setFieldContent(PDEUIMessages.CompCSCreationOperation_introduction);
		// Element: onCompletion
		ICompCSOnCompletion taskConclusion = 
			factory.createCompCSOnCompletion(task);
		taskConclusion.setFieldContent(PDEUIMessages.CompCSCreationOperation_conclusion);
		// Attribute: name
		task.setFieldName(PDEUIMessages.CompCSCreationOperation_task);
		// Attribute: kind
		task.setFieldKind(ICompCSConstants.ATTRIBUTE_VALUE_CHEATSHEET);
		task.setFieldIntro(taskIntro);
		task.setFieldOnCompletion(taskConclusion);
		
		// Configure Task Group
		// Element: intro
		ICompCSIntro taskGroupIntro = factory.createCompCSIntro(taskGroup);
		taskGroupIntro.setFieldContent(PDEUIMessages.CompCSCreationOperation_introduction);
		// Element: onCompletion
		ICompCSOnCompletion taskGroupConclusion = 
			factory.createCompCSOnCompletion(taskGroup);
		taskGroupConclusion.setFieldContent(PDEUIMessages.CompCSCreationOperation_conclusion);
		// Attribute: name
		taskGroup.setFieldName(PDEUIMessages.CompCSCreationOperation_group);
		// Attribute: kind
		taskGroup.setFieldKind(ICompCSConstants.ATTRIBUTE_VALUE_SET);
		taskGroup.setFieldIntro(taskGroupIntro);
		taskGroup.setFieldOnCompletion(taskGroupConclusion);
		taskGroup.addFieldTaskObject(task);

		// Configure Cheat Sheet
		// Attribute: name
		compCS.setFieldName(PDEUIMessages.CompCSCreationOperation_title);
		compCS.setFieldTaskObject(taskGroup);
		
	}

}
