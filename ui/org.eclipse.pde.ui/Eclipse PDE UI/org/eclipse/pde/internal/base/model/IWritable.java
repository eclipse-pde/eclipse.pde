package org.eclipse.pde.internal.base.model;
/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.io.*;
/**
 * Classes that implement this interface can participate
 * in saving the model to the ASCII file using
 * the provided writer.
 */
public interface IWritable {
/**
 * Writes the ASCII representation of the writable
 * into the provider writer. The writable should
 * use the provided intent to write the content
 * starting from the specified column number.
 * Indent string should be written to
 * the writer after every new line.
 *
 * @param indent a string that should be added after each new line
 * to maintain desired horizontal alignment
 * @param writer a writer to be used to write
 * this object's textual representation
 */
void write(String indent, PrintWriter writer);
}
