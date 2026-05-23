package com.mimic.monstermod.mixin.player;

import net.minecraft.client.OptionInstance;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.VideoSettingsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VideoSettingsScreen.class)
public abstract class VideoSettingsScreenMixin extends Screen {
    @Shadow private OptionsList list; // ビデオ設定のスクロールリスト

    protected VideoSettingsScreenMixin(Component title) {
        super(title);
    }
    @Inject(method = "init", at = @At("TAIL"))
    private void addMonsterModConfigToContainer(CallbackInfo ci) {
        if (this.list != null) {
            OptionInstance<Boolean> cleanOption = new OptionInstance<>(
                    "MonsterMod 描画設定を開く", // ここをボタンのメインテキストにする
                    OptionInstance.noTooltip(),
                    // ★ 最大のトリック
                    // 通常は "タイトル: 値" となるところを、
                    // 値の部分を「空文字」ではなく「描画されない特殊な空白」にします。
                    // これにより、バニラのロジックが ": " を追加しても、実質的にタイトルだけに見えます。
                    (label, value) -> Component.literal(""),
                    OptionInstance.BOOLEAN_VALUES,
                    false,
                    (val) -> {
                        if (this.minecraft != null) {
                            this.minecraft.setScreen(new com.mimic.monstermod.gui.VisualConfigScreen(this));
                        }
                    }
            );

            this.list.addBig(cleanOption);
        }
    }
}