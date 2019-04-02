/********************************************************************************
 * Copyright (c) 2018, 2019 vogella GmbH and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Lars Vogel (vogella GmbH) - initial API and implementation
 *     Alexander Fedorov <alexander.fedorov@arsysop.ru> - Bug 534758, Bug 541067
 ********************************************************************************/
package org.eclipse.pde.internal.genericeditor.target.extension.codemining;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.codemining.ICodeMiningProvider;
import org.eclipse.jface.text.codemining.LineHeaderCodeMining;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetHandle;
import org.eclipse.pde.core.target.ITargetPlatformService;
import org.eclipse.pde.core.target.LoadTargetDefinitionJob;
import org.eclipse.pde.internal.core.PDECore;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.PDEUIMessages;
import org.eclipse.swt.events.MouseEvent;

@SuppressWarnings("restriction")
public class TargetDefinitionActivationCodeMining extends LineHeaderCodeMining {

	private final IPath path;
	private final String error;

	public TargetDefinitionActivationCodeMining(int beforeLineNumber, IDocument document, ICodeMiningProvider provider,
			String error) throws BadLocationException {
		super(beforeLineNumber, document, provider);
		ITextFileBufferManager bufferManager = FileBuffers.getTextFileBufferManager();
		String message = error;
		this.path = bufferManager.getTextFileBuffer(document).getLocation();
		if (path == null) {
			message = Messages.TargetDefinitionActivationCodeMining_e_location_outside_lfs;
		}
		this.error = message;
	}

	@Override
	protected CompletableFuture<Void> doResolve(ITextViewer viewer, IProgressMonitor monitor) {
		return CompletableFuture.runAsync(() -> {
			if (error != null) {
				setLabel(error);
				return;
			}
			boolean isCurrent = false;
			try {
				ITargetPlatformService service = acquireTargetPlatformService();
				String memento = service.getWorkspaceTargetHandle().getMemento();
				ITargetHandle targetHandle = getTargetHandle();
				String targetMemento = targetHandle.getMemento();
				isCurrent = Objects.equals(memento, targetMemento);
			} catch (CoreException e) {
				// Caught with default message shown
			}
			super.setLabel(isCurrent ? PDEUIMessages.AbstractTargetPage_reloadTarget
					: PDEUIMessages.AbstractTargetPage_setTarget);
		});
	}

	@Override
	public Consumer<MouseEvent> getAction() {
		return t -> activateTargetPlatform();
	}

	private void activateTargetPlatform() {
		if (error != null) {
			return;
		}
		try {
			ITargetHandle targetHandle = getTargetHandle();
			ITargetDefinition toLoad = targetHandle.getTargetDefinition();
			LoadTargetDefinitionJob.load(toLoad);
		} catch (CoreException e) {
			PDEPlugin.log(e.getStatus());
		}
	}

	private ITargetHandle getTargetHandle() {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		IFile file = root.getFile(path);
		if (!file.exists()) {
			file = root.getFileForLocation(path);
		}
		ITargetHandle handle;
		ITargetPlatformService service = acquireTargetPlatformService();
		if (file != null) {
			handle = service.getTarget(file);
		} else {
			handle = service.getTarget(URIUtil.toURI(path));
		}
		return handle;
	}

	private ITargetPlatformService acquireTargetPlatformService() {
		// to be replaced with injection at some moment
		return PDECore.getDefault().acquireService(ITargetPlatformService.class);
	}
}
