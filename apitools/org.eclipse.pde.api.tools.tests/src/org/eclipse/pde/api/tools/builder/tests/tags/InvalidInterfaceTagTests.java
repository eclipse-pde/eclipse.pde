/*******************************************************************************
 * Copyright (c) 2008, 2018 IBM Corporation and others.
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
package org.eclipse.pde.api.tools.builder.tests.tags;

import org.eclipse.core.runtime.IPath;
import org.eclipse.pde.api.tools.internal.builder.BuilderMessages;
import org.eclipse.pde.api.tools.internal.problems.ApiProblemFactory;
import org.eclipse.pde.api.tools.internal.provisional.descriptors.IElementDescriptor;
import org.eclipse.pde.api.tools.internal.provisional.problems.IApiProblem;

import junit.framework.Test;

/**
 * Tests that unsupported Javadoc tags on interfaces are reported properly
 *
 * @since 1.0
 */
public class InvalidInterfaceTagTests extends TagTest {

	public InvalidInterfaceTagTests(String name) {
		super(name);
	}

	/**
	 * @return the tests for this class
	 */
	public static Test suite() {
		return buildTestSuite(InvalidInterfaceTagTests.class);
	}

	@Override
	protected IPath getTestSourcePath() {
		return super.getTestSourcePath().append("interface"); //$NON-NLS-1$
	}

	@Override
	protected int getDefaultProblemId() {
		return ApiProblemFactory.createProblemId(
				IApiProblem.CATEGORY_USAGE,
				IElementDescriptor.TYPE,
				IApiProblem.UNSUPPORTED_TAG_USE,
				IApiProblem.NO_FLAGS);
	}

	public void testInvalidInterfaceTag1I() {
		x1(true);
	}

	public void testInvalidInterfaceTag1F() {
		x1(false);
	}

	/**
	 * Tests having an @noreference tag on a variety of inner / outer / top-level interfaces in package a.b.c
	 */
	private void x1(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(1));
		setExpectedMessageArgs(new String[][] {
				{"@noreference", BuilderMessages.TagValidator_an_interface_that_is_not_visible}, //$NON-NLS-1$
		});
		deployTagTest("test1.java", inc, false); //$NON-NLS-1$
	}

	public void testInvalidInterfaceTag2I() {
		x2(true);
	}

	public void testInvalidInterfaceTag2F() {
		x2(false);
	}

	/**
	 * Tests having an @noreference tag on an interface in the default package
	 */
	private void x2(boolean inc) {
		/*setExpectedProblemIds(getDefaultProblemSet(1));
		setExpectedMessageArgs(new String[][] {
				{"@noreference", BuilderMessages.TagValidator_an_interface}
		});*/
		deployTagTest("test2.java", inc, true); //$NON-NLS-1$
	}

	public void testInvalidInterfaceTag3I() {
		x3(true);
	}

	public void testInvalidInterfaceTag3F() {
		x3(false);
	}

	/**
	 * Tests having an @nooverride tag on a variety of inner / outer / top-level interfaces in package a.b.c
	 */
	private void x3(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(4));
		setExpectedMessageArgs(new String[][] {
				{"@nooverride", BuilderMessages.TagValidator_an_interface}, //$NON-NLS-1$
				{"@nooverride", BuilderMessages.TagValidator_an_interface}, //$NON-NLS-1$
				{"@nooverride", BuilderMessages.TagValidator_an_interface}, //$NON-NLS-1$
				{"@nooverride", BuilderMessages.TagValidator_an_interface} //$NON-NLS-1$
		});
		deployTagTest("test3.java", inc, false); //$NON-NLS-1$
	}

	public void testInvalidInterfaceTag4I() {
		x4(true);
	}

	public void testInvalidInterfaceTag4F() {
		x4(false);
	}

	/**
	 * Tests having an @nooverride tag on an interface in the default package
	 */
	private void x4(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(1));
		setExpectedMessageArgs(new String[][] {
				{"@nooverride", BuilderMessages.TagValidator_an_interface}	 //$NON-NLS-1$
		});
		deployTagTest("test4.java", inc, true); //$NON-NLS-1$
	}

	public void testInvalidInterfaceTag5I() {
		x5(true);
	}

	public void testInvalidInterfaceTag5F() {
		x5(false);
	}

	/**
	 * Tests having an @noinstantiate tag on a variety of inner / outer / top-level interfaces in package a.b.c
	 */
	private void x5(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(4));
		setExpectedMessageArgs(new String[][] {
				{"@noinstantiate", BuilderMessages.TagValidator_an_interface}, //$NON-NLS-1$
				{"@noinstantiate", BuilderMessages.TagValidator_an_interface}, //$NON-NLS-1$
				{"@noinstantiate", BuilderMessages.TagValidator_an_interface}, //$NON-NLS-1$
				{"@noinstantiate", BuilderMessages.TagValidator_an_interface} //$NON-NLS-1$
		});
		deployTagTest("test5.java", inc, false); //$NON-NLS-1$
	}

	public void testInvalidInterfaceTag6I() {
		x6(true);
	}

	public void testInvalidInterfaceTag6F() {
		x6(false);
	}

	/**
	 * Tests having an @noinstantiate tag on an interface in the default package
	 */
	private void x6(boolean inc) {
		setExpectedProblemIds(getDefaultProblemSet(1));
		setExpectedMessageArgs(new String[][] {
				{"@noinstantiate", BuilderMessages.TagValidator_an_interface}	 //$NON-NLS-1$
		});
		deployTagTest("test6.java", inc, true); //$NON-NLS-1$
	}
}
