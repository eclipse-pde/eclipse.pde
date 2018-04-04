/*******************************************************************************
 * Copyright (c) 2018 Red Hat Inc. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Lucas Bullen (Red Hat Inc.) - initial implementation
 *******************************************************************************/
package org.eclipse.pde.genericeditor.extension.tests;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Scanner;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetPlatformService;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.core.target.TargetDefinitionPersistenceHelper;
import org.junit.Test;
import org.osgi.framework.FrameworkUtil;

public class Bug531602FormattingTests extends AbstractTargetEditorTest {

	@Test
	public void testSettingNullPersists() throws Exception {
		ITargetPlatformService service = PDECore.getDefault().acquireService(ITargetPlatformService.class);
		ITargetDefinition targetDefinition = service.newTarget();
		targetDefinition.setName("test");
		ByteArrayOutputStream expectedOutput = new ByteArrayOutputStream();
		TargetDefinitionPersistenceHelper.persistXML(targetDefinition, expectedOutput);

		ByteArrayOutputStream actualOutput = new ByteArrayOutputStream();
		targetDefinition.setProgramArguments(null);
		TargetDefinitionPersistenceHelper.persistXML(targetDefinition, actualOutput);
		assertEquals(expectedOutput.toString(StandardCharsets.UTF_8.toString()),
				actualOutput.toString(StandardCharsets.UTF_8.toString()));
	}

	@Test
	public void testIndenting() throws Exception {
		ITargetPlatformService service = PDECore.getDefault().acquireService(ITargetPlatformService.class);
		ITargetDefinition targetDefinition = service.newTarget();
		targetDefinition.setOS("test_os");
		ByteArrayOutputStream actualOutput = new ByteArrayOutputStream();
		TargetDefinitionPersistenceHelper.persistXML(targetDefinition, actualOutput);

		try (Scanner s = new Scanner(FrameworkUtil.getBundle(this.getClass())
				.getEntry("testing-files/target-files/IndentingTestCaseTarget.txt").openStream()).useDelimiter("\\A")) {
			String result = s.hasNext() ? s.next() : "";
			IEclipsePreferences preferences = InstanceScope.INSTANCE.getNode("org.eclipse.ui.editors");
			boolean spacesForTabs = preferences.getBoolean("spacesForTabs", false);

			if (spacesForTabs) {
				char[] chars = new char[preferences.getInt("tabWidth", 4)];
				Arrays.fill(chars, ' ');
				result.replace("\t", new String(chars));
			}

			assertEquals(result, actualOutput.toString(StandardCharsets.UTF_8.toString()));
		} catch (IOException e) {
		}
	}

	@Test
	public void testCommentsAndWhitespacePersists() throws Exception {
		InputStream inputStream = FrameworkUtil.getBundle(this.getClass())
				.getEntry("testing-files/target-files/PersistTestCaseTarget.txt").openStream();
		ITargetPlatformService service = PDECore.getDefault().acquireService(ITargetPlatformService.class);
		ITargetDefinition targetDefinition = service.newTarget();
		TargetDefinitionPersistenceHelper.initFromXML(targetDefinition, inputStream);

		ByteArrayOutputStream actualOutput = new ByteArrayOutputStream();
		TargetDefinitionPersistenceHelper.persistXML(targetDefinition, actualOutput);

		try (Scanner s = new Scanner(FrameworkUtil.getBundle(this.getClass())
				.getEntry("testing-files/target-files/PersistTestCaseTarget.txt").openStream()).useDelimiter("\\A")) {
			String result = s.hasNext() ? s.next() : "";
			assertEquals(result, actualOutput.toString(StandardCharsets.UTF_8.toString()));
		} catch (IOException e) {
		}
	}
}
