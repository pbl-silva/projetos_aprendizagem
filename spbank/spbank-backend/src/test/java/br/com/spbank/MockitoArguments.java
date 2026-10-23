package br.com.spbank;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/** Bridge for Mockito's null placeholders, only inside mock stubbing/verification calls. */
@NullMarked
public final class MockitoArguments {
    private MockitoArguments() {}

    // Mockito records a matcher and returns null; the mock intercepts the call before production code.
    public static <T> T nonNullMarker(@Nullable T marker) { return marker; }
}
