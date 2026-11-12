/*******************************************************************************
 * Copyright (c) 2026 vogella GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     vogella GmbH - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ui.tests.launcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstall2;
import org.eclipse.pde.internal.launching.launcher.LaunchArgumentsHelper;
import org.junit.jupiter.api.Test;

public class NativeAccessArgumentTest {

	private static final String FLAG = LaunchArgumentsHelper.ENABLE_NATIVE_ACCESS_VM_ARGUMENT;

	@Test
	public void addedOnJava24AndNewer() {
		assertEquals(List.of("-Xmx1g", FLAG), launchArguments("24", "-Xmx1g"));
		assertEquals(List.of(FLAG), launchArguments("25.0.1"));
	}

	@Test
	public void notAddedBeforeJava24() {
		assertEquals(List.of("-Xmx1g"), launchArguments("21.0.2", "-Xmx1g"));
		assertEquals(List.of(), launchArguments(null));
	}

	@Test
	public void existingNativeAccessConfigurationIsKept() {
		assertEquals(List.of("--enable-native-access=org.example"),
				launchArguments("25", "--enable-native-access=org.example"));
		assertEquals(List.of("--enable-native-access", "org.example"),
				launchArguments("25", "--enable-native-access", "org.example"));
		assertEquals(List.of("--illegal-native-access=deny"), launchArguments("25", "--illegal-native-access=deny"));
	}

	private static List<String> launchArguments(String javaVersion, String... userArguments) {
		IVMInstall vm = mock(IVMInstall.class, withSettings().extraInterfaces(IVMInstall2.class));
		when(((IVMInstall2) vm).getJavaVersion()).thenReturn(javaVersion);
		List<String> arguments = new ArrayList<>(List.of(userArguments));
		LaunchArgumentsHelper.addNativeAccessArgument(arguments, vm);
		return arguments;
	}
}
