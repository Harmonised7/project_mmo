package harmonised.pmmo.events;

import harmonised.pmmo.config.Config;
import harmonised.pmmo.curios.Curios;
import harmonised.pmmo.gui.ScreenshotHandler;
import harmonised.pmmo.gui.WorldXpDrop;
import harmonised.pmmo.gui.XPOverlayGUI;
import harmonised.pmmo.party.PartyPendingSystem;
import harmonised.pmmo.proxy.ClientHandler;
import harmonised.pmmo.skills.AttributeHandler;
import harmonised.pmmo.skills.CheeseTracker;
import harmonised.pmmo.skills.Skill;
import harmonised.pmmo.util.XP;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.item.BoatEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.event.TickEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

public class PlayerTickHandler
{
    private final static Map<UUID, Long> lastAward = new HashMap<>();
    private final static Map<UUID, Long> lastVeinAward = new HashMap<>();
    private final static Map<UUID, Long> lastCheeseUpdate = new HashMap<>();
    private final static Map<UUID, Long> hpRegen = new HashMap<>();
    private final static Map<UUID, Long> sync = new HashMap<>();
    private final static Map<UUID, Integer> sneakCounter = new HashMap<>();
    private final static Map<UUID, Boolean> sneakTracker = new HashMap<>();
    public static boolean syncPrefs = false;
    private static int ticksSinceAttributeRefresh = 0;

