/*******************************************************************************
 *  Copyright (c) 2000, 2012 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.build;

import java.util.Vector;
import org.eclipse.pde.core.build.IBuildEntry;

public class BuildUtil {

	public static IBuildEntry[] getBuildLibraries(IBuildEntry[] entries) {
		Vector<IBuildEntry> temp = new Vector<>();
		for (IBuildEntry entry : entries) {
			if (entry.getName().startsWith(IBuildEntry.JAR_PREFIX))
				temp.add(entry);
		}
		IBuildEntry[] result = new IBuildEntry[temp.size()];
		temp.copyInto(result);
		return result;
	}

}
