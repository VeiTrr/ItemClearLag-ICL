package vt.icl.mixin;

import net.minecraft.entity.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ItemEntity.class)
public abstract class MixinItemEntity {

    @Shadow
    private int pickupDelay;

    @Unique
    public int getPickupDelay() {
        return this.pickupDelay;
    }
}
