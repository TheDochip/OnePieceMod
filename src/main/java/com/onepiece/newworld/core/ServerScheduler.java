package com.onepiece.newworld.core;

import com.onepiece.newworld.OnePieceNewWorld;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Отложенные действия на сервере: эффекты, растянутые на несколько тиков
 * (волна Хаки, молнии столкновения), чтобы не слать сотни частиц в один тик.
 */
@EventBusSubscriber(modid = OnePieceNewWorld.MODID)
public final class ServerScheduler {
    private record Task(ServerLevel level, long runAt, Runnable action) {
    }

    private static final List<Task> TASKS = new ArrayList<>();

    private ServerScheduler() {
    }

    /** Выполнить действие через {@code delayTicks} тиков (0 — в конце текущего тика). */
    public static void schedule(ServerLevel level, int delayTicks, Runnable action) {
        TASKS.add(new Task(level, level.getGameTime() + Math.max(0, delayTicks), action));
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (TASKS.isEmpty()) {
            return;
        }
        List<Task> due = new ArrayList<>();
        for (Iterator<Task> it = TASKS.iterator(); it.hasNext(); ) {
            Task task = it.next();
            if (task.level().getGameTime() >= task.runAt()) {
                due.add(task);
                it.remove();
            }
        }
        for (Task task : due) {
            try {
                task.action().run();
            } catch (RuntimeException e) {
                OnePieceNewWorld.LOGGER.error("Отложенное действие упало", e);
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        TASKS.clear();
    }
}
