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
package org.eclipse.pde.internal.core.build;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Properties;

import org.eclipse.pde.core.IModelChangedEvent;
import org.eclipse.pde.core.ModelChangedEvent;
import org.eclipse.pde.core.build.IBuild;
import org.eclipse.pde.core.build.IBuildModel;
import org.eclipse.pde.core.build.IBuildModelFactory;
import org.eclipse.pde.internal.core.AbstractModel;
import org.eclipse.pde.internal.core.PDECore;

public abstract class BuildModel extends AbstractModel implements IBuildModel {
	protected Build build;
	private BuildModelFactory factory;
	public boolean fragment;

	public IBuild getBuild() {
		if (isLoaded() == false)
			load();
		return build;
	}

	public IBuild getBuild(boolean createIfMissing) {
		if (build == null && createIfMissing) {
			build = new Build();
			build.setModel(this);
			loaded = true;
		}
		return getBuild();
	}

	public IBuildModelFactory getFactory() {
		if (factory == null)
			factory = new BuildModelFactory(this);
		return factory;
	}
	public String getInstallLocation() {
		return null;
	}
	public boolean isFragment() {
		return fragment;
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
		build = new Build();
		build.setModel(this);
		for (Enumeration names = properties.propertyNames();
			names.hasMoreElements();
			) {
			String name = names.nextElement().toString();
			build.processEntry(name, (String) properties.get(name));
		}
		loaded = true;
	}
	public void reload(InputStream source, boolean outOfSync) {
		if (build != null)
			build.reset();
		else {
			build = new Build();
			build.setModel(this);
		}
		load(source, outOfSync);
		fireModelChanged(
			new ModelChangedEvent(
				IModelChangedEvent.WORLD_CHANGED,
				new Object[0],
				null));
	}
	public void setFragment(boolean value) {
		fragment = value;
	}
	public boolean isReconcilingModel() {
		return false;
	}
}
