/*******************************************************************************
 * Copyright (c) 2007, 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ua.ui.editor.toc.actions;

import org.eclipse.pde.internal.ua.core.toc.text.TocTopic;
import org.eclipse.pde.internal.ui.util.PDELabelUtility;

public class TocAddTopicAction extends TocAddObjectAction {

	public TocAddTopicAction() {
		setText(TocActionMessages.TocAddTopicAction_topic);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#run()
	 */
	public void run() {
		if (fParentObject != null) { //Create a new topic object
			TocTopic topic = fParentObject.getModel().getFactory().createTocTopic();

			//Generate the name for the topic
			String name = PDELabelUtility.generateName(getChildNames(), TocActionMessages.TocAddTopicAction_topic);
			topic.setFieldLabel(name);

			//Add the new topic to the parent TOC object
			addChild(topic);
		}
	}
}
