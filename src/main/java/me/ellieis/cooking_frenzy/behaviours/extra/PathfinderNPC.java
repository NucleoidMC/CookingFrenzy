package me.ellieis.cooking_frenzy.behaviours.extra;

import me.ellieis.cooking_frenzy.behaviours.CustomerBehaviour;
import me.ellieis.cooking_frenzy.mixins.MannequinAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Set;

public abstract class PathfinderNPC {
    ArrayList<NodeInterface> nodes;
    int currentNodeId;
    Vec3 pointToNavigate;
    Vec3 oldPos;
    public Mannequin entity;
    ServerLevel level;
    double distanceToPoint;
    long timeToWalk;
    long walkCounter;
    double alpha = 0;
    float currentYaw;
    boolean walkReversePath = false;
    public boolean despawnFlag = false;

    public PathfinderNPC(ArrayList<NodeInterface> nodes, ServerLevel level, Vec3 spawnPos, ResolvableProfile skin) {
        this.nodes = nodes;
        this.level = level;
        this.currentNodeId = 0;
        this.entity = new Mannequin(EntityTypes.MANNEQUIN, this.level);
        ((MannequinAccessor) this.entity).cooking_frenzy$setMannequinProfile(skin);
        this.entity.setInvulnerable(true);
        this.entity.teleportTo(spawnPos.x(), spawnPos.y(), spawnPos.z());
        this.oldPos = this.entity.position();
        this.level.addFreshEntity(this.entity);
    }
    /**
     * Advances through the node tree
     * @param reverse Whether to walk the node tree in reverse (example, going back to spawn after sitting)
     * @return if it was able to trace a path
     */
    public boolean advanceNode(boolean reverse) {
        int oldNodeId = currentNodeId;
        this.oldPos = this.entity.position();
        if (reverse) {
            currentNodeId--;
        } else {
            currentNodeId++;
        }

        if (reverse) {
            if (currentNodeId < 0) {
                this.despawnFlag = true;
                return false;
            }
        } else if (currentNodeId >= nodes.size()) {
            return false;
        }

        NodeInterface currentNode = null;
        NodeInterface oldNode = null;
        for (NodeInterface node : nodes) {
            if (node.step() == currentNodeId) {
                currentNode = node;
            } else if (node.step() == oldNodeId) {
                oldNode = node;
            }
        }

        if (currentNode == null) {
            this.pointToNavigate = null;
            return false;
        }
        if (oldNode == null) {
            oldNode = new Node(this.currentNodeId, currentNode.position());
        }
        this.pointToNavigate = currentNode.position();
        this.distanceToPoint = oldNode.position().distanceTo(this.pointToNavigate);
        // 4.317 is the base walk speed in blocks
        this.timeToWalk = (long) ((this.distanceToPoint / (4.317 / 2)) * 20);
        this.walkCounter = 0;
        this.alpha = 1;
        double dx = pointToNavigate.x - oldPos.x;
        double dz = pointToNavigate.z - oldPos.z;
        this.currentYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        return true;
    }

    private void pathfind() {
        walkCounter++;
        this.alpha = (double) walkCounter  / timeToWalk;
        if (this.alpha > 1) {
            if (advanceNode(walkReversePath)) {
                this.alpha = (double) walkCounter / timeToWalk;
            } else {
                this.pointToNavigate = null;
                this.onPathFinished();
                return;
            }
        }
        Vec3 pos = oldPos.lerp(this.pointToNavigate, this.alpha);
        this.entity.teleportTo(this.level, pos.x(), pos.y(), pos.z(), Set.of(), currentYaw, 0, false);
    }

    public void setShouldWalkReversePath(boolean val) {
        this.walkReversePath = val;
    }

    public boolean getIsWalkingReversePath() {
        return this.walkReversePath;
    }

    abstract void onPathFinished();

    public void tick() {
        if (this.pointToNavigate != null) {
            this.pathfind();
        }
    }
    public interface NodeInterface {
        int step();
        Vec3 position();
    }}
