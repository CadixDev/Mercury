package eclipse;

import java.util.function.Consumer;

@SuppressWarnings("all")
public class X {

    private final String message = "Bug?";

    public static void main(String[] args) {
        new X().doIt();
    }

    private void doIt() {
        new Sub();
    }

    private class Super<T> {

        public Super(String s) {
        }

        public Super(Consumer<T> consumer) {
        }

    }

    private class Sub extends Super<String> {

        public Sub() {
            super(s -> System.out.println(message));
        }

    }

}