package org.eclipse.pde.internal.core.isite;

import org.eclipse.pde.internal.core.ifeature.*;
import org.eclipse.pde.internal.core.ifeature.IVersionable;

/**
 * @author dejan
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public interface ISiteBuildFeature extends IVersionable, ISiteBuildObject {
	IFeature getReferencedFeature();
	void setReferencedFeature(IFeature feature);
	String getTargetURL();
}