/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
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
import java.util.jar.*;

import org.eclipse.pde.core.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.ibundle.*;
import org.osgi.framework.*;

public abstract class BundleModel
	extends AbstractModel
	implements IBundleModel {

	private Bundle fBundle;
	
	public BundleModel() {
		fBundle = new Bundle();
		fBundle.setModel(this);
	}

	public IBundle getBundle() {
		if (!isLoaded())
			load();
		return fBundle;
	}

	public String getInstallLocation() {
		return null;
	}

	public abstract void load();

	public boolean isFragmentModel() {
		return fBundle.getHeader(Constants.FRAGMENT_HOST) != null;
	}

	public void load(InputStream source, boolean outOfSync) {
		try {
			Manifest m = new Manifest(source);
			fBundle.load(manifestToProperties(m.getMainAttributes()));
			if (!outOfSync)
				updateTimeStamp();
		} catch (IOException e) {
		} finally {
		}
	}
	
	private Properties manifestToProperties(Attributes d) {
		Iterator iter = d.keySet().iterator();
		Properties result = new Properties();
		while (iter.hasNext()) {
			Attributes.Name key = (Attributes.Name) iter.next();
			result.put(key.toString(), d.get(key));
		}
		return result;
	}


	public void reload(InputStream source, boolean outOfSync) {
		load(source, outOfSync);
		fireModelChanged(
			new ModelChangedEvent(this,
				IModelChangedEvent.WORLD_CHANGED,
				new Object[0],
				null));
	}
}
