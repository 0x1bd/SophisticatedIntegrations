package org.kvxd.sophisticatedintegrations.terminal;

import com.tom.storagemod.block.entity.StorageTerminalBlockEntity;

import java.util.function.Supplier;

public final class TerminalContext {
    private static final ThreadLocal<TerminalSession> CURRENT = new ThreadLocal<>();

    public static TerminalSession forTerminal(StorageTerminalBlockEntity terminal) {
        TerminalSession session = CURRENT.get();
        return session != null && session.terminal() == terminal && session.canAccess() ? session : null;
    }

    public static <T> T run(TerminalSession session, Supplier<T> action) {
        TerminalSession previous = CURRENT.get();
        if (session != null && session.canAccess()) CURRENT.set(session);
        else CURRENT.remove();
        try {
            return action.get();
        } finally {
            if (previous == null) CURRENT.remove();
            else CURRENT.set(previous);
        }
    }

    private TerminalContext() {}
}
