/*******************************************************************************
 *  Copyright (c) 2000, 2008 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.feature;

import org.eclipse.pde.internal.core.ifeature.IFeatureChild;
import org.eclipse.pde.internal.core.ifeature.IFeatureData;
import org.eclipse.pde.internal.core.ifeature.IFeatureImport;
import org.eclipse.pde.internal.core.ifeature.IFeatureInfo;
import org.eclipse.pde.internal.core.ifeature.IFeatureInstallHandler;
import org.eclipse.pde.internal.core.ifeature.IFeatureModel;
import org.eclipse.pde.internal.core.ifeature.IFeatureModelFactory;
import org.eclipse.pde.internal.core.ifeature.IFeaturePlugin;
import org.eclipse.pde.internal.core.ifeature.IFeatureURL;
import org.eclipse.pde.internal.core.ifeature.IFeatureURLElement;

public class FeatureFactory implements IFeatureModelFactory {
	private final IFeatureModel model;

	public FeatureFactory(IFeatureModel model) {
		this.model = model;
	}

	@Override
	public IFeaturePlugin createPlugin() {
		FeaturePlugin plugin = new FeaturePlugin();
		plugin.model = model;
		plugin.parent = model.getFeature();
		return plugin;
	}

	@Override
	public IFeatureData createData() {
		FeatureData data = new FeatureData();
		data.model = model;
		data.parent = model.getFeature();
		return data;
	}

	@Override
	public IFeatureChild createChild() {
		FeatureChild child = new FeatureChild();
		child.model = model;
		child.parent = model.getFeature();
		return child;
	}

	@Override
	public IFeatureImport createImport() {
		FeatureImport iimport = new FeatureImport();
		iimport.model = model;
		iimport.parent = model.getFeature();
		return iimport;
	}

	@Override
	public IFeatureURL createURL() {
		FeatureURL url = new FeatureURL();
		url.model = model;
		url.parent = model.getFeature();
		return url;
	}

	@Override
	public IFeatureInstallHandler createInstallHandler() {
		FeatureInstallHandler handler = new FeatureInstallHandler();
		handler.model = model;
		handler.parent = model.getFeature();
		return handler;
	}

	@Override
	public IFeatureInfo createInfo(int index) {
		FeatureInfo info = new FeatureInfo(index);
		info.model = model;
		info.parent = model.getFeature();
		return info;
	}

	@Override
	public IFeatureURLElement createURLElement(IFeatureURL parent, int elementType) {
		FeatureURLElement element = new FeatureURLElement(elementType);
		element.model = model;
		element.parent = parent;
		return element;
	}
}
