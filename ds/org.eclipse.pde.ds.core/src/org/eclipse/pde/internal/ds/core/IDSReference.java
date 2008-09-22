/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Rafael Oliveira Nóbrega <rafael.oliveira@gmail.com> - bug 223738
 *******************************************************************************/
package org.eclipse.pde.internal.ds.core;

/**
 * Represents a dependency that a component has on a set of target services.
 * 
 * A component configuration is not satisfied, unless all its references are
 * satisfied. A reference specifies target services by specifying their
 * interface and an optional target filter.
 * 
 * @since 3.4
 * @see IDSComponent
 * @see IDSObject
 */
public interface IDSReference extends IDSObject {

	/**
	 * Sets the name of the reference.
	 * 
	 * This name is local to the component and can be used to locate a bound
	 * service of this reference with one of the locateService methods of
	 * ComponentContext.
	 * 
	 * @param name
	 *            new name of the reference
	 */
	public void setReferenceName(String name);

	/**
	 * Returns the name of the reference.
	 * 
	 * @return String containing the name of the reference
	 */
	public String getReferenceName();

	/**
	 * Sets the fully qualified name of the class that is used by the component
	 * to access the service.
	 * 
	 * The service provided to the component must be type compatible with this
	 * class. That is, the component must be able to cast the service object to
	 * this class. A service must be registered under this name to be considered
	 * for the set of target services.
	 * 
	 * @param interfaceName
	 *            new fully qualified name of the class used to access the
	 *            service
	 */
	public void setReferenceInterface(String interfaceName);

	/**
	 * Returns the fully qualified name of the class that is used by the
	 * component to access the service.
	 * 
	 * @return String containing the fully qualified name of the class used to
	 *         access the service
	 */
	public String getReferenceInterface();

	/**
	 * Sets if the reference is optional and if the component implementation
	 * support a single bound service or multiple bound services.
	 * 
	 * The cardinality for a reference can be specified as one of four choices:
	 * 0..1 (optional and unary), 1..1 (mandatory and unary - default), 0..n
	 * (optional and multiple), 1..n (mandatory and multiple).
	 * 
	 * @param cardinality
	 *            new cardinality value
	 */
	public void setReferenceCardinality(String cardinality);

	/**
	 * Returns if the reference is optional and if the component implementation
	 * support a single bound service or multiple bound services.
	 * 
	 * @return String containing one of four choices: 0..1 (optional and unary),
	 *         1..1 (mandatory and unary - default), 0..n (optional and
	 *         multiple), 1..n (mandatory and multiple).
	 */
	public String getReferenceCardinality();

	/**
	 * Sets the assumption of the component about dynamicity.
	 * 
	 * The policy for a reference can be specified as one of two choices: The
	 * static policy is the most simple policy and is the default one. A
	 * component instance never sees any of the dynamics. The dynamic policy is
	 * the second option and is slightly more complex since the component
	 * implementation must properly handle changes in the set of bound services.
	 * 
	 * @param policy
	 *            new value of the policy (static or dynamic)
	 * 
	 */
	public void setReferencePolicy(String policy);

	/**
	 * Return the policy of the component
	 * 
	 * @return String containing the policy value
	 */
	public String getReferencePolicy();

	/**
	 * Sets the optional OSGi Framework filter expression that further
	 * constrains the set of target services.
	 * 
	 * The default is no filter, limiting the set of matched services to all
	 * service registered under the given reference interface. The value of this
	 * attribute is used to set a target property.
	 * 
	 * @param target
	 *            the new value of attribute target
	 */
	public void setReferenceTarget(String target);

	/**
	 * Returns the target filter expression that further constrains the set of target
	 * services.
	 * 
	 * @return String containing the attribute target value
	 */
	public String getReferenceTarget();

	/**
	 * Sets the name of a method in the component implementation class that is
	 * used to notify that a service is bound to the component configuration.
	 * 
	 * For static references, this method is only called before the activate
	 * method. For dynamic references, this method can also be called while the
	 * component configuration is active.
	 * 
	 * @param bind
	 *            new method's name to notify that a service is bound
	 */
	public void setReferenceBind(String bind);

	/**
	 * Returns the name of a method in the component implementation class that
	 * is used to notify that a service is bound to the component configuration.
	 * 
	 * @return String containing the name of the method
	 */
	public String getReferenceBind();


	/**
	 * Sets the name of a method in the component implementation class that is
	 * used to notify the component configuration that the service is unbound.
	 * 
	 * For static references, the method is only called after the deactivate
	 * method. For dynamic references, this method can also be called while the
	 * component configuration is active.
	 * 
	 * @param unbind
	 *            new method's name to notify that a service is unbound
	 */
	public void setReferenceUnbind(String unbind);

	/**
	 * Returns the name of a method in the component implementation class that
	 * is used to notify the component configuration that the service is
	 * unbound.
	 * 
	 * @return String containing the name of the method
	 */
	public String getReferenceUnbind();

}
