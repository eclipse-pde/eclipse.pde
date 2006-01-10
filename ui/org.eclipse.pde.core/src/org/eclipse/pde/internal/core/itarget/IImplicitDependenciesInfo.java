package org.eclipse.pde.internal.core.itarget;

public interface IImplicitDependenciesInfo extends ITargetObject {
	
	public static final String P_IMPLICIT_PLUGINS = "implicit-plugins"; //$NON-NLS-1$
	
	ITargetPlugin[] getPlugins();
	
	public void addPlugin(ITargetPlugin plugin);
	
	public void addPlugins(ITargetPlugin[] plugins);
	
	public void removePlugin(ITargetPlugin plugin);
	
	public void removePlugins(ITargetPlugin[] plugins);
	
	public boolean containsPlugin(String id);

}
