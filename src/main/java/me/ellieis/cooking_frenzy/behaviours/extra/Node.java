package me.ellieis.cooking_frenzy.behaviours.extra;

import net.minecraft.world.phys.Vec3;
import xyz.nucleoid.map_templates.TemplateRegion;

public record Node(int step, Vec3 position) implements PathfinderNPC.NodeInterface {
    public static Node fromRegion(TemplateRegion region) {
        return new Node(region.getData().getInt("step").orElseThrow(), region.getBounds().centerBottom());
    }
}

