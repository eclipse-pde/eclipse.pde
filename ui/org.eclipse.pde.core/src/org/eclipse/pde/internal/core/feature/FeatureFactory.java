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
package org.eclipse.pde.internal.core.feature;

import org.eclipse.pde.internal.core.ifeature.*;

public class FeatureFactory implements IFeatureModelFactory {
	private IFeatureModel model;

public FeatureFactory(IFeatureModel model) {
	this.model = model;
}

public IFeaturePlugin createPlugin() {
	FeaturePlugin plugin = new FeaturePlugin();
	plugin.model = model;
	plugin.parent = model.getFeature();
	return plugin;
}

public IFeatureData createData() {
	FeatureData data = new FeatureData();
	data.model = model;
	data.parent = model.getFeature();
	return data;
}

public IFeatureChild createChild() {
	FeatureChild child = new FeatureChild();
	child.model = model;
	child.parent = model.getFeature();
	return child;
}

public IFeatureImport createImport() {
	FeatureImport iimport = new FeatureImport();
	iimport.model = model;
	iimport.parent = model.getFeature();
	return iimport;
}
public IFeatureURL createURL() {
	FeatureURL url = new FeatureURL();
	url.model = model;
	url.parent = model.getFeature();
	return url;
}

public IFeatureInstallHandler createInstallHandler() {
	FeatureInstallHandler handler = new FeatureInstallHandler();
	handler.model = model;
	handler.parent = model.getFeature();
	return handler;
}

public IFeatureInfo createInfo(int index) {
	FeatureInfo info = new FeatureInfo(index);
	info.model = model;
	info.parent = model.getFeature();
	return info;
}

public IFeatureURLElement createURLElement(IFeatureURL parent, int elementType) {
	FeatureURLElement element = new FeatureURLElement(elementType);
	element.model = model;
	element.parent = parent;
	return element;
}
}
