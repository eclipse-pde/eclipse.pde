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
import java.util.*;
import java.util.jar.*;

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
		if (bundle!=null && bundle.getHeader(IBundle.KEY_HOST_BUNDLE)!=null)
			return true;
		return false;
	}

	public void load(InputStream source, boolean outOfSync) {
		Manifest manifest = new Manifest();
		try {
			manifest.read(source);
			if (!outOfSync)
				updateTimeStamp();
		} catch (IOException e) {
			String message = "Error while parsing bundle manifest in "+getInstallLocation();
			IStatus status = new Status(IStatus.ERROR, PDECore.PLUGIN_ID, IStatus.OK, message, e);    
			PDECore.log(new CoreException(status));
			return;
		}
		bundle = new Bundle();
		bundle.setModel(this);
		Attributes atts = manifest.getMainAttributes();
		Set keySet = atts.keySet();

		for (Iterator iter = keySet.iterator(); iter.hasNext();) {
			Object key = iter.next();
			Object value = atts.get(key);
			if (value!=null)
				bundle.processHeader(key.toString(), value.toString());
		}
		loaded = true;
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
}
