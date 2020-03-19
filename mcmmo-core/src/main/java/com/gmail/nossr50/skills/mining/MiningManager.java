package com.gmail.nossr50.skills.mining;

import com.gmail.nossr50.core.MetadataConstants;
import com.gmail.nossr50.datatypes.experience.XPGainReason;
import com.gmail.nossr50.datatypes.interactions.NotificationType;
import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.datatypes.skills.SubSkillType;
import com.gmail.nossr50.datatypes.skills.SuperAbilityType;
import com.gmail.nossr50.datatypes.skills.behaviours.MiningBehaviour;
import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.runnables.skills.AbilityCooldownTask;
import com.gmail.nossr50.skills.SkillManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Container;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class MiningManager extends SkillManager {
    
    private final MiningBehaviour miningBehaviour;
    
    public MiningManager(mcMMO pluginRef, McMMOPlayer mcMMOPlayer) {
        super(pluginRef, mcMMOPlayer, PrimarySkillType.MINING);
        
        //Init behaviour
        miningBehaviour = pluginRef.getDynamicSettingsManager().getSkillBehaviourManager().getMiningBehaviour();
    }

    public double getOreBonus(int rank) {
        return pluginRef.getConfigManager().getConfigMining().getBlastMining().getOreBonus(rank);
    }

    public double getDebrisReduction(int rank) {
        return pluginRef.getConfigManager().getConfigMining().getBlastMining().getDebrisReduction(rank);
    }

    public int getDropMultiplier(int rank) {
        return pluginRef.getConfigManager().getConfigMining().getBlastMining().getDropMultiplier(rank);
    }

    public boolean canUseDemolitionsExpertise() {
        if (!pluginRef.getRankTools().hasUnlockedSubskill(getPlayer(), SubSkillType.MINING_DEMOLITIONS_EXPERTISE))
            return false;

        return getSkillLevel() >= miningBehaviour.getDemolitionExpertUnlockLevel() && pluginRef.getPermissionTools().demolitionsExpertise(getPlayer());
    }

    public boolean canDetonate() {
        Player player = getPlayer();

        return canUseBlastMining() && player.isSneaking()
                && miningBehaviour.isDetonator(player.getInventory().getItemInMainHand())
                && pluginRef.getPermissionTools().remoteDetonation(player);
    }

    public boolean canUseBlastMining() {
        //Not checking permissions?
        return pluginRef.getRankTools().hasUnlockedSubskill(getPlayer(), SubSkillType.MINING_BLAST_MINING);
    }

    public boolean canUseBiggerBombs() {
        if (!pluginRef.getRankTools().hasUnlockedSubskill(getPlayer(), SubSkillType.MINING_BIGGER_BOMBS))
            return false;

        return getSkillLevel() >= miningBehaviour.getBiggerBombsUnlockLevel() && pluginRef.getPermissionTools().biggerBombs(getPlayer());
    }

    public boolean canDoubleDrop() {
        return pluginRef.getRankTools().hasUnlockedSubskill(getPlayer(), SubSkillType.MINING_DOUBLE_DROPS) && pluginRef.getPermissionTools().isSubSkillEnabled(getPlayer(), SubSkillType.MINING_DOUBLE_DROPS);
    }

    /**
     * Process double drops & XP gain for miningBehaviour.
     *
     * @param blockState The {@link BlockState} to check ability activation for
     */
    public void miningBlockCheck(BlockState blockState) {
        Player player = getPlayer();

        applyXpGain(miningBehaviour.getBlockXp(blockState), XPGainReason.PVE);

        if (mcMMOPlayer.getSuperAbilityMode(pluginRef.getSkillTools().getSuperAbility(skill))) {
            pluginRef.getSkillTools().handleDurabilityChange(getPlayer().getInventory().getItemInMainHand(), pluginRef.getConfigManager().getConfigSuperAbilities().getSuperAbilityLimits().getToolDurabilityDamage());
        }

        if (!canDoubleDrop() || !pluginRef.getDynamicSettingsManager().getBonusDropManager().isBonusDropWhitelisted(blockState.getType()))
            return;

        boolean silkTouch = player.getInventory().getItemInMainHand().containsEnchantment(Enchantment.SILK_TOUCH);

        if (silkTouch && !pluginRef.getConfigManager().getConfigMining().getMiningSubSkills().getDoubleDrops().isAllowSilkTouchDoubleDrops())
            return;

        //TODO: Make this readable
        if (pluginRef.getRandomChanceTools().checkRandomChanceExecutionSuccess(getPlayer(), SubSkillType.MINING_DOUBLE_DROPS)) {
            pluginRef.getBlockTools().markDropsAsBonus(blockState, mcMMOPlayer.getSuperAbilityMode(pluginRef.getSkillTools().getSuperAbility(skill)));
        }
    }

    /**
     * Detonate TNT for Blast Mining
     */
    public void remoteDetonation() {
        Player player = getPlayer();
        Block targetBlock = player.getTargetBlock(pluginRef.getBlockTools().getTransparentBlocks(), miningBehaviour.MAXIMUM_REMOTE_DETONATION_DISTANCE);

        //Blast mining cooldown check needs to be first so the player can be messaged
        if (!blastMiningCooldownOver() || targetBlock.getType() != Material.TNT || !pluginRef.getEventManager().simulateBlockBreak(targetBlock, player, true)) {
            return;
        }

        TNTPrimed tnt = player.getWorld().spawn(targetBlock.getLocation(), TNTPrimed.class);

        //SkillUtils.sendSkillMessage(player, SuperAbilityType.BLAST_MINING.getAbilityPlayer(player));
        pluginRef.getNotificationManager().sendPlayerInformation(player, NotificationType.SUPER_ABILITY, "Mining.Blast.Boom");
        //player.sendMessage(LocaleLoader.getString("Mining.Blast.Boom"));

        tnt.setMetadata(MetadataConstants.TNT_TRACKING_METAKEY, mcMMOPlayer.getPlayerMetadata());
        tnt.setFuseTicks(0);
        targetBlock.setType(Material.AIR);

        mcMMOPlayer.setAbilityDATS(SuperAbilityType.BLAST_MINING, System.currentTimeMillis());
        mcMMOPlayer.setAbilityInformed(SuperAbilityType.BLAST_MINING, false);
        new AbilityCooldownTask(pluginRef, mcMMOPlayer, SuperAbilityType.BLAST_MINING).runTaskLater(pluginRef, pluginRef.getSkillTools().getSuperAbilityCooldown(SuperAbilityType.BLAST_MINING) * pluginRef.getMiscTools().TICK_CONVERSION_FACTOR);
    }

    /**
     * Handler for explosion drops and XP gain.
     *
     * @param yield The % of blocks to drop
     * @param event The {@link EntityExplodeEvent}
     */
    //TODO: Rewrite this garbage
    //TODO: Rewrite this garbage
    //TODO: Rewrite this garbage
    //TODO: Rewrite this garbage
    //TODO: Rewrite this garbage
    //TODO: Rewrite this garbage
    //TODO: Rewrite this garbage
    //TODO: Rewrite this garbage
    //TODO: Rewrite this garbage
    //TODO: Rewrite this garbage
    //TODO: Rewrite this garbage
    //TODO: Rewrite this garbage
    //TODO: Rewrite this garbage
    //TODO: Rewrite this garbage
    //TODO: Rewrite this garbage
    //TODO: Rewrite this garbage
    //TODO: Rewrite this garbage
    //TODO: Rewrite this garbage
    //TODO: Rewrite this garbage
    //TODO: Rewrite this garbage
    //TODO: Rewrite this garbage
    //TODO: Rewrite this garbage
    //TODO: Rewrite this garbage
    //TODO: Rewrite this garbage
    public void blastMiningDropProcessing(float yield, EntityExplodeEvent event) {
        //Strip out only stuff that gives mining XP

        List<BlockState> ores = new ArrayList<BlockState>();

        List<Block> notOres = new ArrayList<>();
        for (Block targetBlock : event.blockList()) {
            //Containers usually have 0 XP unless someone edited their config in a very strange way
            if (pluginRef.getDynamicSettingsManager().getExperienceManager().getMiningXp(targetBlock.getType()) == 0 || targetBlock instanceof Container || pluginRef.getPlaceStore().isTrue(targetBlock)) {
                notOres.add(targetBlock);
            } else {
                ores.add(targetBlock.getState());
            }
        }

        int xp = 0;

//        float oreBonus = (float) (getOreBonus() / 100);
        //TODO: Pretty sure something is fucked with debrisReduction stuff
//        float debrisReduction = (float) (getDebrisReduction() / 100);
        int dropMultiplier = getDropMultiplier();

//        float debrisYield = yield - debrisReduction;

        for (BlockState blockState : ores) {
            if (pluginRef.getMiscTools().getRandom().nextInt(ores.size()) >= (ores.size() / 2)) {
                xp += pluginRef.getDynamicSettingsManager().getExperienceManager().getMiningXp(blockState.getType());

                pluginRef.getMiscTools().dropItem(pluginRef.getMiscTools().getBlockCenter(blockState), new ItemStack(blockState.getType())); // Initial block that would have been dropped

                for (int i = 1; i < dropMultiplier; i++) {
                    if(pluginRef.getMiscTools().getRandom().nextInt(100) >= 75)
                        miningBehaviour.handleSilkTouchDrops(blockState); // Bonus drops - should drop the block & not the items
                }
            }
        }

        //Replace the event blocklist with the newYield list
        event.setYield(0F);

        applyXpGain(xp, XPGainReason.PVE);
    }

    /**
     * Increases the blast radius of the explosion.
     *
     * @param radius to modify
     * @return modified radius
     */
    public float biggerBombs(float radius) {
        return (float) (radius + getBlastRadiusModifier());
    }

    public double processDemolitionsExpertise(double damage) {
        return damage * ((100.0D - getBlastDamageModifier()) / 100.0D);
    }

    /**
     * Gets the Blast Mining tier
     *
     * @return the Blast Mining tier
     */
    public int getBlastMiningTier() {
        return pluginRef.getRankTools().getRank(getPlayer(), SubSkillType.MINING_BLAST_MINING);
    }

    /**
     * Gets the Blast Mining tier
     *
     * @return the Blast Mining tier
     */
    public double getOreBonus() {
        return getOreBonus(getBlastMiningTier());
    }

    /**
     * Gets the Blast Mining tier
     *
     * @return the Blast Mining tier
     */
    public double getDebrisReduction() {
        return getDebrisReduction(getBlastMiningTier());
    }

    /**
     * Gets the Blast Mining tier
     *
     * @return the Blast Mining tier
     */
    public int getDropMultiplier() {
        return getDropMultiplier(getBlastMiningTier());
    }

    /**
     * Gets the Blast Mining tier
     *
     * @return the Blast Mining tier
     */
    public double getBlastRadiusModifier() {
        return miningBehaviour.getBlastRadiusModifier(getBlastMiningTier());
    }

    /**
     * Gets the Blast Mining tier
     *
     * @return the Blast Mining tier
     */
    public double getBlastDamageModifier() {
        return miningBehaviour.getBlastDamageDecrease(getBlastMiningTier());
    }

    private boolean blastMiningCooldownOver() {
        int timeRemaining = mcMMOPlayer.calculateTimeRemaining(SuperAbilityType.BLAST_MINING);

        if (timeRemaining > 0) {
            //getPlayer().sendMessage(pluginRef.getLocaleManager().getString("Skills.TooTired", timeRemaining));
            pluginRef.getNotificationManager().sendPlayerInformation(getPlayer(), NotificationType.ABILITY_COOLDOWN, "Skills.TooTired", String.valueOf(timeRemaining));
            return false;
        }

        return true;
    }
}