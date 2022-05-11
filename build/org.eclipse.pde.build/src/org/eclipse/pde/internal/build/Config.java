/*******************************************************************************
 *  Copyright (c) 2000, 2013 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 * 
 *  Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.build;

public class Config {

	public static String ANY = "*"; //$NON-NLS-1$
	private final String ws;
	private final String os;
	private final String arch;

	private static Config genericConfig; //singleton

	public Config(String os, String ws, String arch) {
		this.ws = ws;
		this.os = os;
		this.arch = arch;
	}

	public Config(String[] config) {
		this(config[0], config[1], config[2]);
	}

	public Config(String config) {
		this(Utils.getArrayFromStringWithBlank(config, ",")); //$NON-NLS-1$
	}

	public String getArch() {
		return arch;
	}

	public String getOs() {
		return os;
	}

	public String getWs() {
		return ws;
	}

	@Override
	public boolean equals(Object config) {
		if (this == config)
			return true;

		if (!(config instanceof Config))
			return false;

		Config aConfig = (Config) config;
		if (!os.equalsIgnoreCase(aConfig.os))
			return false;

		if (!ws.equalsIgnoreCase(aConfig.ws))
			return false;

		if (!arch.equalsIgnoreCase(aConfig.arch))
			return false;

		return true;
	}

	@Override
	public int hashCode() {
		return os.hashCode() + ws.hashCode() + arch.hashCode();
	}

	@Override
	public String toString() {
		return toString("_"); //$NON-NLS-1$
	}

	public String toString(String separator) {
		return os + separator + ws + separator + arch;
	}

	public String toStringReplacingAny(String separator, String value) {
		if (value == null) {
			return ""; //$NON-NLS-1$
		}

		String newOs = os;
		String newWs = ws;
		String newArch = arch;

		if (os == ANY)
			newOs = value;
		if (ws == ANY)
			newWs = value;
		if (arch == ANY)
			newArch = value;

		return newOs + separator + newWs + separator + newArch;
	}

	public String getPlatformFilter() {
		return "(& (osgi.ws=" + ws + ") (osgi.os=" + os + ") (osgi.arch=" + arch + "))"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	}

	public static Config genericConfig() {
		if (genericConfig == null)
			genericConfig = new Config(ANY, ANY, ANY);

		return genericConfig;
	}

}
