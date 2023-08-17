/*******************************************************************************
 * Copyright (c) 2021 Christoph Läubrich and others.
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
package org.eclipse.pde.internal.core.target;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.target.ITargetDefinition;
import org.eclipse.pde.core.target.ITargetHandle;

public class RemoteTargetHandle implements ITargetHandle {

	private enum RemoteState {
		UNKNWON, EXISTS, NOT_FOUND, FAILED;
	}

	private static final String FILE_SCHEMA = "file:"; //$NON-NLS-1$

	private static final Map<URI, RemoteTargetHandle> REMOTE_HANDLES = new ConcurrentHashMap<>();
	/**
	 * URI scheme for local targets
	 */
	static final String SCHEME = "remote"; //$NON-NLS-1$
	private final URI uri;
	private RemoteState state;

	private RemoteTargetHandle(URI uri) {
		this.uri = uri;
	}

	@Override
	public String getMemento() throws CoreException {
		return SCHEME + ":" + uri.toASCIIString(); //$NON-NLS-1$
	}

	@Override
	public boolean exists() {
		synchronized (this) {
			if (state == RemoteState.UNKNWON) {
				try {
					if ("file".equalsIgnoreCase(uri.getScheme())) { //$NON-NLS-1$
						File file = new File(uri);
						state = file.isFile() ? RemoteState.EXISTS : RemoteState.NOT_FOUND;
					} else {
						URLConnection connection = uri.toURL().openConnection();
						if (connection instanceof HttpURLConnection http) {
							try {
								http.setInstanceFollowRedirects(true);
								http.setRequestMethod("HEAD"); //$NON-NLS-1$
								int code = http.getResponseCode() / 100;
								state = code == 2 ? RemoteState.EXISTS : RemoteState.NOT_FOUND;
							} finally {
								http.disconnect();
							}
						} else {
							connection.getInputStream().close();
							state = RemoteState.EXISTS;
						}
					}
				} catch (IOException e) {
					state = RemoteState.FAILED;
				}
			}
			return state == RemoteState.EXISTS;
		}
	}

	@Override
	public ITargetDefinition getTargetDefinition() throws CoreException {
		TargetDefinition definition = new TargetDefinition(this);
		try (InputStream stream = uri.toURL().openStream()) {
			synchronized (this) {
				state = RemoteState.EXISTS;
			}
			definition.setContents(stream);
		} catch (MalformedURLException e) {
			throw new CoreException(
					Status.error(NLS.bind(Messages.RemoteTargetHandle_malformed_URL, uri, e.getMessage()), e));
		} catch (IOException e) {
			throw new CoreException(
					Status.error(NLS.bind(Messages.RemoteTargetHandle_ioproblem, uri, e.getMessage()), e));
		}
		return definition;
	}

	public static URI getEffectiveUri(String uri) throws CoreException, URISyntaxException {
		Objects.requireNonNull(uri);
		IStringVariableManager manager = VariablesPlugin.getDefault().getStringVariableManager();
		URI resolvedUri = new URI(convertRawToUri(manager.performStringSubstitution(uri)));
		return resolvedUri;
	}

	public static RemoteTargetHandle get(String uri) throws CoreException {
		Objects.requireNonNull(uri);
		try {
			URI resolvedUri = getEffectiveUri(uri);
			RemoteTargetHandle handle = REMOTE_HANDLES.computeIfAbsent(resolvedUri, RemoteTargetHandle::new);
			synchronized (handle) {
				if (handle.state != RemoteState.EXISTS) {
					// if it has failed or not found, reset the state here it
					// might has changed in the meantime ...
					handle.state = RemoteState.UNKNWON;
				}
			}
			return handle;
		} catch (URISyntaxException e) {
			throw new CoreException(Status.error(NLS.bind(Messages.RemoteTargetHandle_invalid_URI, e.getMessage()), e));
		}
	}

	static String convertRawToUri(String resolvePath) {
		// We need to convert windows path separators here...
		resolvePath = resolvePath.replace('\\', '/');
		String lc = resolvePath.toLowerCase();
		if (lc.startsWith(FILE_SCHEMA) && lc.charAt(FILE_SCHEMA.length()) != '/') {
			// according to rfc a file URI must always start with a slash
			resolvePath = FILE_SCHEMA + '/' + resolvePath.substring(FILE_SCHEMA.length());
		}
		return resolvePath;
	}

}
