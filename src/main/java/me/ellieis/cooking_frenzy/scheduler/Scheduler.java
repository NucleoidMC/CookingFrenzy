package me.ellieis.cooking_frenzy.scheduler;

import net.minecraft.world.level.Level;
import xyz.nucleoid.plasmid.api.game.GameSpace;

import java.util.ArrayList;

public class Scheduler {
    private final ArrayList<Task> tasks = new ArrayList<>();
    private final ArrayList<CountdownTask> countdownTasks = new ArrayList<>();
    long time;
    public Scheduler(long time) {
        this.time = time;
    }
    public ArrayList<Task> getTasks() {
        return tasks;
    }

    public void addTask(Task task) {
        tasks.add(task);
    }
    public void addCountdown(CountdownTask task) {
        for (int i = 0; i < (task.executionTime() - time) / task.tickInterval(); i++) {
            countdownTasks.add(new CountdownTask(time + task.tickInterval() * i, task.tickInterval(), i, task.task()));
        }
    }
    public void clearTasks() {
        this.tasks.clear();
    }
    public void onTick(long time) {
        this.time = time;
        for (int i = 0; i < tasks.size(); i++) {
            Task task = tasks.get(i);
            if (task.executionTime() <= time) {
                task.task().run();
                tasks.remove(task);
            }
        }
        for (int i = 0; i < countdownTasks.size(); i++) {
            CountdownTask task = countdownTasks.get(i);
            if (task.executionTime() <= time) {
                task.task().accept(task.i() + 1);
                countdownTasks.remove(task);
            }
        }
    }
}
