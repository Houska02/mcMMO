package com.gmail.nossr50.commands.party;

import com.gmail.nossr50.core.data.UserManager;
import com.gmail.nossr50.core.datatypes.party.Party;
import com.gmail.nossr50.core.locale.LocaleLoader;
import com.gmail.nossr50.core.party.PartyManager;
import com.gmail.nossr50.core.util.commands.CommandUtils;
import com.gmail.nossr50.mcMMO;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PartyChangeOwnerCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        switch (args.length) {
            case 2:
                Party playerParty = UserManager.getPlayer((Player) sender).getParty();
                String targetName = CommandUtils.getMatchedPlayerName(args[1]);
                OfflinePlayer target = mcMMO.p.getServer().getOfflinePlayer(targetName);

                if (!playerParty.hasMember(target.getUniqueId())) {
                    sender.sendMessage(LocaleLoader.getString("Party.NotInYourParty", targetName));
                    return true;
                }

                PartyManager.setPartyLeader(target.getUniqueId(), playerParty);
                return true;

            default:
                sender.sendMessage(LocaleLoader.getString("Commands.Usage.2", "party", "owner", "<" + LocaleLoader.getString("Commands.Usage.Player") + ">"));
                return true;
        }
    }
}
