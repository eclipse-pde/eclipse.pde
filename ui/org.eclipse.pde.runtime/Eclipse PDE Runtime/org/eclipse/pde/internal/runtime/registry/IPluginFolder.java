package org.eclipse.pde.internal.runtime.registry;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.*;
import java.util.*;


public interface IPluginFolder {
	public static final int F_EXTENSIONS = 1;
	public static final int F_EXTENSION_POINTS = 2;
	public static final int F_IMPORTS = 3;
	public static final int F_LIBRARIES = 4;
	public static final int F_FRAGMENTS = 5;
	public Object[] getChildren();
int getFolderId();
	public IPluginDescriptor getPluginDescriptor();
}
