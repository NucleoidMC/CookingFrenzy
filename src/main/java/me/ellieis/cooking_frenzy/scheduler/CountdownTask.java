package me.ellieis.cooking_frenzy.scheduler;

import java.util.function.Consumer;

public record CountdownTask (long executionTime, long tickInterval, long i, Consumer<Long> task) {
    public CountdownTask(long executionTime, Consumer<Long> task) {
        this(executionTime, 20, 0, task);
    }
    public CountdownTask(long executionTime, long tickInterval, Consumer<Long> task) {
        this(executionTime, tickInterval, 0, task);
    }
}