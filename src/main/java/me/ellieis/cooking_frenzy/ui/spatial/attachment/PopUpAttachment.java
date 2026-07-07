package me.ellieis.cooking_frenzy.ui.spatial.attachment;

import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.ChunkAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.ManualAttachment;
import me.ellieis.cooking_frenzy.ui.Common;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

public class PopUpAttachment extends ChunkAttachment {
    int popupTicks;
    int popupMaxTicks;
    float popupAmount;
    public Vec3 unalteredPos;
    public PopUpAttachment(ElementHolder holder, LevelChunk chunk, Vec3 position, int popupTicks, float popupAmount) {
        super(holder, chunk, position, true);
        this.popupTicks = popupTicks;
        this.popupMaxTicks = popupTicks;
        this.popupAmount = popupAmount;
        this.unalteredPos = position.add(0, popupAmount, 0);
    }

    public static HolderAttachment of(ElementHolder holder, ServerLevel world, BlockPos pos, int popupTicks, float popupAmount) {
        return of(holder, world, Vec3.atCenterOf(pos), popupTicks, popupAmount);
    }

    public static HolderAttachment of(ElementHolder holder, ServerLevel world, Vec3 pos, int popupTicks, float popupAmount) {
        var chunk = world.getChunk(BlockPos.containing(pos));

        if (chunk instanceof LevelChunk chunk1) {
            return new PopUpAttachment(holder, chunk1, pos, popupTicks, popupAmount);
        } else {
            //CommonImpl.LOGGER.warn("Some mod tried to attach to chunk at " + BlockPos.containing(pos).toShortString() + ", but it isn't loaded!", new NullPointerException());
            return new ManualAttachment(holder, world, () -> pos);
        }
    }
    @Override
    public void tick() {
        super.tick();
        if (this.popupTicks > 0) {
            this.popupTicks--;
            float range = Common.mapRange(popupTicks, popupMaxTicks,0, popupAmount, 0);
            this.pos = this.unalteredPos.subtract(0, range, 0);
        }
    }
}
