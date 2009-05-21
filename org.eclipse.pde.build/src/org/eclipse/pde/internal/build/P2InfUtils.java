/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM - Initial API and implementation
 ******************************************************************************/
package org.eclipse.pde.internal.build;

import org.eclipse.osgi.service.resolver.VersionRange;
import org.osgi.framework.Version;

public class P2InfUtils {
	public static final int INSTRUCTION_INSTALL = 0;
	public static final int INSTRUCTION_UNINSTALL = 1;
	public static final int INSTRUCTION_CONFIGURE = 2;
	public static final int INSTRUCTION_UNCONFIGURE = 3;

	public static final String NAMESPACE_IU = "org.eclipse.equinox.p2.iu"; //$NON-NLS-1$
	public static final String NAMESPACE_OSGI = "osgi.bundle"; //$NON-NLS-1$
	public static final String NAMESPACE_TYPE = "org.eclipse.equinox.p2.eclipse.type"; //$NON-NLS-1$
	public static final String NAMESPACE_FLAVOR = "org.eclipse.equinox.p2.flavor"; //$NON-NLS-1$

	private static final VersionRange BUNDLE_RANGE = new VersionRange("[1.0.0, 2.0.0)"); //$NON-NLS-1$

	public static void printBundleCU(StringBuffer buffer, int i, String name, Version hostVersion, String filter, String[] instructions) {
		printBundleCU(buffer, i, name, hostVersion.toString(), hostVersion, filter, instructions);
	}

	public static void printBundleCU(StringBuffer buffer, int i, String name, String cuVersion, Version hostVersion, String filter, String[] instructions) {
		VersionRange hostRange = new VersionRange(hostVersion, true, hostVersion, true);
		//cuVersion may not be a proper OSGi version at this point (ie 1.0.0.$qualifier$)
		String cuRange = "[" + cuVersion + "," + cuVersion + "]"; //$NON-NLS-1$ //$NON-NLS-2$//$NON-NLS-3$
		String prefix = "units." + i + '.'; //$NON-NLS-1$

		//generate requirement to the new CU we are creating
		printRequires(buffer, null, i, NAMESPACE_IU, "@FLAVOR@" + name, cuRange, filter, true); //$NON-NLS-1$

		buffer.append(prefix + "id=@FLAVOR@" + name + '\n'); //$NON-NLS-1$ 
		buffer.append(prefix + "version=" + cuVersion + '\n'); //$NON-NLS-1$
		buffer.append(prefix + "properties.1.name=org.eclipse.pde.build.default\n"); //$NON-NLS-1$
		buffer.append(prefix + "properties.1.value=true\n"); //$NON-NLS-1$
		if (filter != null)
			buffer.append(prefix + "filter=" + filter + '\n'); //$NON-NLS-1$ 

		printProvides(buffer, prefix, 1, NAMESPACE_IU, "@FLAVOR@" + name, cuVersion); //$NON-NLS-1$
		printProvides(buffer, prefix, 2, NAMESPACE_FLAVOR, "@FLAVOR@", "1.0.0"); //$NON-NLS-1$ //$NON-NLS-2$

		printInstructions(buffer, prefix, instructions);

		printHostRequires(buffer, prefix, 1, NAMESPACE_OSGI, name, hostRange, false);
		printHostRequires(buffer, prefix, 2, NAMESPACE_TYPE, "bundle", BUNDLE_RANGE, false); //$NON-NLS-1$ 

		printRequires(buffer, prefix, 1, NAMESPACE_OSGI, name, hostRange, null, false);
		printRequires(buffer, prefix, 2, NAMESPACE_TYPE, "bundle", BUNDLE_RANGE, null, false); //$NON-NLS-1$
	}

	public static void printIU(StringBuffer buffer, int i, String name, Version version, String filter, String[] instructions) {
		printIU(buffer, i, name, version.toString(), filter, instructions);
	}

