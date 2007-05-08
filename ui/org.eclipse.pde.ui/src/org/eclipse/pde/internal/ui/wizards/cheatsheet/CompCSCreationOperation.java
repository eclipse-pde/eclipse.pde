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
import org.eclipse.pde.internal.core.icheatsheet.comp.ICompCSObject;
import org.eclipse.pde.internal.core.icheatsheet.comp.ICompCSOnCompletion;
import org.eclipse.pde.internal.core.icheatsheet.comp.ICompCSTask;
import org.eclipse.pde.internal.core.icheatsheet.comp.ICompCSTaskGroup;
import org.eclipse.pde.internal.ui.PDEUIMessages;

/**
 * CompCSCreationOperation
 *
 */
public class CompCSCreationOperation extends BaseCSCreationOperation
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
		compCS.setFieldName(PDEUIMessages.CompCSCreationOperation_title);
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
		taskIntro.setFieldContent(
				formatTextBold(PDEUIMessages.CompCSCreationOperation_introduction));
		// Element: onCompletion
		ICompCSOnCompletion taskConclusion = 
			factory.createCompCSOnCompletion(task);
		taskConclusion.setFieldContent(
				formatTextBold(PDEUIMessages.CompCSCreationOperation_conclusion));
		// Attribute: name
		task.setFieldName(PDEUIMessages.CompCSCreationOperation_task);
		// Attribute: kind
		task.setFieldKind(ICompCSConstants.ATTRIBUTE_VALUE_CHEATSHEET);
		task.setFieldIntro(taskIntro);
		task.setFieldOnCompletion(taskConclusion);		
		
		return task;
	}
	
	/**
	 * @param text
	 * @return
	 */
	private static String formatTextBold(String text) {
		// TODO: MP: CompCS:  Create generalized HTML formatter utility
		StringBuffer buffer = new StringBuffer();
		buffer.append("<b>"); //$NON-NLS-1$
		buffer.append(text);
		buffer.append("</b>"); //$NON-NLS-1$
		return buffer.toString();
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
		taskGroupIntro.setFieldContent(
				formatTextBold(PDEUIMessages.CompCSCreationOperation_introduction));
		// Element: onCompletion
		ICompCSOnCompletion taskGroupConclusion = 
			factory.createCompCSOnCompletion(taskGroup);
		taskGroupConclusion.setFieldContent(
				formatTextBold(PDEUIMessages.CompCSCreationOperation_conclusion));
		// Attribute: name
		taskGroup.setFieldName(PDEUIMessages.CompCSCreationOperation_group);
		// Attribute: kind
		taskGroup.setFieldKind(ICompCSConstants.ATTRIBUTE_VALUE_SET);
		taskGroup.setFieldIntro(taskGroupIntro);
		taskGroup.setFieldOnCompletion(taskGroupConclusion);	
		
		return taskGroup;
	}
}
