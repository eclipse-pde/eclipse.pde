/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.core;

import java.io.PrintWriter;

/**
 * Classes that implement this interface can participate in saving the model to
 * the ASCII output stream using the provided writer.
 * 
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 * @since 2.0
 */
public interface IWritable {
	/**
	 * Writes the ASCII representation of the writable into the provider writer.
	 * The writable should use the provided indent to write the stream starting
	 * from the specified column number. Indent string should be written to the
	 * writer after every new line.
	 * 
	 * @param indent
	 *            a string that should be added after each new line to maintain
	 *            desired horizontal alignment
	 * @param writer
	 *            a writer to be used to write this object's textual
	 *            representation
	 */
	void write(String indent, PrintWriter writer);
}
