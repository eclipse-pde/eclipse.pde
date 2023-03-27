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
package org.eclipse.pde.internal.core.bnd;

import aQute.bnd.osgi.Jar;
import aQute.bnd.osgi.Resource;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.function.Predicate;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;

/**
 * Wraps an {@link IFile} to a BND {@link Resource} that can be used to perform
 * analyzer operations.
 *
 * @since 3.17
 */
public class FileResource implements Resource {

	private IFile file;

	private String extra;

	public FileResource(IFile file) {
		this.file = file;
	}

	@Override
	public long lastModified() {
		return file.getLocalTimeStamp();
	}

	@Override
	public InputStream openInputStream() throws Exception {
		return file.getContents(true);
	}

	@Override
	public void write(OutputStream out) throws Exception {
		try (InputStream stream = openInputStream()) {
			stream.transferTo(out);
		}
	}

	@Override
	public void close() throws IOException {
	}

	@Override
	public void setExtra(String extra) {
		this.extra = extra;
	}

	@Override
	public String getExtra() {
		return extra;
	}

	@Override
	public long size() throws Exception {
		URI location = file.getLocationURI();
		if (location != null) {
			IFileStore store = EFS.getStore(location);
			if (store != null) {
				IFileInfo fetchInfo = store.fetchInfo();
				if (fetchInfo.exists()) {
					return fetchInfo.getLength();
				}
			}
		}
		return -1;
	}

	@Override
	public synchronized ByteBuffer buffer() throws Exception {

		try (InputStream stream = openInputStream()) {
			return ByteBuffer.wrap(stream.readAllBytes());
		}
	}

	public static void addResources(Jar jar, IContainer container, Predicate<IResource> filter) throws CoreException {
		addResources(jar, container, container.getProjectRelativePath().toString(), filter);
	}

	private static void addResources(Jar jar, IContainer container, String prefix, Predicate<IResource> filter)
			throws CoreException {
		if (container == null || !container.exists()) {
			return;
		}
		for (IResource resource : container.members()) {
			if (filter == null || filter.test(container)) {
				if (resource instanceof IFile) {
					IPath projectRelativePath = resource.getProjectRelativePath();
					String relativePath = projectRelativePath.toString();
					String base = relativePath.substring(prefix.length());
					if (base.startsWith("/")) { //$NON-NLS-1$
						base = base.substring(1);
					}
					jar.putResource(base, new FileResource((IFile) resource));
				}
				if (resource instanceof IContainer) {
					addResources(jar, (IContainer) resource, prefix, filter);
				}
			}
		}
	}

}
