package net.mimic.monstermod.mixin.Accessors;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(LivingEntity.class)
public interface LivingEntityAccessor {
    // @Invoker("playBlockSound") は存在しないか、名前が変更されたため削除
    // 必要であれば、Minecraftソースで正しいメソッド名を確認するか、別の方法でサウンドを再生します。
}