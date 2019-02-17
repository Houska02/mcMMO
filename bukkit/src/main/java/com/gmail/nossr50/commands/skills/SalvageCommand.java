package com.gmail.nossr50.commands.skills;

import com.gmail.nossr50.core.data.UserManager;
import com.gmail.nossr50.core.locale.LocaleLoader;
import com.gmail.nossr50.core.skills.PrimarySkillType;
import com.gmail.nossr50.core.skills.SubSkillType;
import com.gmail.nossr50.core.skills.child.salvage.Salvage;
import com.gmail.nossr50.core.skills.child.salvage.SalvageManager;
import com.gmail.nossr50.core.util.TextComponentFactory;
import com.gmail.nossr50.core.util.skills.RankUtils;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class SalvageCommand extends SkillCommand {
    private boolean canAdvancedSalvage;
    private boolean canArcaneSalvage;

    public SalvageCommand() {
        super(PrimarySkillType.SALVAGE);
    }

    @Override
    protected void dataCalculations(Player player, float skillValue) {
        // TODO Auto-generated method stub

    }

    @Override
    protected void permissionsCheck(Player player) {
        canAdvancedSalvage = canUseSubskill(player, SubSkillType.SALVAGE_ADVANCED_SALVAGE);
        canArcaneSalvage = canUseSubskill(player, SubSkillType.SALVAGE_ARCANE_SALVAGE);
    }

    @Override
    protected List<String> statsDisplay(Player player, float skillValue, boolean hasEndurance, boolean isLucky) {
        List<String> messages = new ArrayList<String>();
        SalvageManager salvageManager = UserManager.getPlayer(player).getSalvageManager();

        if (canAdvancedSalvage) {
            messages.add(LocaleLoader.getString("Ability.Generic.Template", LocaleLoader.getString("Salvage.Ability.Bonus.0"),
                    LocaleLoader.getString("Salvage.Ability.Bonus.1", salvageManager.getSalvageableAmount())));
        }

        if (canArcaneSalvage) {
            messages.add(getStatMessage(false, true, SubSkillType.SALVAGE_ARCANE_SALVAGE,
                    String.valueOf(salvageManager.getArcaneSalvageRank()),
                    String.valueOf(RankUtils.getHighestRank(SubSkillType.SALVAGE_ARCANE_SALVAGE))));

            if (Salvage.arcaneSalvageEnchantLoss) {
                messages.add(LocaleLoader.getString("Ability.Generic.Template", LocaleLoader.getString("Salvage.Arcane.ExtractFull"), percent.format(salvageManager.getExtractFullEnchantChance() / 100)));
            }

            if (Salvage.arcaneSalvageDowngrades) {
                messages.add(LocaleLoader.getString("Ability.Generic.Template", LocaleLoader.getString("Salvage.Arcane.ExtractPartial"), percent.format(salvageManager.getExtractPartialEnchantChance() / 100)));
            }
        }

        return messages;
    }

    @Override
    protected List<TextComponent> getTextComponents(Player player) {
        List<TextComponent> textComponents = new ArrayList<>();

        TextComponentFactory.getSubSkillTextComponents(player, textComponents, PrimarySkillType.SALVAGE);

        return textComponents;
    }
}
