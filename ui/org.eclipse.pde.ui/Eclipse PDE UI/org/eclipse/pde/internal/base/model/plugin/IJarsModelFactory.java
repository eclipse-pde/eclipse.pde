package org.eclipse.pde.internal.base.model.plugin;

import org.eclipse.core.resources.IResource;
import org.w3c.dom.*;
/**
 * This model factory should be used to
 * create new instances of plugin.jars model
 * objects.
 */
public interface IJarsModelFactory {
/**
 * Creates a new Jar entry with
 * the provided name.
 * @return a new jar entry instance
 */
IJarEntry createEntry(String name);
}
