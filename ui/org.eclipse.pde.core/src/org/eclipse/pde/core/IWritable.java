package org.eclipse.pde.core;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.PrintWriter;
/**
 * Classes that implement this interface can participate
 * in saving the model to the ASCII output stream using
 * the provided writer.
 * <p>
 * <b>Note:</b> This field is part of an interim API that is still under development and expected to
 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 * </p>
 */
public interface IWritable {
	/**
	 * Writes the ASCII representation of the writable
	 * into the provider writer. The writable should
	 * use the provided indent to write the stream
	 * starting from the specified column number.
	 * Indent string should be written to
	 * the writer after every new line.
	 *
	 * @param indent a string that should be added after each new line
	 * to maintain desired horizontal alignment
	 * @param writer a writer to be used to write
	 * this object's textual representation
	 * <p>
	 * <b>Note:</b> This field is part of an interim API that is still under development and expected to
	 * change significantly before reaching stability. It is being made available at this early stage to solicit feedback
	 * from pioneering adopters on the understanding that any code that uses this API will almost certainly be broken
	 * (repeatedly) as the API evolves.
	 * </p>
	 */
	void write(String indent, PrintWriter writer);
}