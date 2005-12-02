/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.core.target;

import java.io.PrintWriter;

import org.eclipse.pde.internal.core.itarget.ITarget;
import org.eclipse.pde.internal.core.itarget.ITargetModel;
import org.w3c.dom.Node;

public class Target extends TargetObject implements ITarget {

	private static final long serialVersionUID = 1L;
	
	public Target(ITargetModel model) {
		super(model);
	}

	public void reset() {
	}

	public void parse(Node node) {
	}

	public void write(String indent, PrintWriter writer) {
		writer.print(indent + "<target>"); //$NON-NLS-1$
		writer.print(indent + "</target>");
	}

}
