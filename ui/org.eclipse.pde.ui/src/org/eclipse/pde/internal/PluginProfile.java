/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
package org.eclipse.pde.internal;

public class PluginProfile {
private String name;

	public PluginProfile() {
		this(null);
	}

	public PluginProfile(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
}