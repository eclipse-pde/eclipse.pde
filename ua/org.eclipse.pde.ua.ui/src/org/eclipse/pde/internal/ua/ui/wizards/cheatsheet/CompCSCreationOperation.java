/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
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

package org.eclipse.pde.internal.ua.ui.wizards.cheatsheet;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.pde.internal.ua.core.cheatsheet.comp.CompCSWorkspaceModel;
import org.eclipse.pde.internal.ua.core.icheatsheet.comp.*;

public class CompCSCreationOperation extends BaseCSCreationOperation {

	public CompCSCreationOperation(IFile file) {
		super(file);
	}

	public CompCSCreationOperation(ISchedulingRule rule) {
		super(rule);
	}

	@Override
	protected void createContent() {
		CompCSWorkspaceModel model = new CompCSWorkspaceModel(fFile, false);
		initializeCS(model.getCompCS());
		model.save();
		model.dispose();
	}

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
