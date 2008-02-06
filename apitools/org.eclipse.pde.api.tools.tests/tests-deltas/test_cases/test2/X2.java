@interface Ann {
	int key() default 4;
}
enum E {
	A, B,  C, D
}
@interface Annot {
	int value() default 0;
	E e() default E.D;
	Ann annotation();
}
public @interface X2 {
	int id() default 0;
	String[] name() default { "toto", "tata" };
	Annot annotat() default @Annot(value=10,e=E.C,annotation=@Ann());
}