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
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

import org.eclipse.lsp4e.server.StreamConnectionProvider;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.pde.ls.bnd.BNDLanguageServer;

public class BNDLanguageServerClient implements StreamConnectionProvider {

	private InputStream input;
	private OutputStream output;
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
		LinkedBlockingQueue<Integer> inqueue = new LinkedBlockingQueue<>(10 * 1024 * 104);
		LinkedBlockingQueue<Integer> outqueue = new LinkedBlockingQueue<>(10 * 1024 * 104);
		Launcher<LanguageClient> launcher = LSPLauncher.createServerLauncher(server, new QueueInputStream(inqueue),
				new QueueOutputStream(outqueue));
		input = new QueueInputStream(outqueue);
		output = new QueueOutputStream(inqueue);
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

	private static final class QueueInputStream extends InputStream {

		private final BlockingQueue<Integer> byteSource;
		private boolean closed;

		public QueueInputStream(BlockingQueue<Integer> byteSource) {
			this.byteSource = byteSource;
		}

		@Override
		public int read() throws IOException {
			if (closed) {
				return -1;
			}
			try {
				Integer take = byteSource.take();
				if (take < 0) {
					closed = true;
				}
				return take;
			} catch (InterruptedException e) {
				throw new InterruptedIOException();
			}
		}

		@Override
		public void close() throws IOException {
			closed = true;
		}

	}

	private static final class QueueOutputStream extends OutputStream {

		private final BlockingQueue<Integer> byteSink;
		private boolean closed;

		public QueueOutputStream(BlockingQueue<Integer> byteSink) {
			this.byteSink = byteSink;
		}

		@Override
		public void write(int b) throws IOException {
			if (closed) {
				throw new IOException("closed");
			}
			try {
				byteSink.put(b);
			} catch (InterruptedException e) {
				throw new InterruptedIOException();
			}
		}

		@Override
		public void close() throws IOException {
			closed = true;
			try {
				byteSink.put(-1);
			} catch (InterruptedException e) {
				throw new InterruptedIOException();
			}
		}

	}

}
