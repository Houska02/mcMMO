package com.gmail.nossr50.commands.chat;

import com.gmail.nossr50.commands.ToggleCommand;
import com.gmail.nossr50.core.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.core.locale.LocaleLoader;
import com.gmail.nossr50.core.util.Permissions;
import org.bukkit.command.CommandSender;

public class McChatSpy extends ToggleCommand {
    @Override
    protected boolean hasOtherPermission(CommandSender sender) {
        return Permissions.adminChatSpyOthers(sender);
    }

    @Override
    protected boolean hasSelfPermission(CommandSender sender) {
        return Permissions.adminChatSpy(sender);
    }

    @Override
    protected void applyCommandAction(McMMOPlayer mcMMOPlayer) {
        mcMMOPlayer.getPlayer().sendMessage(LocaleLoader.getString("Commands.AdminChatSpy." + (mcMMOPlayer.isPartyChatSpying() ? "Disabled" : "Enabled")));
        mcMMOPlayer.togglePartyChatSpying();
    }

    @Override
    protected void sendSuccessMessage(CommandSender sender, String playerName) {
        sender.sendMessage(LocaleLoader.getString("Commands.AdminChatSpy.Toggle", playerName));
    }
}
