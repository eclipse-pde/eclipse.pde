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

package org.eclipse.pde.internal.ui.wizards.templates;

import org.eclipse.pde.ui.templates.IFieldData;

	public class FieldData implements IFieldData {
		private boolean fragment;
		private String name;
		private String version;
		private String pluginId;
		private String pluginVersion;
		private int match;
		private String provider;
		private boolean doMain;
		private String className;
		private boolean thisCheck;
		private boolean bundleCheck;
		private boolean workspaceCheck;
		private boolean hasPreference;
		private boolean isUIPlugin;
		
		public boolean isFragment() {
			return fragment;
		}
		
		public String getName() {
			return name;
		}
		public String getVersion() {
			return version;
		}
		public String getProvider() {
			return provider;
		}
		public String getClassName() {
			return className;
		}
		/**
		 * @return Returns the bundleCheck.
		 */
		public boolean isBundleCheck() {
			return bundleCheck;
		}
		/**
		 * @return Returns true if generated plugin extends AbstractUIPlugin
		 */
		public boolean isUIPlugin(){
			return isUIPlugin;
		}
		/**
		 * @return Returns the preference page check
		 */
		public boolean hasPreference(){
			return hasPreference;
		}
		/**
		 * @param pref The preference page selection to set.
		 */
		public void setHasPreference(boolean pref){
			hasPreference = pref;
		}
		/**
		 * @param bundleCheck The bundleCheck to set.
		 */
		public void setBundleCheck(boolean bundleCheck) {
			this.bundleCheck = bundleCheck;
		}

		/**
		 * @return Returns the doMain.
		 */
		public boolean isDoMain() {
			return doMain;
		}

		/**
		 * @param doMain The doMain to set.
		 */
		public void setDoMain(boolean doMain) {
			this.doMain = doMain;
		}

		/**
		 * @return Returns the match.
		 */
		public int getMatch() {
			return match;
		}

		/**
		 * @param match The match to set.
		 */
		public void setMatch(int match) {
			this.match = match;
		}

		/**
		 * @return Returns the pluginId.
		 */
		public String getPluginId() {
			return pluginId;
		}

		/**
		 * @param pluginId The pluginId to set.
		 */
		public void setPluginId(String pluginId) {
			this.pluginId = pluginId;
		}

		/**
		 * @return Returns the pluginVersion.
		 */
		public String getPluginVersion() {
			return pluginVersion;
		}

		/**
		 * @param pluginVersion The pluginVersion to set.
		 */
		public void setPluginVersion(String pluginVersion) {
			this.pluginVersion = pluginVersion;
		}

		/**
		 * @return Returns the thisCheck.
		 */
		public boolean isThisCheck() {
			return thisCheck;
		}

		/**
		 * @param thisCheck The thisCheck to set.
		 */
		public void setThisCheck(boolean thisCheck) {
			this.thisCheck = thisCheck;
		}

		/**
		 * @return Returns the workspaceCheck.
		 */
		public boolean isWorkspaceCheck() {
			return workspaceCheck;
		}

		/**
		 * @param workspaceCheck The workspaceCheck to set.
		 */
		public void setWorkspaceCheck(boolean workspaceCheck) {
			this.workspaceCheck = workspaceCheck;
		}

		/**
		 * @param className The className to set.
		 */
		public void setClassName(String className) {
			this.className = className;
		}

		public void setIsUIPlugin(boolean UIPlugin){
			isUIPlugin = UIPlugin;
		}
		/**
		 * @param fragment The fragment to set.
		 */
		public void setFragment(boolean fragment) {
			this.fragment = fragment;
		}

		/**
		 * @param name The name to set.
		 */
		public void setName(String name) {
			this.name = name;
		}

		/**
		 * @param provider The provider to set.
		 */
		public void setProvider(String provider) {
			this.provider = provider;
		}

		/**
		 * @param version The version to set.
		 */
		public void setVersion(String version) {
			this.version = version;
		}

	}
