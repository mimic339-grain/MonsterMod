package com.mimic.monstermod.mixin.player;

import com.mimic.monstermod.entity.hitbox.BoneHitboxPart;
import com.mimic.monstermod.entity.hitbox.BoneHitboxRegistry;
import com.mimic.monstermod.entity.hitbox.IHitboxPartOwner;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.entity.PartEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * プレイヤーがモンスターに変身した際、変身先の部位ごとの当たり判定(頭は弱点、など)を
 * 成立させるためのMixin。
 *
 * 【なぜ常にパーツを確保するのか】
 * Forgeは ServerLevel$EntityCallbacks#onTrackingStart(=エンティティがワールドに
 * 追加された瞬間)にしか PartEntity を登録しない。変身したタイミングで動的にパーツを
 * 追加しても登録されず、当たり判定として一切機能しない。
 * そのため「ログイン時に固定数を確保しておき、非変身時は休眠させる」方式を取る。
 *
 * 非変身時のパーツはサイズ0・isPickable()=false なので、通常プレイへの影響はない。
 * ticksされる訳でもなく、保持コストは空のEntityオブジェクト数個分のみ。
 */
@Mixin(Player.class)
public abstract class PlayerMultipartMixin implements IHitboxPartOwner {

    @Unique
    private BoneHitboxPart[] monstermod$hitboxParts;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void monstermod$initHitboxParts(CallbackInfo ci) {
        Player self = (Player) (Object) this;
        BoneHitboxPart[] parts = new BoneHitboxPart[BoneHitboxRegistry.MAX_PARTS_PER_PLAYER];
        for (int i = 0; i < parts.length; i++) {
            parts[i] = new BoneHitboxPart(self);
            parts[i].deactivate();
        }
        this.monstermod$hitboxParts = parts;
    }

    @Override
    public BoneHitboxPart[] monstermod$getHitboxParts() {
        return this.monstermod$hitboxParts;
    }

    /**
     * 常に true を返す。変身していない間もパーツは休眠状態で存在するが、
     * サイズ0かつ isPickable()=false なので判定には引っかからない。
     * (Forgeの登録タイミングの都合上、ここを動的に変えることはできない)
     */
    public boolean isMultipartEntity() {
        return this.monstermod$hitboxParts != null;
    }

    public PartEntity<?>[] getParts() {
        return this.monstermod$hitboxParts;
    }
}
