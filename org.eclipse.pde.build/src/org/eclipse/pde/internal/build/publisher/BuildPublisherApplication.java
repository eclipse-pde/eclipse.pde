/*******************************************************************************
 *  Copyright (c) 2008, 2009 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *      IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.build.publisher;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.equinox.p2.publisher.AbstractPublisherApplication;
import org.eclipse.equinox.p2.publisher.IPublisherAction;

public class BuildPublisherApplication extends AbstractPublisherApplication {
	private List actions;

	public void addAction(IPublisherAction action) {
		if (actions == null)
			actions = new ArrayList(1);
		actions.add(action);
	}

	protected IPublisherAction[] createActions() {
		if (actions == null)
			return new IPublisherAction[0];
		return (IPublisherAction[]) actions.toArray(new IPublisherAction[actions.size()]);
	}

	public void setAppend(boolean value) {
		super.append = value;
	}

}
