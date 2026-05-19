/*******************************************************************************
 * Copyright (c) 2025 Dieter Mai and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Dieter Mai - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.util;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubMonitor;

/**
 * Deletes content of a directory recursively.
 */
public class DeleteContentWalker implements FileVisitor<Path> {

	/**
	 * Deletes the given root directory and its content. If the deletion fails
	 * or is canceled by the user, the method returns silently.
	 *
	 * @param root
	 *            The root directory to delete.
	 * @param monitor
	 *            The monitor to report progress to. Can be null.
	 */
	public static void deleteDirectory(Path root, IProgressMonitor monitor) {
		// if there is no file, no need to proceed
		if (root == null || !Files.exists(root)) {
			return;
		}

		IProgressMonitor submonitor = createSubMonitor(root, monitor);

		try {
			Files.walkFileTree(root, new DeleteContentWalker(root, submonitor));
		} catch (IOException e) {
			// noting to do
		} finally {
			submonitor.done();
		}
	}

	private static IProgressMonitor createSubMonitor(Path root, IProgressMonitor monitor) {
		IProgressMonitor submonitor;
		if (monitor == null || monitor instanceof NullProgressMonitor) {
			submonitor = SubMonitor.convert(monitor);
		} else {
			try (var stream = Files.list(root)) {
				// only use the root content for progress. anything else would
				// be overkill.
				long count = stream.count();
				submonitor = SubMonitor.convert(monitor, (int) count);
			} catch (IOException e) {
				// In case of error, just ignore the monitor;
				submonitor = SubMonitor.convert(monitor);
			}
		}
		return submonitor;
	}

	private final Path root;
	private final IProgressMonitor monitor;

	private DeleteContentWalker(Path root, IProgressMonitor monitor) {
		this.root = root;
		this.monitor = monitor;
	}

	@Override
	public FileVisitResult preVisitDirectory(Path path, BasicFileAttributes attrs) throws IOException {
		if (Platform.OS.isWindows()) {
			Files.setAttribute(path, "dos:readonly", false); //$NON-NLS-1$
		}
		return resultIfNotCanceled(FileVisitResult.CONTINUE);
	}

	@Override
	public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
		if (Platform.OS.isWindows()) {
			Files.setAttribute(path, "dos:readonly", false); //$NON-NLS-1$
		}
		Files.deleteIfExists(path);

		if (Objects.equals(path.getParent(), root)) {
			monitor.worked(1);
		}
		return resultIfNotCanceled(FileVisitResult.CONTINUE);
	}

	@Override
	public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
		if (exc != null) {
			throw exc;
		}
		return resultIfNotCanceled(FileVisitResult.CONTINUE);
	}

	@Override
	public FileVisitResult postVisitDirectory(Path path, IOException exc) throws IOException {
		Files.deleteIfExists(path);

		if (Objects.equals(path.getParent(), root)) {
			monitor.worked(1);
		}
		return resultIfNotCanceled(FileVisitResult.CONTINUE);
	}

	/**
	 * Returns the given result if not canceled. If canceled
	 * {@link FileVisitResult#TERMINATE} is returned.
	 */
	private FileVisitResult resultIfNotCanceled(FileVisitResult result) {
		if (monitor.isCanceled()) {
			return FileVisitResult.TERMINATE;
		}
		return result;
	}
}