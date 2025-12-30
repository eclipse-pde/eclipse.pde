/*******************************************************************************
 * Copyright (c) 2018, 2024 Red Hat Inc. and others
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

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javax.xml.stream.XMLStreamException;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.equinox.p2.metadata.IVersionedId;
import org.eclipse.jface.dialogs.IPageChangeProvider;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.IDocument;
import org.eclipse.pde.internal.genericeditor.target.extension.model.ITargetConstants;
import org.eclipse.pde.internal.genericeditor.target.extension.model.LocationNode;
import org.eclipse.pde.internal.genericeditor.target.extension.model.Node;
import org.eclipse.pde.internal.genericeditor.target.extension.model.RepositoryCache;
import org.eclipse.pde.internal.genericeditor.target.extension.model.UnitNode;
import org.eclipse.pde.internal.genericeditor.target.extension.model.xml.Parser;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

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

			Node rootNode = null;
			Parser parser = Parser.getDefault();
			try {
				rootNode = parser.parse(document);
			} catch (XMLStreamException e) {
			}
			if (rootNode == null) {
				return null;
			}
			List<Node> locationsNode = rootNode.getChildNodesByTag(ITargetConstants.LOCATIONS_TAG);
			if (locationsNode == null || locationsNode.isEmpty()) {
				return null;
			}

			int offsetChange = 0;
			String documentText = document.get();

			List<LocationNode> locationNodes = locationsNode.get(0).getChildNodesByTag(ITargetConstants.LOCATION_TAG)
					.stream().map(LocationNode.class::cast).toList();

			// Fetch all repos at once to fetch pending metadata in parallel
			locationNodes.stream().map(LocationNode::getRepositoryLocations).flatMap(List::stream)
					.forEach(RepositoryCache::prefetchP2MetadataOfRepository);

			for (LocationNode locationNode : locationNodes) {
				List<String> repositoryLocations = locationNode.getRepositoryLocations();
				if (repositoryLocations.isEmpty()) {
					continue;
				}
				Map<String, List<IVersionedId>> repositoryUnits = RepositoryCache
						.fetchP2UnitsFromRepos(repositoryLocations);
				for (Node n2 : locationNode.getChildNodesByTag(ITargetConstants.UNIT_TAG)) {
					UnitNode unitNode = ((UnitNode) n2);
					String declaredVersion = unitNode.getVersion();

					@SuppressWarnings("restriction")
					// if not ok, probably a version range
					boolean isValidExplicitVersion = org.eclipse.pde.internal.core.util.VersionUtil
							.validateVersion(declaredVersion).isOK();
					if (declaredVersion == null || !isValidExplicitVersion) {
						continue;
					}
					List<IVersionedId> versions = repositoryUnits.get(unitNode.getId());
					if (versions == null || versions.isEmpty()) {
						continue;
					}
					String version = versions.get(0).getVersion().toString();
					if (version.isEmpty() || version.equals(declaredVersion)) {
						continue;
					}

					String nodeString = documentText.substring(unitNode.getOffsetStart() + offsetChange,
							unitNode.getOffsetEnd() + offsetChange);

					nodeString = nodeString.replaceFirst("version=\"" + declaredVersion + "\"",
							"version=\"" + version + "\"");
					documentText = documentText.substring(0, unitNode.getOffsetStart() + offsetChange) + nodeString
							+ documentText.substring(unitNode.getOffsetEnd() + offsetChange, documentText.length());

					offsetChange += version.length() - declaredVersion.length();
				}
			}
			if (document.get().equals(documentText)) {
				Display.getDefault().asyncExec(() -> MessageDialog.openInformation(null, "No Version Updates",
						"There are no version updates required for this document."));
			} else {
				final String newText = documentText;
				Display.getDefault().asyncExec(() -> {
					document.set(newText);
				});
			}
			return documentText;
		});
	}

	private IDocument getDocument() {
		IEditorPart editor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		IDocumentProvider provider = null;
		if (editor instanceof ITextEditor textEditor) {
			provider = textEditor.getDocumentProvider();
		} else if (editor instanceof IPageChangeProvider pageChangeProvider) {
			if (pageChangeProvider.getSelectedPage() instanceof ITextEditor textEditor) {
				provider = textEditor.getDocumentProvider();
			}
		}
		if(provider == null) {
			return null;
		}
		IEditorInput input = editor.getEditorInput();
		return provider.getDocument(input);
	}
}
