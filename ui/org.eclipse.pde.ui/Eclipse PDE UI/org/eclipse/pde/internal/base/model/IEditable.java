package org.eclipse.pde.internal.base.model;

import java.io.*;
/**
 * Models that implement this interface indicate that
 * they can be changed. When a model is changed,
 * it becomes 'dirty'. This state can either be reset
 * (in case of a 'false allarm' or naturally set to
 * false as a result of saving the changes.
 * Models that implement this interface are expected
 * to be able to save in ASCII file format
 * (e.g. XML).
 */
public interface IEditable {
/**
 * Tests whether the model has been changed from the last clean
 * state.
 * @return true if the model has been changed and need saving
 */
public boolean isDirty();
/**
 * Saves the model into the provided writer.
 * The assumption is that the model can be
 * persisted in an ASCII file (for example, an XML file).
 * This method should clear the 'dirty' flag when
 * done.
 *
 * @param writer an object that should be used to
 * write ASCII representation of the model
 */
public void save(PrintWriter writer);
/**
 * Sets the dirty flag of the model. This method is
 * not intended to be used outside the model. Instead,
 * a dirty model should be saved to clear the flag.
 *
 * @param dirty a new value for the 'dirty' flag
 */
void setDirty(boolean dirty);
}
