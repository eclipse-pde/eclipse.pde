/*******************************************************************************
 * Copyright (c) 2008, 2018 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ua.tests.cheatsheet;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({ SimpleCSIntroTestCase.class, SimpleCSItemTestCase.class, SimpleCSSubItemTestCase.class,
		SimpleCSItemAPITestCase.class, SimpleCSSubItemAPITestCase.class })
public class AllCheatSheetModelTests {
}
