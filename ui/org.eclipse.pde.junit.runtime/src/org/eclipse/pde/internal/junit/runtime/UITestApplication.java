/*******************************************************************************
 * Copyright (c) 2003, 2023 IBM Corporation and others.
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
 *     Carsten Reckord <eclipse@reckord.de> - bug 288343
 *******************************************************************************/
package org.eclipse.pde.internal.junit.runtime;

/**
 * A Workbench that runs a test suite specified in the
 * command line arguments.
 */
public class UITestApplication extends NonUIThreadTestApplication {

	public UITestApplication() {
		// Unlike in NonUIThreadTestApplication if the platform dependency is not available we will fail the launch
		this.runInUIThreadAndRequirePlatformUI = true;
		// In 3.0, the default is the "org.eclipse.ui.ide.worbench" application.
		this.defaultApplicationId = "org.eclipse.ui.ide.workbench"; //$NON-NLS-1$
	}

}
