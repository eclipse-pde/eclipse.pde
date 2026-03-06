package a.api;
public class AClass {
	// bundle B b.api package imported by A
	public Object objectFromB_allowed = new b.api.MyObject();
	public Object objectFromB_restricted = new  b.internal.MyObject();

	// bundle G reexported via B, g.api package is imported by A
	public Object objectFromG_allowed = new g.api.MyObject();
	public Object objectFromG_restricted = new g.internal.MyObject();

	/*
	 * All references below never compilable before https://github.com/eclipse-pde/eclipse.pde/pull/2218
	 *
	 * bundles C, D, E, F, H not required by A, neither package is imported by A
	 * bundles C, D, E, F, H only referenced in different ways from bundle B or G and not reexported
	 */

	// Bundle C directly required by B, but not reexported by B
	public Object objectFromC_not_accessible1 = new c.api.MyObject();
	public Object objectFromC_not_accessible2 = new c.internal.MyObject();

	// Bundle D package imported by B, but not reexported by B
	public Object objectFromD_not_accessible1 = new d.api.MyObject();
	public Object objectFromD_not_accessible2 = new d.internal.MyObject();

	/*
	 * Regression introduced: all (transitive optional) dependencies from B or G are now accessible from A,
	 * even if not reexported by B or G and not imported by A.
	 */
	// Optionally required or imported by B, but not reexported by B
	public Object objectFromE_not_accessible1 = new e.api.MyObject();
	public Object objectFromE_not_accessible2 = new e.internal.MyObject();
	public Object objectFromF_not_accessible1 = new f.api.MyObject();
	public Object objectFromF_not_accessible2 = new f.internal.MyObject();

	// Bundle H package optionally imported by G, not reexported by anyone
	public Object objectFromH_not_accessible1 = new h.api.MyObject();
	public Object objectFromH_not_accessible2 = new h.internal.MyObject();
}
