package org.eclipse.pde.internal.core.isite;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.CoreException;
/**
 * The top-level model object of the Eclipse update site model.
 */
public interface ISiteBuild extends ISiteBuildObject {
	String P_PLUGIN_LOCATION = "plugin-location";
	String P_FEATURE_LOCATION = "feature-location";
	String P_SHOW_CONSOLE = "show-console";
	String P_AUTOBUILD = "autobuild";
	String P_SCRUB_OUTPUT = "scrub-output";

	void setPluginLocation(IPath location) throws CoreException;
	void setFeatureLocation(IPath location) throws CoreException;
	IPath getPluginLocation();
	IPath getFeatureLocation();
	boolean getScrubOutput();
	boolean isAutobuild();
	void setAutobuild(boolean value) throws CoreException;
	void setScrubOutput(boolean value) throws CoreException;
	boolean getShowConsole();
	void setShowConsole(boolean value) throws CoreException;
	
	void addFeatures(ISiteBuildFeature [] features) throws CoreException;
	void removeFeatures(ISiteBuildFeature [] features) throws CoreException;
	ISiteBuildFeature [] getFeatures();
	void resetReferences();
}