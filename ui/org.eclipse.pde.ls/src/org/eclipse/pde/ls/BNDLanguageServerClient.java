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
package org.eclipse.pde.ls;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.Future;

import org.eclipse.lsp4e.server.StreamConnectionProvider;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.pde.ls.bnd.BNDLanguageServer;

public class BNDLanguageServerClient implements StreamConnectionProvider {

	private PipedInputStream input;
	private PipedOutputStream output;
	private Future<Void> listening;

	@Override
	public InputStream getErrorStream() {
		return null;
	}

	@Override
	public InputStream getInputStream() {
		return input;
	}

	@Override
	public OutputStream getOutputStream() {
		return output;
	}

	@Override
	public void start() throws IOException {
		System.out.println("BNDLanguageServerClient.start()");
		BNDLanguageServer server = new BNDLanguageServer();
		PipedOutputStream pipedOutputStream = new PipedOutputStream();
		PipedInputStream pipedInputStream = new PipedInputStream();
		Launcher<LanguageClient> launcher = LSPLauncher.createServerLauncher(server, pipedInputStream,
				pipedOutputStream);
		input = new PipedInputStream(pipedOutputStream);
		output = new PipedOutputStream(pipedInputStream);
		listening = launcher.startListening();
	}

	@Override
	public void stop() {
		System.out.println("BNDLanguageServerClient.stop()");
		// TODO how to properly shutdown?!?
		listening.cancel(true);
		try {
			input.close();
		} catch (IOException e) {
		}
		try {
			output.close();
		} catch (IOException e) {
		}
	}

}
