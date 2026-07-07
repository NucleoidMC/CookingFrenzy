package me.ellieis.cooking_frenzy.scheduler;

import xyz.nucleoid.plasmid.api.game.GameSpace;

import java.util.function.Consumer;

public record Task (long executionTime, Runnable task) {
}