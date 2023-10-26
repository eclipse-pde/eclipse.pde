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
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;

import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.SemanticTokens;
import org.eclipse.lsp4j.SemanticTokensParams;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.TextDocumentService;

import aQute.bnd.help.Syntax;
import aQute.bnd.properties.Document;
import aQute.bnd.properties.IDocument;
import aQute.bnd.properties.LineType;
import aQute.bnd.properties.PropertiesLineReader;

public class BNDTextDocumentService implements TextDocumentService {

	private final Map<String, IDocument> docs = new ConcurrentHashMap<>();
	private Executor executor;

	BNDTextDocumentService(Executor executor) {
		this.executor = executor;
	}

	@Override
	public void didOpen(DidOpenTextDocumentParams params) {
		System.out.println("BNDTextDocumentService.didOpen()");
		IDocument model = new Document(params.getTextDocument().getText());
		this.docs.put(params.getTextDocument().getUri(), model);
	}

	@Override
	public void didChange(DidChangeTextDocumentParams params) {
		IDocument model = new Document(params.getContentChanges().get(0).getText());
		this.docs.put(params.getTextDocument().getUri(), model);
	}

	@Override
	public void didClose(DidCloseTextDocumentParams params) {
		System.out.println("BNDTextDocumentService.didClose()");
		this.docs.remove(params.getTextDocument().getUri());
	}

	@Override
	public void didSave(DidSaveTextDocumentParams params) {
		// TODO anything to do here?
		System.out.println("BNDTextDocumentService.didSave()");
	}

	@Override
	public CompletableFuture<SemanticTokens> semanticTokensFull(SemanticTokensParams params) {
		System.out.println("BNDTextDocumentService.semanticTokensFull()");
		return withDocument(params.getTextDocument(), document -> {
			SemanticTokens tokens = new SemanticTokens();
			PropertiesLineReader reader = new PropertiesLineReader(document);
			LineType type;
			while ((type = reader.next()) != LineType.eof) {
				// TODO how to use SemanticTokens ?!?
			}
			// TODO https://github.com/eclipse/lsp4e/issues/861
			return null;
		});
	}

	@Override
	public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams params) {
		System.out.println("BNDTextDocumentService.completion()");
		return withDocument(params.getTextDocument(), document -> {
			// TODO prefix
			return Either.forRight(new CompletionList(false, Syntax.HELP.values().stream().map(syntax -> {
				CompletionItem item = new CompletionItem();
				item.setLabel(syntax.getHeader());
				item.setInsertText(syntax.getHeader() + ": ");
				return item;
			}).toList()));
		});
	}

	private <T> CompletableFuture<T> withDocument(TextDocumentIdentifier identifier, DocumentCallable<T> callable) {
		IDocument document = docs.get(identifier.getUri());
		if (document == null) {
			return CompletableFuture.failedFuture(new IllegalStateException());
		}
		CompletableFuture<T> future = new CompletableFuture<>();
		future.completeAsync(() -> {
			try {
				return callable.call(document);
			} catch (Exception e) {
				future.completeExceptionally(e);
				return null;
			}

		}, executor);
		return future;
	}

	private static interface DocumentCallable<V> {
		V call(IDocument document) throws Exception;
	}

}
