package org.eclipse.pde.internal.builders;


import org.eclipse.pde.core.plugin.IPluginBase;

public class DependencyLoop {
	private IPluginBase [] members;
	private String name;

	public IPluginBase [] getMembers() {
		return members;
	}
	
	void setMembers(IPluginBase[] members) {
		this.members = members;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String toString() {
		return name;
	}
}