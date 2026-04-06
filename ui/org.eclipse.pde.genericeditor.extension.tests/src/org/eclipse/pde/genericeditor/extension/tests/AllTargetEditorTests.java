/*******************************************************************************
 * Copyright (c) 2017, 2018 Red Hat Inc. and others
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Lucas Bullen (Red Hat Inc.)
 *******************************************************************************/
package org.eclipse.pde.genericeditor.extension.tests;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

@Suite
@SelectClasses({ AttributeNameCompletionTests.class, AttributeValueCompletionTests.class, TagNameCompletionTests.class,
	TagValueCompletionTests.class, Bug527084CompletionWithCommentsTest.class,
	Bug528706CompletionWithMultilineTagsTest.class, UpdateUnitVersionsCommandTests.class, Bug531602FormattingTests.class,
	TargetPlatformPresentationReconcilerTest.class })
public class AllTargetEditorTests {

}
