package org.eclipse.pde.internal.core.ifeature;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.IIdentifiable;
import org.eclipse.pde.core.plugin.IMatchRules;
/**
 * The reference to a plug-in that is part of this feature.
 */
public interface IFeatureChild extends IFeatureObject, IIdentifiable, IMatchRules {
	String P_VERSION = "version";
	String P_OPTIONAL = "optional";
	String P_NAME = "name";
	String P_MATCH = "match";
	String P_SEARCH_LOCATION = "search-location";
	
	int ROOT = 0;
	int SELF = 1;
	int BOTH = 2;
	
	String getVersion();
	void setVersion(String version) throws CoreException;
	boolean isOptional();
	void setOptional(boolean optional) throws CoreException;
	String getName();
	void setName(String name) throws CoreException;
	int getSearchLocation();
	void setSearchLocation(int location) throws CoreException;
	int getMatch();
	void setMatch(int match) throws CoreException;
}