    public static void handlePlayerTick(TickEvent.PlayerTickEvent event)
    {
        PlayerEntity player = event.player;
        boolean isRemote = player.world.isRemote;

        if(XP.isPlayerSurvival(player) && player.isAlive())
        {
            UUID uuid = player.getUniqueID();

            if(player.isSprinting())
                AttributeHandler.updateSpeed(player);
            else
                AttributeHandler.resetSpeed(player);

            if(!player.world.isRemote && ticksSinceAttributeRefresh++ >= 200)
            {
                ticksSinceAttributeRefresh = 0;
                for (ServerPlayerEntity otherPlayer : player.world.getServer().getPlayerList().getPlayers())
                {
                    AttributeHandler.updateAll(otherPlayer);
                }
            }

            if(!lastAward.containsKey(uuid))
                lastAward.put(uuid, System.nanoTime());

            if(!isRemote)
            {
                ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
                if(!lastVeinAward.containsKey(uuid))
                    lastVeinAward.put(uuid, System.nanoTime());
                if(!lastCheeseUpdate.containsKey(uuid))
                    lastCheeseUpdate.put(uuid, System.nanoTime());
                if(!hpRegen.containsKey(uuid))
                    hpRegen.put(uuid, System.nanoTime());
                if(!sync.containsKey(uuid))
                    sync.put(uuid, System.nanoTime());

                //Sneak
                if(!sneakCounter.containsKey(uuid))
                {
                    sneakTracker.put(uuid, player.isSneaking());
                    sneakCounter.put(uuid, sneakTracker.get(uuid) ? 1 : 0);
                }
                int sneakCount = sneakCounter.get(uuid);
                if(player.isSneaking() && !sneakTracker.get(uuid))
                {
                    sneakCount++;
                    if(!player.world.isRemote)
                    {
                        double roll = Math.random();
                        double chance = 0.01 * (sneakCount - 250) / 5;
                        if(sneakCount > 250 && roll < chance)
                        {
                            player.attackEntityFrom(DamageSource.WITHER, (float) Math.max(1, chance*2.5));
                            System.out.println(chance);
                            if(serverPlayer.getHealth() <= 0)
                                sneakCount = 0;
                        }
                        if(sneakCount > 50)
                        {
                            double award = (sneakCount - 50) / 200D;
                            if(award > 0)
                                XP.awardXp(serverPlayer, Skill.ENDURANCE.toString(), "twerking", award, true, false, false);
                        }
                    }
                }
                sneakCounter.put(uuid, sneakCount);
                sneakTracker.put(uuid, player.isSneaking());
                //End of sneak

                double veinGap      = ((System.nanoTime() - lastVeinAward.get     (uuid)) / 1000000000D);
                double cheeseGap    = ((System.nanoTime() - lastCheeseUpdate.get  (uuid)) / 1000000000D);
                double hpRegenGap   = ((System.nanoTime() - hpRegen.get           (uuid)) / 1000000000D);
                double syncGap      = ((System.nanoTime() - sync.get              (uuid)) / 1000000000D);

                if(veinGap > 0.25)
                {
                    sneakCounter.put(uuid, Math.max(0, sneakCount - 1));
                    WorldTickHandler.updateVein(player, veinGap);
                    lastVeinAward.put(uuid, System.nanoTime());

                    if(Config.forgeConfig.antiCheeseEnabled.get() && cheeseGap > Config.forgeConfig.cheeseCheckFrequency.get())
//                if(Config.forgeConfig.antiCheeseEnabled.get() && cheeseGap > 0.1)
                    {
                        CheeseTracker.trackCheese(serverPlayer);
                        lastCheeseUpdate.put(uuid, System.nanoTime());
                    }

                    if(hpRegenGap > getHpRegenTime(player))
                    {
                        float startHp = player.getHealth();
                        player.heal(1f);
                        XP.awardXp(serverPlayer, Skill.ENDURANCE.toString(), "Regeneration", (60 / getHpRegenTime(player)) * Config.forgeConfig.hpRegenXpMultiplier.get() * (player.getHealth() - startHp), true, false, false);
                        hpRegen.put(uuid, System.nanoTime());
                    }
                }

                if(syncGap > 2.5)
                {
                    PartyPendingSystem.sendPlayerOfflineData((ServerPlayerEntity) player);

                    sync.put(uuid, System.nanoTime());
                }
            }

            double gap          = ((System.nanoTime() - lastAward.get         (uuid)) / 1000000000D);

            if(gap > 0.5)
            {
                int swimLevel = Skill.getLevel(Skill.SWIMMING.toString(), player);
                int flyLevel = Skill.getLevel(Skill.FLYING.toString(), player);
                int agilityLevel = Skill.getLevel(Skill.AGILITY.toString(), player);
                float swimAmp = EnchantmentHelper.getDepthStriderModifier(player);
                float speedAmp = 0;
                PlayerInventory inv = player.inventory;

                XP.checkBiomeLevelReq(player);

                if(!isRemote)
                {
                    if(Curios.isLoaded())
                    {
                        Curios.getCurios(player).forEach(value ->
                        {
                            for (int i = 0; i < value.getSlots(); i++)
                            {
                                XP.applyWornPenalty(player, value.getStacks().getStackInSlot(i));
                            }
                        });
                    }

                    if(!inv.getStackInSlot(39).isEmpty())	//Helm
                        XP.applyWornPenalty(player, player.inventory.getStackInSlot(39));
                    if(!inv.getStackInSlot(38).isEmpty())	//Chest
                        XP.applyWornPenalty(player, player.inventory.getStackInSlot(38));
                    if(!inv.getStackInSlot(37).isEmpty())	//Legs
                        XP.applyWornPenalty(player, player.inventory.getStackInSlot(37));
                    if(!inv.getStackInSlot(36).isEmpty())	//Boots
                        XP.applyWornPenalty(player, player.inventory.getStackInSlot(36));
                    if(!player.getHeldItemMainhand().isEmpty())
                        XP.applyEnchantmentUsePenalty(player, player.getHeldItemMainhand());
                    if(!player.getHeldItemOffhand().isEmpty())
                    {
                        XP.applyWornPenalty(player, player.getHeldItemOffhand());
                        XP.applyEnchantmentUsePenalty(player, player.getHeldItemOffhand());
                    }
                }
////////////////////////////////////////////XP_STUFF//////////////////////////////////////////

                if(player.isPotionActive(Effects.SPEED))
                    speedAmp = player.getActivePotionEffect(Effects.SPEED).getAmplifier() + 1;

                double swimAward = (3D + swimLevel    / 10.00D) * gap * (1D + swimAmp / 4D);
                double flyAward  = (1D + flyLevel     / 30.77D) * gap ;
                double runAward  = (1D + agilityLevel / 30.77D) * gap * (1D + speedAmp / 4D);

                if(!player.velocityChanged)
                    swimAward *= 0.1d;

                lastAward.replace(uuid, System.nanoTime());
                Block waterBlock = Blocks.WATER;
                Block tallSeagrassBlock = Blocks.TALL_SEAGRASS;
                Block kelpBlock = Blocks.KELP_PLANT;
                BlockPos playerPos = XP.vecToBlock(player.getPositionVec());
                Block currBlock;
                boolean waterBelow = true;

                for(int i = -1; i <= 1; i++)
                {
                    for(int j = -1; j <= 1; j++)
                    {
                        currBlock = player.getEntityWorld().getBlockState(playerPos.down().east(i).north(j)).getBlock();
                        if(!(currBlock.equals(waterBlock) || currBlock.equals(tallSeagrassBlock) || currBlock.equals(kelpBlock)))
                            waterBelow = false;
                    }
                }
                boolean waterAbove = player.getEntityWorld().getBlockState(playerPos.up()  ).getBlock().equals(waterBlock);
                boolean nightVisionPref = Config.getPreferencesMap(player).getOrDefault("underwaterNightVision", 1D) == 1;

                if(nightVisionPref && XP.isNightvisionUnlocked(player) && XP.isNightvisionUnlocked(player) && player.isInWater() && waterAbove)
                    player.addPotionEffect(new EffectInstance(Effects.NIGHT_VISION, 300, 0, false, false));

                if(!isRemote)
                {
                    ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
                    ServerWorld world = serverPlayer.getServerWorld();
                    Vector3d xpDropPos = player.getPositionVec();
                    if(player.isSprinting())
                    {
                        if(player.isInWater() && (waterAbove || waterBelow))
                        {
                            WorldXpDrop xpDrop = WorldXpDrop.fromXYZ(XP.getDimResLoc(world), xpDropPos.getX(), xpDropPos.getY(), xpDropPos.getZ(), 0.35, swimAward, Skill.SWIMMING.toString());
                            XP.addWorldXpDrop(xpDrop, serverPlayer);
                            XP.awardXp(serverPlayer, Skill.SWIMMING.toString(), "swimming fast", swimAward * 1.25f, true, false, false);
                        }
                        else
                        {
                            WorldXpDrop xpDrop = WorldXpDrop.fromXYZ(XP.getDimResLoc(world), xpDropPos.getX(), xpDropPos.getY() + 0.523, xpDropPos.getZ(), 0.15, runAward, Skill.AGILITY.toString());
                            XP.addWorldXpDrop(xpDrop, serverPlayer);
                            XP.awardXp(serverPlayer, Skill.AGILITY.toString(), "running", runAward, true, false, false);
                        }
                    }

                    if(player.isInWater() && (waterAbove || waterBelow || player.areEyesInFluid(FluidTags.WATER)))
                    {
                        if(!player.isSprinting())
                        {
                            WorldXpDrop xpDrop = WorldXpDrop.fromXYZ(XP.getDimResLoc(world), xpDropPos.getX(), xpDropPos.getY(), xpDropPos.getZ(), 0.35, swimAward, Skill.SWIMMING.toString());
                            XP.addWorldXpDrop(xpDrop, serverPlayer);
                            XP.awardXp(serverPlayer, Skill.SWIMMING.toString(), "swimming", swimAward, true, false, false);
                        }
                    }
                    else if(player.isElytraFlying())
                    {
                        WorldXpDrop xpDrop = WorldXpDrop.fromXYZ(XP.getDimResLoc(world), xpDropPos.getX(), xpDropPos.getY(), xpDropPos.getZ(), 0.35, flyAward, Skill.FLYING.toString());
                        XP.addWorldXpDrop(xpDrop, serverPlayer);
                        XP.awardXp(serverPlayer, Skill.FLYING.toString(), "flying", flyAward, true, false, false);
                    }

                    if((player.getRidingEntity() instanceof BoatEntity) && waterBelow)
                    {
                        if(!player.isSprinting())
                            swimAward *= 1.5;

                        WorldXpDrop xpDrop = WorldXpDrop.fromXYZ(XP.getDimResLoc(world), xpDropPos.getX(), xpDropPos.getY(), xpDropPos.getZ(), 0.35, swimAward, Skill.SWIMMING.toString());
                        XP.addWorldXpDrop(xpDrop, serverPlayer);
                        XP.awardXp(serverPlayer, Skill.SAILING.toString(), "Sailing", swimAward, true, false, false);
                    }
                }
////////////////////////////////////////////ABILITIES//////////////////////////////////////////
            }
        }

        if(isRemote)
        {
            if(XPOverlayGUI.screenshots.size() > 0)
            {
                for(String key : new HashSet<>(XPOverlayGUI.screenshots))
                {
                    ScreenshotHandler.takeScreenshot(key, "levelup");
                    XPOverlayGUI.screenshots.remove(key);
                    XPOverlayGUI.listOn = XPOverlayGUI.listWasOn;
                }
            }

            if(syncPrefs)
            {
                ClientHandler.syncPrefsToServer();
                syncPrefs = false;
            }
        }
    }

    public static double getHpRegenTime(PlayerEntity player)
    {
        double dividend = Config.getConfig("hpRegenPerMinuteBase") + Skill.getLevel(Skill.ENDURANCE.toString(), player) * Config.getConfig("hpRegenPerMinuteBoostPerLevel");
        return dividend <= 0 ? Double.POSITIVE_INFINITY : 60 / dividend;
    }
}