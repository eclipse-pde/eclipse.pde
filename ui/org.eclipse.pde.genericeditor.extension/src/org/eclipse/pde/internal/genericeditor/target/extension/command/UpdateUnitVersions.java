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
package org.eclipse.pde.internal.genericeditor.target.extension.command;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import javax.xml.stream.XMLStreamException;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IPageChangeProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.internal.genericeditor.target.extension.model.ITargetConstants;
import org.eclipse.pde.internal.genericeditor.target.extension.model.LocationNode;
import org.eclipse.pde.internal.genericeditor.target.extension.model.Node;
import org.eclipse.pde.internal.genericeditor.target.extension.model.RepositoryCache;
import org.eclipse.pde.internal.genericeditor.target.extension.model.UnitNode;
import org.eclipse.pde.internal.genericeditor.target.extension.model.xml.Parser;
import org.eclipse.pde.internal.genericeditor.target.extension.p2.UpdateJob;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.osgi.framework.Version;

public class UpdateUnitVersions extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IDocument document = getDocument();

		// Logic placed in a CompletableFuture so that the progress monitors of the
		// cache update jobs are shown
		return CompletableFuture.supplyAsync(() -> {
			if (document == null) {
				return null;
			}

			Parser parser = Parser.getDefault();
			try {
				parser.parse(document);
			} catch (XMLStreamException e) {
				return null;
			}
			Node rootNode = parser.getRootNode();
			if (rootNode == null)
				return null;
			List<Node> locationsNode = rootNode.getChildNodesByTag(ITargetConstants.LOCATIONS_TAG);
			if (locationsNode == null || locationsNode.isEmpty())
				return null;

			int offsetChange = 0;
			String documentText = document.get();
			for (Node n1 : locationsNode.get(0).getChildNodesByTag(ITargetConstants.LOCATION_TAG)) {
				LocationNode locationNode = (LocationNode) n1;
				String repositoryLocation = locationNode.getRepositoryLocation();
				if (repositoryLocation == null) {
					continue;
				}
				RepositoryCache cache = RepositoryCache.getDefault();
				if (!cache.isUpToDate(repositoryLocation)) {
					try {
						updateCache(locationNode);
					} catch (InterruptedException e) {
						e.printStackTrace();
						continue;
					}
				}
				List<UnitNode> repositoryUnits = cache.fetchP2UnitsFromRepo(repositoryLocation, false);
				for (Node n2 : locationNode.getChildNodesByTag(ITargetConstants.UNIT_TAG)) {
					UnitNode unitNode = ((UnitNode) n2);
					List<String> versions = null;
					for (UnitNode unit : repositoryUnits) {
						if (unit.getId().equals(unitNode.getId())) {
							versions = unit.getAvailableVersions();
							break;
						}
					}
					if (versions == null || versions.isEmpty()) {
						continue;
					}
					Collections.sort(versions, (v1, v2) -> (new Version(v2)).compareTo(new Version(v1)));
					String version = versions.get(0);
					if (version == null || version.isEmpty() || unitNode.getVersion() == null
							|| version.equals(unitNode.getVersion())) {
						continue;
					}

					String nodeString = documentText.substring(unitNode.getOffsetStart() + offsetChange,
							unitNode.getOffsetEnd() + offsetChange);

					nodeString = nodeString.replaceFirst("version=\"" + unitNode.getVersion() + "\"",
							"version=\"" + version + "\"");
					documentText = documentText.substring(0, unitNode.getOffsetStart() + offsetChange) + nodeString
							+ documentText.substring(unitNode.getOffsetEnd() + offsetChange, documentText.length());

					offsetChange += version.length() - unitNode.getVersion().length();
				}
			}
			if (document.get().equals(documentText)) {
				Display.getDefault().asyncExec(() -> {
					MessageDialog.openInformation(null,
						"No Version Updates", "There are no version updates required for this document.");
				});
			} else {
				final String newText = documentText;
				Display.getDefault().asyncExec(() -> {
					document.set(newText);
				});
			}
			return documentText;
		});
	}

	private void updateCache(LocationNode locationNode) throws InterruptedException {
		Job job = new UpdateJob(locationNode);
		job.setUser(true);
		job.schedule();
		while (job.getResult() == null) {
			Thread.sleep(50);
		}
	}

	private IDocument getDocument() {
		IEditorPart editor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		IDocumentProvider provider = null;
		if (editor instanceof ITextEditor) {
			provider = ((ITextEditor) editor).getDocumentProvider();
		} else if (editor instanceof IPageChangeProvider) {
			Object selectedPage = ((IPageChangeProvider) editor).getSelectedPage();
			if (selectedPage instanceof ITextEditor) {
				provider = ((ITextEditor) selectedPage).getDocumentProvider();
			}
		}
		if(provider == null) {
			return null;
		}
		IEditorInput input = editor.getEditorInput();
		return provider.getDocument(input);
	}
}
