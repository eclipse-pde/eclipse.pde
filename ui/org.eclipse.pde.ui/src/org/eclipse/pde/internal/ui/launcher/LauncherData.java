package org.eclipse.pde.internal.ui.launcher;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.pde.core.plugin.IPluginModelBase;

/**
 * @version 	1.0
 * @author
 */
public class LauncherData {
	private IPath workspaceLocation;
	private boolean clearWorkspace;
	private IVMInstall vmInstall;
	private IPluginModelBase [] plugins;
	private String vmArguments;
	private String programArguments;
	private String applicationName;
	private boolean tracingEnabled;
	private boolean showSplash;

	public void setWorkspaceLocation(IPath workspaceLocation) {
		this.workspaceLocation = workspaceLocation;
	}

	public IPath getWorkspaceLocation() {
		return workspaceLocation;
	}
	/**
	 * Gets the vmInstall.
	 * @return Returns a IVMInstall
	 */
	public IVMInstall getVmInstall() {
		return vmInstall;
	}

	/**
	 * Sets the vmInstall.
	 * @param vmInstall The vmInstall to set
	 */
	public void setVmInstall(IVMInstall vmInstall) {
		this.vmInstall = vmInstall;
	}

	/**
	 * Gets the plugins.
	 * @return Returns a IPluginModelBase[]
	 */
	public IPluginModelBase[] getPlugins() {
		return plugins;
	}

	/**
	 * Sets the plugins.
	 * @param plugins The plugins to set
	 */
	public void setPlugins(IPluginModelBase[] plugins) {
		this.plugins = plugins;
	}

	/**
	 * Gets the vmArguments.
	 * @return Returns a String
	 */
	public String getVmArguments() {
		return vmArguments;
	}

	/**
	 * Sets the vmArguments.
	 * @param vmArguments The vmArguments to set
	 */
	public void setVmArguments(String vmArguments) {
		this.vmArguments = vmArguments;
	}

	/**
	 * Gets the programArguments.
	 * @return Returns a String
	 */
	public String getProgramArguments() {
		return programArguments;
	}

	/**
	 * Sets the programArguments.
	 * @param programArguments The programArguments to set
	 */
	public void setProgramArguments(String programArguments) {
		this.programArguments = programArguments;
	}

	/**
	 * Gets the applicationName.
	 * @return Returns a String
	 */
	public String getApplicationName() {
		return applicationName;
	}

	/**
	 * Sets the applicationName.
	 * @param applicationName The applicationName to set
	 */
	public void setApplicationName(String applicationName) {
		this.applicationName = applicationName;
	}

	/**
	 * Gets the clearWorkspace.
	 * @return Returns a boolean
	 */
	public boolean getClearWorkspace() {
		return clearWorkspace;
	}

	/**
	 * Sets the clearWorkspace.
	 * @param clearWorkspace The clearWorkspace to set
	 */
	public void setClearWorkspace(boolean clearWorkspace) {
		this.clearWorkspace = clearWorkspace;
	}
	
	public void setShowSplash(boolean showSplash) {
		this.showSplash = showSplash;
	}
	
	public boolean getShowSplash() {
		return showSplash;
	}

	/**
	 * Gets the tracingEnabled.
	 * @return Returns a boolean
	 */
	public boolean getTracingEnabled() {
		return tracingEnabled;
	}

	/**
	 * Sets the tracingEnabled.
	 * @param tracingEnabled The tracingEnabled to set
	 */
	public void setTracingEnabled(boolean tracingEnabled) {
		this.tracingEnabled = tracingEnabled;
	}

}
