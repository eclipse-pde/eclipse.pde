public class X {
	private int i;
	
	public int foo() {
		class C {
			int value;
			C(int value) {
				this.value = value + i;
			}
			int getValue() {
				return this.value;
			}
		}
		return new C(0).getValue();
	}
}