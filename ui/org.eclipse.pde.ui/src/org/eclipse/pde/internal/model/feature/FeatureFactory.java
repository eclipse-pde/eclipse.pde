package org.eclipse.pde.internal.model.feature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.net.URL;
import org.eclipse.pde.internal.base.model.feature.*;

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
