/*******************************************************************************
 * Copyright (c) 2018 Red Hat Inc. and others
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.services.IServiceLocator;
import org.junit.Test;

public class UpdateUnitVersionsCommandTests extends AbstractTargetEditorTest {

	@Test
	public void testUpdateRequired() throws Exception {

		Map<String, String> expected = new HashMap<>();
		expected.put("org.eclipse.fake", "1.0.1");
		ITextViewer textViewer = getTextViewerForTarget("RequiresUnitVersionUpdateTarget");
		insertFirstUninsertedLocation(textViewer.getDocument(), getLocationForSite("SingleUnitSingleVersion"));
		confirmVersionUpdates(expected);
	}

	@Test
	public void testVersionSort() throws Exception {
		Map<String, String> expected = new HashMap<>();
		expected.put("org.eclipse.fake.1", "2.0.0"); // 2 vs 1
		expected.put("org.eclipse.fake.2", "1.2.0"); // 1.2 vs 1.1
		expected.put("org.eclipse.fake.3", "1.1.2"); // 1.1.2 vs 1.1.1
		expected.put("org.eclipse.fake.4", "1.1.1.v2018-01-02"); // 1.1.1.v2018-01-02 vs 1.1.1.v2018-01-01
		expected.put("org.eclipse.fake.5", "1.1.1.banana"); // 1.1.1.banana vs 1.1.1.apple
		expected.put("org.eclipse.fake.6", "1.10.0"); // 1.10 vs 1.9
		expected.put("org.eclipse.fake.7", "1.0.0.v2"); // 1.0.0.v2 vs 1.0.0
		ITextViewer textViewer = getTextViewerForTarget("TestReplaceWithNewestVersionTarget");
		insertFirstUninsertedLocation(textViewer.getDocument(), getLocationForSite("MultipleUnitsConfirmSorting"));
		confirmVersionUpdates(expected);
	}

	private ICommandService service = ((IServiceLocator) PlatformUI.getWorkbench()).getService(ICommandService.class);

	private Map<String, String> getVersionsForIdsFromTargetFile(String targetFile) {
		Map<String, String> units = new HashMap<>();
		String[] splitUnits = targetFile.split("<unit");
		for (int i = 1; i < splitUnits.length; i++) {
			int idIndex = splitUnits[i].indexOf("id=\"");
			int versionIndex = splitUnits[i].indexOf("version=\"");
			if (idIndex == -1 || versionIndex == -1) {
				continue;
			}
			idIndex += 4;
			versionIndex += 9;
			String id = splitUnits[i].substring(idIndex, splitUnits[i].indexOf("\"", idIndex));
			String version = splitUnits[i].substring(versionIndex, splitUnits[i].indexOf("\"", versionIndex));
			units.put(id, version);
		}
		return units;
	}

	private void confirmVersionUpdates(Map<String, String> expected) throws Exception {

		Command command = service.getCommand("org.eclipse.pde.updateUnitVersions");
		Object response = command.executeWithChecks(new ExecutionEvent());
		String updatedText = (String) ((CompletableFuture<?>) response).get();
		assertNotNull(updatedText);

		Map<String, String> actual = getVersionsForIdsFromTargetFile(updatedText);

		for (Entry<String, String> unit : expected.entrySet()) {
			String expectedID = unit.getKey();
			String expectedVersion = unit.getValue();
			assertTrue("ID: " + expectedID + " not found in actual", actual.containsKey(expectedID));
			assertEquals("ID: " + expectedID + " has the incorrect version.", expectedVersion, actual.get(expectedID));
		}
	}

	private void insertFirstUninsertedLocation(IDocument document, String Location) {
		String documentText = document.get();
		document.set(documentText.replaceFirst("REPO_LOCATION", Location));
	}
}
