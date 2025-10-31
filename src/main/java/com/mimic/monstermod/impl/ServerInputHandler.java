package com.mimic.monstermod.impl;

import com.mimic.monstermod.capability.PlayerTransformationProvider;
import com.mimic.monstermod.identity.BaseMonsterIdentity;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * ServerInputHandler 完全版
 * - 入力のTick消費型処理
 * - 攻撃キー削除済み
 * - YSMMOD準拠
 */
public class ServerInputHandler {

    private static final ServerInputHandler INSTANCE = new ServerInputHandler();
    public static ServerInputHandler getInstance() { return INSTANCE; }

    private final Map<Player, PlayerInputState> inputStates = new WeakHashMap<>();

    public void updateInput(Player player, boolean dodge, int skillIndex) {
        inputStates.put(player, new PlayerInputState(dodge, skillIndex));
    }

    public void handleInput(Player player) {
        PlayerInputState state = inputStates.computeIfAbsent(player, k -> new PlayerInputState());

        player.getCapability(PlayerTransformationProvider.PlayerTransformationCapability.PLAYER_TRANSFORMATION)
                .ifPresent(trans -> {
                    BaseMonsterIdentity identity = trans.getIdentity();
                    if (identity == null) return;

                    if (state.dodgePressed) {
                        identity.setPendingDodge(true);
                        state.dodgePressed = false;
                    }

                    if (state.skillIndex >= 0) {
                        identity.handleClientInput(player, state.skillIndex);
                        state.skillIndex = -1;
                    }
                });
    }

    public static class PlayerInputState {
        public boolean dodgePressed = false;
        public int skillIndex = -1;

        public PlayerInputState() {}
        public PlayerInputState(boolean d, int s) {
            this.dodgePressed = d;
            this.skillIndex = s;
        }
    }
}
