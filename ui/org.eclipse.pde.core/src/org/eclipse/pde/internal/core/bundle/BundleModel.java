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
package org.eclipse.pde.internal.core.bundle;

import java.io.*;
import java.util.*;

import org.eclipse.pde.core.*;
import org.eclipse.pde.core.bundle.*;
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

	public void load(InputStream source, boolean outOfSync) {
		Properties properties = new Properties();
		try {
			properties.load(source);
			if (!outOfSync)
				updateTimeStamp();
		} catch (IOException e) {
			PDECore.logException(e);
			return;
		}
		bundle = new Bundle();
		bundle.setModel(this);
		for (Enumeration names = properties.propertyNames();
			names.hasMoreElements();
			) {
			String name = names.nextElement().toString();
			bundle.processHeader(name, (String) properties.get(name));
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