	public static void printIU(StringBuffer buffer, int i, String name, String version, String filter, String[] instructions) {
		printRequires(buffer, null, i, NAMESPACE_IU, "@FLAVOR@" + name, "[" + version + "," + version + "]", filter, true); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$

		String prefix = "units." + i + '.'; //$NON-NLS-1$

		buffer.append(prefix + "id=@FLAVOR@" + name + '\n'); //$NON-NLS-1$ 
		buffer.append(prefix + "version=" + version + '\n'); //$NON-NLS-1$
		buffer.append(prefix + "properties.1.name=org.eclipse.pde.build.default\n"); //$NON-NLS-1$
		buffer.append(prefix + "properties.1.value=true\n"); //$NON-NLS-1$
		if (filter != null)
			buffer.append(prefix + "filter=" + filter + '\n'); //$NON-NLS-1$ 

		printProvides(buffer, prefix, 1, NAMESPACE_IU, "@FLAVOR@" + name, version); //$NON-NLS-1$
		printProvides(buffer, prefix, 2, NAMESPACE_FLAVOR, "@FLAVOR@", "1.0.0"); //$NON-NLS-1$ //$NON-NLS-2$

		buffer.append(prefix + "touchpoint.id=org.eclipse.equinox.p2.osgi\n"); //$NON-NLS-1$
		buffer.append(prefix + "touchpoint.version=1.0.0\n"); //$NON-NLS-1$
		printInstructions(buffer, prefix, instructions);
	}

	private static void printInstructions(StringBuffer buffer, String prefix, String[] instructions) {
		if (instructions[INSTRUCTION_INSTALL] != null)
			buffer.append(prefix + "instructions.install=" + instructions[INSTRUCTION_INSTALL] + '\n'); //$NON-NLS-1$ 
		if (instructions[INSTRUCTION_UNINSTALL] != null)
			buffer.append(prefix + "instructions.uninstall=" + instructions[INSTRUCTION_UNINSTALL] + '\n'); //$NON-NLS-1$ 
		if (instructions[INSTRUCTION_UNCONFIGURE] != null)
			buffer.append(prefix + "instructions.unconfigure=" + instructions[INSTRUCTION_UNCONFIGURE] + '\n'); //$NON-NLS-1$ 
		if (instructions[INSTRUCTION_CONFIGURE] != null)
			buffer.append(prefix + "instructions.configure=" + instructions[INSTRUCTION_CONFIGURE] + '\n'); //$NON-NLS-1$ 		
	}

	public static void printRequires(StringBuffer buffer, String prefix, int i, String namespace, String name, VersionRange range, String filter, boolean greedy) {
		printRequires(buffer, prefix, i, namespace, name, range.toString(), filter, greedy);
	}

	public static void printRequires(StringBuffer buffer, String prefix, int i, String namespace, String name, String range, String filter, boolean greedy) {

		if (prefix == null)
			prefix = ""; //$NON-NLS-1$
		buffer.append(prefix + "requires." + i + ".namespace=" + namespace + "\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		buffer.append(prefix + "requires." + i + ".name=" + name + '\n'); //$NON-NLS-1$ //$NON-NLS-2$
		buffer.append(prefix + "requires." + i + ".range=" + range.toString() + '\n'); //$NON-NLS-1$ //$NON-NLS-2$
		buffer.append(prefix + "requires." + i + ".greedy=" + Boolean.toString(greedy) + '\n'); //$NON-NLS-1$ //$NON-NLS-2$
		if (filter != null)
			buffer.append(prefix + "requires." + i + ".filter=" + filter + '\n'); //$NON-NLS-1$ //$NON-NLS-2$
	}

	public static void printProvides(StringBuffer buffer, String prefix, int i, String namespace, String name, String version) {
		buffer.append(prefix + "provides." + i + ".namespace=" + namespace + '\n'); //$NON-NLS-1$ //$NON-NLS-2$ 
		buffer.append(prefix + "provides." + i + ".name=" + name + '\n'); //$NON-NLS-1$ //$NON-NLS-2$ 
		buffer.append(prefix + "provides." + i + ".version=" + version + '\n'); //$NON-NLS-1$ //$NON-NLS-2$ 
	}

	public static void printHostRequires(StringBuffer buffer, String prefix, int i, String namespace, String name, VersionRange range, boolean greedy) {
		if (prefix == null)
			prefix = ""; //$NON-NLS-1$
		buffer.append(prefix + "hostRequirements." + i + ".namespace=" + namespace + "\n"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		buffer.append(prefix + "hostRequirements." + i + ".name=" + name + '\n'); //$NON-NLS-1$ //$NON-NLS-2$
		buffer.append(prefix + "hostRequirements." + i + ".range=" + range.toString() + '\n'); //$NON-NLS-1$ //$NON-NLS-2$
		buffer.append(prefix + "hostRequirements." + i + ".greedy=" + Boolean.toString(greedy) + '\n'); //$NON-NLS-1$ //$NON-NLS-2$ 
	}

}
