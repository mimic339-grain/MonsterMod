package com.mimic.monstermod.util;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

public class TeamUtil {

    public static void createDefaultTeams(ServerLevel level) {
        Scoreboard scoreboard = level.getScoreboard();

        createTeam(scoreboard, "monster", Component.literal("Monster Team"));
        createTeam(scoreboard, "hunter", Component.literal("Hunter Team"));
    }

    private static void createTeam(Scoreboard scoreboard, String name, Component displayName) {
        PlayerTeam team = scoreboard.getPlayerTeam(name);
        if (team == null) {
            team = scoreboard.addPlayerTeam(name);  // 正しいメソッド名に注意
            team.setDisplayName(displayName);
            team.setAllowFriendlyFire(false);
            team.setSeeFriendlyInvisibles(true);
        }
    }
}