/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.osgi.bundle;

import java.io.*;

import org.eclipse.core.runtime.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.osgi.bundle.*;
import org.eclipse.pde.internal.core.*;

public abstract class BundleModel
	extends AbstractModel
	implements IBundleModel {
	protected Bundle bundle;

	public IBundle getBundle() {
		if (isLoaded() == false)
			load();
		return bundle;
	}

	public IBundle getBundle(boolean createIfMissing) {
		if (bundle == null && createIfMissing) {
			bundle = new Bundle();
			bundle.setModel(this);
			loaded = true;
		}
		return getBundle();
	}

	public String getInstallLocation() {
		return null;
	}

	public abstract void load();

	public boolean isFragmentModel() {
		IBundle bundle = getBundle();
		if (bundle != null
			&& bundle.getHeader(IBundle.KEY_FRAGMENT_HOST) != null)
			return true;
		return false;
	}

	public void load(InputStream source, boolean outOfSync) {
		BufferedReader reader;
		try {
			if (bundle==null) {
				bundle = new Bundle();
				bundle.setModel(this);
			}
			try {
				reader =
					new BufferedReader(new InputStreamReader(source, "UTF8"));
			} catch (UnsupportedEncodingException e) {
				reader = new BufferedReader(new InputStreamReader(source));
			}
			readManifest(reader);
			if (!outOfSync)
				updateTimeStamp();
			loaded = true;
		} catch (CoreException e) {
			PDECore.log(e);
		}
	}

	private void readManifest(BufferedReader reader) throws CoreException {
		String header = null;
		StringBuffer value = new StringBuffer(256);
		boolean firstLine = true;

		try {
			while (true) {
				String line = reader.readLine();

				if (line == null || line.length() == 0) {
					if (!firstLine) {
						bundle.processHeader(header, value.toString().trim());
					}
					break;
				}

				if (line.charAt(0) == ' ') {
					if (firstLine) {
						//TODO Need to NL message
						throwException(
							"Error in manifest at line " + line,
							null);
					}
					value.append(line.substring(1));
					continue;
				}

				if (!firstLine) {
					bundle.processHeader(header, value.toString().trim());
					value.setLength(0);
				}

				int colon = line.indexOf(':');
				if (colon == -1) {
					//TODO Need to NL message
					throwException("Error in manifest at line " + line, null);
				}
				header = line.substring(0, colon);
				value.append(line.substring(colon + 1));
				firstLine = false;
			}
		} catch (IOException e) {
			//TODO Need to NL message
			String message =
				"Error while parsing bundle manifest in "
					+ getInstallLocation();
			throwException(message, e);
		}
	}

	private void throwException(String message, Throwable e)
		throws CoreException {
		IStatus status =
			new Status(
				IStatus.ERROR,
				PDECore.PLUGIN_ID,
				IStatus.OK,
				message,
				e);
		throw new CoreException(status);
	}

	public void reload(InputStream source, boolean outOfSync) {
		if (bundle != null)
			bundle.reset();
		else {
			bundle = new Bundle();
			bundle.setModel(this);
		}
		load(source, outOfSync);
		fireModelChanged(
			new ModelChangedEvent(
				IModelChangedEvent.WORLD_CHANGED,
				new Object[0],
				null));
	}
	/* (non-Javadoc)
	 * @see org.eclipse.pde.core.IModel#isReconcilingModel()
	 */
	public boolean isReconcilingModel() {
		return false;
	}
}
