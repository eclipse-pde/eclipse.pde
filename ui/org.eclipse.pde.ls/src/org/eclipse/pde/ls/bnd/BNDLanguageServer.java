/*******************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.ls.bnd;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.eclipse.lsp4j.CompletionOptions;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.SemanticTokenModifiers;
import org.eclipse.lsp4j.SemanticTokenTypes;
import org.eclipse.lsp4j.SemanticTokensLegend;
import org.eclipse.lsp4j.SemanticTokensWithRegistrationOptions;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.TextDocumentSyncKind;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;

//See 
// https://github.com/eclipse-lsp4j/lsp4j/blob/main/documentation/README.md#implement-your-language-server
// https://github.com/AlloyTools/org.alloytools.alloy/tree/master/org.alloytools.alloy.lsp
// https://github.com/bndtools/bnd/issues/5833
// https://www.vogella.com/tutorials/EclipseLanguageServer/article.html
// https://github.com/mickaelistria/eclipse-languageserver-demo/tree/master/Le%20LanguageServer%20de%20Chamrousse
public class BNDLanguageServer implements LanguageServer {

	private BNDTextDocumentService documentService;
	private BNDWorkspaceService workspaceService;
	private ExecutorService executorService;

	public BNDLanguageServer() {
		executorService = Executors.newWorkStealingPool();
		documentService = new BNDTextDocumentService(executorService);
		workspaceService = new BNDWorkspaceService();
	}

	@Override
	public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
		final InitializeResult res = new InitializeResult(new ServerCapabilities());
		res.getCapabilities().setCompletionProvider(new CompletionOptions(false, List.of()));
		res.getCapabilities().setTextDocumentSync(TextDocumentSyncKind.Full);
		res.getCapabilities().setSemanticTokensProvider(
				new SemanticTokensWithRegistrationOptions(
						new SemanticTokensLegend(List.of(SemanticTokenTypes.Property, SemanticTokenTypes.Comment),
								List.of(SemanticTokenModifiers.Readonly)),
						Boolean.TRUE));
		return CompletableFuture.completedFuture(res);
	}

	@Override
	public CompletableFuture<Object> shutdown() {
		return CompletableFuture.supplyAsync(() -> {
			executorService.shutdown();
			return Boolean.TRUE;
		});
	}

	@Override
	public void exit() {
		executorService.shutdownNow();
		try {
			executorService.awaitTermination(30, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	@Override
	public TextDocumentService getTextDocumentService() {
		return documentService;
	}

	@Override
	public WorkspaceService getWorkspaceService() {
		return workspaceService;
	}

}
