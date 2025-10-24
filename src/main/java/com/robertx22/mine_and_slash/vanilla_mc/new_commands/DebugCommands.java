package com.robertx22.mine_and_slash.vanilla_mc.new_commands;

import com.mojang.brigadier.CommandDispatcher;
import com.robertx22.library_of_exile.command_wrapper.CommandBuilder;
import com.robertx22.library_of_exile.command_wrapper.PermWrapper;
import com.robertx22.library_of_exile.command_wrapper.StringWrapper;
import com.robertx22.mine_and_slash.event_hooks.my_events.OnResourceLost;
import com.robertx22.mine_and_slash.event_hooks.my_events.OnResourceRestore;
import com.robertx22.mine_and_slash.vanilla_mc.commands.CommandRefs;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

import java.util.List;

public class DebugCommands {

    public static void init(CommandDispatcher<CommandSourceStack> dispatcher) {

        // /mine_and_slash debug <spend_threshold|resource_restore|onexpire> <true|false|toggle|on|off>
        CommandBuilder.of(CommandRefs.ID, dispatcher, x -> {
            StringWrapper SUBJECT = new StringWrapper("subject", () -> List.of("spend_threshold", "resource_restore", "onexpire"));
            StringWrapper STATE = new StringWrapper("state", () -> List.of("true", "false", "toggle", "on", "off"));

            x.addLiteral("debug", PermWrapper.OP);
            x.addArg(SUBJECT);
            x.addArg(STATE);

            x.action(e -> {
                String subject = SUBJECT.get(e);
                String state = STATE.get(e);

                String subjLower = subject == null ? "" : subject.toLowerCase(java.util.Locale.ROOT);
                String stateLower = state == null ? "" : state.toLowerCase(java.util.Locale.ROOT);
                boolean subjOk = subjLower.equals("spend_threshold") || subjLower.equals("resource_restore") || subjLower.equals("onexpire");
                boolean stateOk = stateLower.equals("true") || stateLower.equals("false") || stateLower.equals("toggle") || stateLower.equals("on") || stateLower.equals("off");

                if (!subjOk || !stateOk) {
                    e.getSource().sendFailure(Component.literal("Usage: /" + CommandRefs.ID + " debug <spend_threshold|resource_restore|onexpire> <true|false|toggle|on|off>"));
                    return;
                }

                boolean current;
                if ("resource_restore".equalsIgnoreCase(subject)) {
                    current = OnResourceRestore.DEBUG_ENABLED;
                } else if ("spend_threshold".equalsIgnoreCase(subject)) {
                    current = OnResourceLost.DEBUG_ENABLED; // spend_threshold
                } else {
                    current = com.robertx22.mine_and_slash.mmorpg.DebugHud.ON_EXPIRE;
                }

                boolean newValue = current;
                if ("true".equalsIgnoreCase(state) || "on".equalsIgnoreCase(state)) {
                    newValue = true;
                } else if ("false".equalsIgnoreCase(state) || "off".equalsIgnoreCase(state)) {
                    newValue = false;
                } else if ("toggle".equalsIgnoreCase(state)) {
                    newValue = !current;
                }

                if ("resource_restore".equalsIgnoreCase(subject)) {
                    OnResourceRestore.DEBUG_ENABLED = newValue;
                } else if ("spend_threshold".equalsIgnoreCase(subject)) {
                    OnResourceLost.DEBUG_ENABLED = newValue;
                } else {
                    com.robertx22.mine_and_slash.mmorpg.DebugHud.ON_EXPIRE = newValue;
                }

                String label = "resource_restore".equalsIgnoreCase(subject) ? "ResourceRestore" : "spend_threshold".equalsIgnoreCase(subject) ? "SpendThresholds" : "OnExpire";
                Component msg = Component.literal("[" + label + "] Debug: " + (newValue ? "ON" : "OFF"));
                CommandSourceStack src = e.getSource();
                if (src.getEntity() instanceof Player p) {
                    p.sendSystemMessage(msg);
                } else {
                    src.sendSuccess(() -> msg, true);
                }
            });

        }, "Toggle debug: /" + CommandRefs.ID + " debug <spend_threshold|resource_restore|onexpire> <true|false|toggle|on|off>");
    }
}


