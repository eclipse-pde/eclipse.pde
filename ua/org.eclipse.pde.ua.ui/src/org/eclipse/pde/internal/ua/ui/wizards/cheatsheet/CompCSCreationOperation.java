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
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.pde.internal.ua.core.cheatsheet.comp.CompCSWorkspaceModel;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.*;

/**
 * CompCSCreationOperation
 */
public class CompCSCreationOperation extends BaseCSCreationOperation implements IRunnableWithProgress {

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
		// Create Task Group
		// Element: taskGroup
		ICompCSTaskGroup taskGroup = createBasicGroup(compCS);
		// Create Task
		// Element: task
		ICompCSTask task = createBasicTask(taskGroup);
		// Configure Group
		taskGroup.addFieldTaskObject(task);
		// Configure Cheat Sheet
		// Attribute: name
		compCS.setFieldName(CSWizardMessages.CompCSCreationOperation_title);
		compCS.setFieldTaskObject(taskGroup);
	}

	/**
	 * @param parent
	 * @return
	 */
	public static ICompCSTask createBasicTask(ICompCSObject parent) {
		ICompCSModelFactory factory = parent.getModel().getFactory();
		// Create Task
		// Element: task
		ICompCSTask task = factory.createCompCSTask(parent);
		// Configure Task
		// Element: intro
		ICompCSIntro taskIntro = factory.createCompCSIntro(task);
		taskIntro.setFieldContent(formatTextBold(CSWizardMessages.CompCSCreationOperation_intro));
		// Element: onCompletion
		ICompCSOnCompletion taskConclusion = factory.createCompCSOnCompletion(task);
		taskConclusion.setFieldContent(formatTextBold(CSWizardMessages.CompCSCreationOperation_conclusion));
		// Attribute: name
		task.setFieldName(CSWizardMessages.CompCSCreationOperation_task);
		// Attribute: kind
		task.setFieldKind(ICompCSConstants.ATTRIBUTE_VALUE_CHEATSHEET);
		task.setFieldIntro(taskIntro);
		task.setFieldOnCompletion(taskConclusion);

		return task;
	}

	/**
	 * @param parent
	 * @return
	 */
	public static ICompCSTaskGroup createBasicGroup(ICompCSObject parent) {
		ICompCSModelFactory factory = parent.getModel().getFactory();
		// Create Task Group
		// Element: taskGroup
		ICompCSTaskGroup taskGroup = factory.createCompCSTaskGroup(parent);
		// Configure Task Group
		// Element: intro
		ICompCSIntro taskGroupIntro = factory.createCompCSIntro(taskGroup);
		taskGroupIntro.setFieldContent(formatTextBold(CSWizardMessages.CompCSCreationOperation_intro));
		// Element: onCompletion
		ICompCSOnCompletion taskGroupConclusion = factory.createCompCSOnCompletion(taskGroup);
		taskGroupConclusion.setFieldContent(formatTextBold(CSWizardMessages.CompCSCreationOperation_conclusion));
		// Attribute: name
		taskGroup.setFieldName(CSWizardMessages.CompCSCreationOperation_group);
		// Attribute: kind
		taskGroup.setFieldKind(ICompCSConstants.ATTRIBUTE_VALUE_SET);
		taskGroup.setFieldIntro(taskGroupIntro);
		taskGroup.setFieldOnCompletion(taskGroupConclusion);

		return taskGroup;
	}
}
