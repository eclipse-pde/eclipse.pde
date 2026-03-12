package a.api;

/**
 * Test consumer that references only directly imported API from B, G, and X,
 * exercising the rich type hierarchy and x-internal discouraged access.
 * <p>
 * Bundle Ad declares:
 * {@code Import-Package: b.api, g.api, x.api, x.internal}
 * <p>
 * Expected markers:
 * <ul>
 * <li>b.api, g.api, x.api: {@code K_ACCESSIBLE} → <b>no markers</b></li>
 * <li>x.internal: {@code K_DISCOURAGED} ({@code x-internal:=true})
 * → <b>discouraged access warning</b></li>
 * </ul>
 * <p>
 * No transitive dependency types are directly referenced — Ad only uses
 * directly imported packages. This validates that exported API from direct
 * dependencies produces zero markers, and that x-internal packages produce
 * discouraged (not forbidden) markers.
 */
public class AClass {
	// ---- Directly imported bundles: exported API usage ----

	// B exports b.api → K_ACCESSIBLE → no marker
	// b.api.MyObject extends c.api.MyObject implements d.api.Processor
	public b.api.MyObject service = new b.api.MyObject();

	// Exercise inherited methods from transitive type hierarchy — no markers
	public boolean setup() {
		service.configure("settings");
		return service.isConfigured();
	}

	// G exports g.api → K_ACCESSIBLE → no marker
	public g.api.MyObject gService = new g.api.MyObject();
	public String desc = gService.describe();

	// X exports x.api → K_ACCESSIBLE → no marker
	public x.api.MyObject xService = new x.api.MyObject();
	public String xName = xService.getName();

	// X exports x.internal with x-internal:=true → K_DISCOURAGED → warning
	public Object objectFromX_discouraged = new x.internal.MyObject();
}
