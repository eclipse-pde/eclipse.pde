/*******************************************************************************
 * Copyright (c) 2017, 2018 Red Hat Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Lucas Bullen (Red Hat Inc.)
 *******************************************************************************/
package org.eclipse.pde.genericeditor.extension.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ AttributeNameCompletionTests.class, AttributeValueCompletionTests.class, TagNameCompletionTests.class,
	TagValueCompletionTests.class, Bug527084CompletionWithCommentsTest.class,
	Bug528706CompletionWithMultilineTagsTest.class, UpdateUnitVersionsCommandTests.class, Bug531602FormattingTests.class })
public class AllTargetEditorTests {

}
