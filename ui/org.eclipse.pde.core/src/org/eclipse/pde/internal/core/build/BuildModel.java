/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.build;

import java.io.*;
import java.util.*;

import org.eclipse.pde.core.*;
import org.eclipse.pde.core.build.*;
import org.eclipse.pde.internal.core.*;

public abstract class BuildModel extends AbstractModel implements IBuildModel {
	protected Build fBuild;

	private BuildModelFactory fFactory;

	public IBuild getBuild() {
		if (isLoaded() == false)
			load();
		return fBuild;
	}

	public IBuild getBuild(boolean createIfMissing) {
		if (fBuild == null && createIfMissing) {
			fBuild = new Build();
			fBuild.setModel(this);
			setLoaded(true);
		}
		return getBuild();
	}

	public IBuildModelFactory getFactory() {
		if (fFactory == null)
			fFactory = new BuildModelFactory(this);
		return fFactory;
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
		fBuild = new Build();
		fBuild.setModel(this);
		for (Enumeration names = properties.propertyNames(); names.hasMoreElements();) {
			String name = names.nextElement().toString();
			fBuild.processEntry(name, (String) properties.get(name));
		}
		setLoaded(true);
	}

	public void reload(InputStream source, boolean outOfSync) {
		if (fBuild != null)
			fBuild.reset();
		else {
			fBuild = new Build();
			fBuild.setModel(this);
		}
		load(source, outOfSync);
		fireModelChanged(new ModelChangedEvent(this,
				IModelChangedEvent.WORLD_CHANGED, new Object[0], null));
	}
}
