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
import org.eclipse.pde.internal.core.cheatsheet.simple.SimpleCSWorkspaceModel;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCS;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSDescription;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSIntro;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSItem;
import org.eclipse.pde.internal.core.icheatsheet.simple.ISimpleCSModelFactory;

/**
 * SimpleCheatSheetCreationOperation
 *
 */
public class SimpleCheatSheetCreationOperation extends
		BaseCheatSheetCreationOperation {

	/**
	 * @param file
	 */
	public SimpleCheatSheetCreationOperation(IFile file) {
		super(file);
	}

	/**
	 * @param rule
	 */
	public SimpleCheatSheetCreationOperation(ISchedulingRule rule) {
		super(rule);
	}

	/**
	 * 
	 */
	protected void createContent() {
        SimpleCSWorkspaceModel model = new SimpleCSWorkspaceModel(fFile, false);
        initializeCS(model.getSimpleCS());
        // TODO: MP: Figure out how to prevent overwrite of file with same name
        // check if exists ? Do in wizard somehow ?
        model.save();
        model.dispose();
	}	

	/**
	 * @param simpleCS
	 */
	protected void initializeCS(ISimpleCS simpleCS) {
        // TODO: MP: Revisit and externalize
		simpleCS.setTitle("Cheat Sheet Title Text"); //$NON-NLS-1$
		ISimpleCSModelFactory factory = simpleCS.getModel().getFactory();
		ISimpleCSIntro intro = factory.createSimpleCSIntro();
		ISimpleCSDescription description = factory.createSimpleCSDescription();
        // TODO: MP: Revisit and externalize
		description.setContent("Cheat Sheet Introduction Text"); //$NON-NLS-1$
		intro.setDescription(description); 
		simpleCS.setIntro(intro);
		ISimpleCSItem item = factory.createSimpleCSItem();
        // TODO: MP: Revisit and externalize
		item.setTitle("Item Title Text"); //$NON-NLS-1$
		ISimpleCSDescription description2 = factory.createSimpleCSDescription();
        // TODO: MP: Revisit and externalize
		description2.setContent("Item Description"); //$NON-NLS-1$
		item.setDescription(description2); //$NON-NLS-1$
		simpleCS.addItem(item);
		
//		// TODO: MP: Optional stuff for testing now 
//		ISimpleCSOnCompletion onCompletion = factory.createSimpleCSOnCompletion();
//        // TODO: MP: Revisit and externalize
//		onCompletion.setContent("Description"); //$NON-NLS-1$
//		item.setOnCompletion(onCompletion);
//		ISimpleCSSubItem subitem = factory.createSimpleCSSubItem();
//        // TODO: MP: Revisit and externalize
//		subitem.setLabel("Label"); //$NON-NLS-1$
//		item.addSubItem(subitem);
//
//		ISimpleCSSubItem subitem2 = factory.createSimpleCSSubItem();
//        // TODO: MP: Revisit and externalize
//		subitem2.setLabel("Label"); //$NON-NLS-1$
//		item.addSubItem(subitem2);
//
//		
//		ISimpleCSCommand command = factory.createSimpleCSCommand();
//        // TODO: MP: Revisit and externalize
//		command.setSerialization("org.eclipse.ui.window.preferences(preferencePageId=org.eclipse.pde.ui.TargetPlatformPreferencePage)"); //$NON-NLS-1$
//		subitem.setExecutable(command);
	}		
}
