/*******************************************************************************
 * Copyright (c) 2015 vogella GmbH.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Simon Scholz <simon.scholz@vogella.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.spy.preferences.handler;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.pde.spy.preferences.parts.PreferenceSpyPart;

public class ExpandAllHandler {

	@Execute
	public void execute(MPart part) {
		Object partImpl = part.getObject();
		if (partImpl instanceof PreferenceSpyPart) {
			((PreferenceSpyPart) partImpl).getViewer().expandAll();
		}
	}

}