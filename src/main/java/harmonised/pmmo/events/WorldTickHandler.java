package harmonised.pmmo.events;

import harmonised.pmmo.config.FConfig;
import harmonised.pmmo.config.JType;
import harmonised.pmmo.config.JsonConfig;
import harmonised.pmmo.network.MessageUpdateBoolean;
import harmonised.pmmo.network.MessageUpdatePlayerNBT;
import harmonised.pmmo.network.NetworkHandler;
import harmonised.pmmo.skills.AttributeHandler;
import harmonised.pmmo.skills.Skill;
import harmonised.pmmo.skills.VeinInfo;
import harmonised.pmmo.util.NBTHelper;
import harmonised.pmmo.util.XP;
import net.minecraft.block.Block;
import net.minecraft.block.BlockCrops;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.init.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Enchantments;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

public class WorldTickHandler
{
    public static final Logger LOGGER = LogManager.getLogger();

    public static Map<EntityPlayer, VeinInfo> activeVein;
    public static Map<EntityPlayer, ArrayList<BlockPos>> veinSet;
    private static double minVeinCost, minVeinHardness, levelsPerHardnessMining, levelsPerHardnessWoodcutting, levelsPerHardnessExcavation, levelsPerHardnessFarming, levelsPerHardnessCrafting, veinMaxBlocks, maxVeinCharge, exhaustionPerBlock;
    private static int veinMaxDistance;
//    public static long lastVeinUpdateTime = System.nanoTime();

    public static void refreshVein()
    {
        activeVein = new HashMap<>();
        veinSet = new HashMap<>();

        minVeinCost = FConfig.getConfig( "minVeinCost" );
        minVeinHardness = FConfig.getConfig( "minVeinHardness" );
        levelsPerHardnessMining = FConfig.getConfig( "levelsPerHardnessMining" );
        levelsPerHardnessWoodcutting = FConfig.getConfig( "levelsPerHardnessWoodcutting" );
        levelsPerHardnessExcavation = FConfig.getConfig( "levelsPerHardnessExcavation" );
        levelsPerHardnessFarming = FConfig.getConfig( "levelsPerHardnessFarming" );
        levelsPerHardnessCrafting = FConfig.getConfig( "levelsPerHardnessCrafting" );
        veinMaxDistance = (int) Math.floor( FConfig.veinMaxDistance );
        exhaustionPerBlock = FConfig.exhaustionPerBlock;
        veinMaxBlocks = FConfig.veinMaxBlocks;
        maxVeinCharge = FConfig.maxVeinCharge;
    }

    public static void handleWorldTick( TickEvent.WorldTickEvent event )
    {
        int veinSpeed = (int) Math.floor( FConfig.veinSpeed );
        VeinInfo veinInfo;
        World world;
        ItemStack startItemStack;
        Item startItem;
        BlockPos veinPos;
        IBlockState veinState;
        Map<String, Double> abilitiesMap;
        String regKey;
        String skill;
        double cost;
        boolean correctBlock, correctItem, correctHeldItem, isOwner;
        UUID blockUUID, playerUUID;

        if( event.world.getMinecraftServer() == null )
            return;

        for( EntityPlayer thePlayer : event.world.getMinecraftServer().getPlayerList().getPlayers() )
        {
            final EntityPlayerMP player = (EntityPlayerMP) thePlayer;
            playerUUID = player.getUniqueID();

            for( int i = 0; i < veinSpeed; i++ )
            {
                if( activeVein.containsKey( player ) && veinSet.get( player ).size() > 0 )
                {
                    veinInfo = activeVein.get(player);
                    world = veinInfo.world;
                    startItemStack = veinInfo.itemStack;
                    startItem = veinInfo.startItem;
                    veinPos = veinSet.get( player ).get( 0 );
                    veinState = world.getBlockState( veinPos );
                    abilitiesMap = FConfig.getAbilitiesMap( player );
                    regKey = veinState.getBlock().getRegistryName().toString();
                    cost = getVeinCost( veinState, veinPos, player );
                    correctBlock = world.getBlockState( veinPos ).getBlock().equals( veinInfo.state.getBlock() );
                    correctItem = !startItem.isDamageable() || ( startItemStack.getItemDamage() < startItemStack.getMaxDamage() );
                    correctHeldItem = player.getHeldItemMainhand().getItem().equals( startItem );
                    blockUUID = ChunkDataHandler.checkPos( world.getWorldType().getId(), veinPos );
                    isOwner = blockUUID == null || blockUUID.equals( playerUUID );
                    skill = XP.getSkill( veinState );

                    if( veinState.getBlock() instanceof BlockCrops && skill.equals( Skill.FARMING.toString() ) && !( JsonConfig.data.get( JType.BLOCK_SPECIFIC ).containsKey( regKey ) && JsonConfig.data.get( JType.BLOCK_SPECIFIC ).get( regKey ).containsKey( "growsUpwards" ) ) )
                    {
                        BlockCrops blockCrops = (BlockCrops) veinState.getBlock();

                        if( blockCrops.isMaxAge( veinState ) )
                        {
                            veinSet.get( player ).remove( 0 );
                            return;
                        }
                    }

                    if( ( abilitiesMap.get( "veinLeft" ) >= cost || player.isCreative() ) && XP.isVeining.contains( player.getUniqueID() ) )
                    {
                        veinSet.get( player ).remove( 0 );

                        BlockEvent.BreakEvent veinEvent = new BlockEvent.BreakEvent( world, veinPos, veinState, player );
                        MinecraftForge.EVENT_BUS.post(veinEvent);

                        if ( !veinEvent.isCanceled() )
                        {
                            if( correctBlock )
                            {
                                if( player.isCreative() )
                                    world.destroyBlock(veinPos, false );
                                else if( correctItem && correctHeldItem && player.getFoodStats().getFoodLevel() > 0 )
                                {
                                    if( FConfig.veiningOtherPlayerBlocksAllowed || isOwner )
                                    {
                                        abilitiesMap.put("veinLeft", abilitiesMap.get("veinLeft") - cost);
                                        destroyBlock( world, veinPos, player, startItemStack );
                                        player.addExhaustion( (float) exhaustionPerBlock );
                                    }
                                }
                                else
                                {
                                    activeVein.remove( player );
                                    veinSet.remove( player );
                                    NetworkHandler.sendToPlayer( new MessageUpdateBoolean( false, 0 ), (EntityPlayerMP) player );
                                }
                            }
                        }
                    }
                    else
                    {
                        activeVein.remove( player );
                        veinSet.remove( player );
                        NetworkHandler.sendToPlayer( new MessageUpdateBoolean( false, 0 ), (EntityPlayerMP) player );
                    }
                }
                else
                {
                    activeVein.remove( player );
                    veinSet.remove( player );
                    NetworkHandler.sendToPlayer( new MessageUpdateBoolean( false, 0 ), (EntityPlayerMP) player );
                }
            }
        }
    }

    public static void destroyBlock( World world, BlockPos pos, EntityPlayerMP player, ItemStack toolUsed )
    {
        IBlockState state = world.getBlockState(pos);
        world.playEvent(2001, pos, Block.getStateId(state) );

        player.interactionManager.blockRemoving(pos);
        state.getBlock().harvestBlock(world, player, pos, state, world.getTileEntity(pos), toolUsed);
        state.getBlock().removedByPlayer(state, world, pos, player, true);

        if( FConfig.damageToolWhileVeining && toolUsed.isItemStackDamageable() && !player.isCreative() )
            toolUsed.damageItem( 1, player );
    }

    public static void scheduleVein(EntityPlayer player, VeinInfo veinInfo )
    {
        double veinLeft = FConfig.getAbilitiesMap( player ).getOrDefault( "veinLeft", 0D );
        double veinCost = getVeinCost( veinInfo.state, veinInfo.pos, player );
        String blockKey = veinInfo.state.getBlock().getRegistryName().toString();
        ArrayList<BlockPos> blockPosArrayList;

        if( !( canVeinGlobal( blockKey, player ) && canVeinDimension( blockKey, player )  ) || !XP.checkReq( player, player.getHeldItemMainhand().getItem().getRegistryName(), JType.REQ_TOOL ) )
            return;

        blockPosArrayList = getVeinShape( veinInfo, veinLeft, veinCost, player.isCreative(), false );

        if( blockPosArrayList.size() > 0 )
        {
            activeVein.put( player, veinInfo );
            veinSet.put( player, blockPosArrayList );
            NetworkHandler.sendToPlayer( new MessageUpdateBoolean( true, 0 ), (EntityPlayerMP) player );
        }
    }

    public static boolean canVeinGlobal( String blockKey, EntityPlayer player )
    {
        if( player.isCreative() )
            return true;

        Map<String, Double> globalBlacklist = null;

        if( JsonConfig.data.get( JType.VEIN_BLACKLIST ).containsKey( "all_dimensions" ) )
            globalBlacklist = JsonConfig.data.get( JType.VEIN_BLACKLIST ).get( "all_dimensions" );

        return globalBlacklist == null || !globalBlacklist.containsKey(blockKey);
    }

    public static boolean canVeinDimension( String blockKey, EntityPlayer player )
    {
        if( player.isCreative() )
            return true;

        World world = player.world;
        if( world == null )
            return true;

        String dimensionKey = Integer.toString( world.getWorldType().getId() );

        Map<String, Double> dimensionBlacklist = null;

        if( JsonConfig.data.get( JType.VEIN_BLACKLIST ).containsKey( dimensionKey.toString() ) )
            dimensionBlacklist = JsonConfig.data.get( JType.VEIN_BLACKLIST ).get( dimensionKey.toString() );

        return dimensionBlacklist == null || !dimensionBlacklist.containsKey(blockKey);
    }

    private static ArrayList<BlockPos> getVeinShape( VeinInfo veinInfo, double veinLeft, double veinCost, boolean isCreative, boolean isLooped )
    {
        Set<BlockPos> vein = new HashSet<>();
        ArrayList<BlockPos> outVein = new ArrayList<>();
        ArrayList<BlockPos> curLayer = new ArrayList<>();
        ArrayList<BlockPos> nextLayer = new ArrayList<>();
        BlockPos originPos = veinInfo.pos;
        BlockPos highestPos = originPos;
        curLayer.add( originPos );
        BlockPos curPos2;
        Block block = veinInfo.state.getBlock();
        Material material = veinInfo.state.getMaterial();
        String regKey = block.getRegistryName().toString();
        String skill = XP.getSkill( veinInfo.state );

        int yLimit = 1;

        if( JsonConfig.data.get( JType.BLOCK_SPECIFIC ).containsKey( regKey ) )
        {
            if( JsonConfig.data.get( JType.BLOCK_SPECIFIC ).get( regKey ).containsKey( "growsUpwards" ) )
                yLimit = 0;
        }

        while( ( isCreative || veinLeft * 10 > veinCost * vein.size() || ( FConfig.veinWoodTopToBottom && !isLooped && skill.equals( Skill.WOODCUTTING.toString()  ) ) ) && vein.size() <= veinMaxBlocks )
        {
            for( BlockPos curPos : curLayer )
            {
                if( curPos.getDistance( originPos.getX(), originPos.getY(), originPos.getZ() ) <= veinMaxDistance )
                {
                    for( int i = yLimit; i >= -yLimit; i-- )
                    {
                        for( int j = 1; j >= -1; j-- )
                        {
                            for( int k = 1; k >= -1; k-- )
                            {
                                curPos2 = curPos.up(i).north(j).east(k);
                                if( !vein.contains( curPos2 ) && veinInfo.world.getBlockState( curPos2 ).getBlock().equals( block ) )
                                {
                                    vein.add( curPos2 );
                                    outVein.add( curPos2 );
                                    nextLayer.add( curPos2 );

                                    if( curPos2.getY() > highestPos.getY() )
                                        highestPos = new BlockPos( curPos2 );
                                }
                            }
                        }
                    }
                }
            }

            if( nextLayer.size() == 0 )
                break;

            curLayer = nextLayer;
            nextLayer = new ArrayList<>();
        }

        if( !isLooped )
        {
            if( ( FConfig.veinWoodTopToBottom && material.equals( Material.WOOD ) ) /* || block.equals( Blocks.SAND ) || block.equals( Blocks.GRAVEL ) */ )
                veinInfo.pos = highestPos;
            return getVeinShape( veinInfo, veinLeft, veinCost, isCreative, true );
        }

        return outVein;
    }

    public static double getVeinCost( IBlockState state, BlockPos pos, EntityPlayer player )
    {
        String skill = XP.getSkill( state );
        double cost;
//        double startHardness = state.getBlockHardness( player.world, pos );
        double hardness = state.getBlockHardness( player.world, pos );
        double level = Skill.getLevel( skill, player );

        if( hardness < minVeinHardness )
            hardness = minVeinHardness;

//        if( startHardness == 0 )
//            hardness = 0;

        switch( skill )
        {
            case "mining":
                cost = hardness / ( level / levelsPerHardnessMining );
                break;

            case "woodcutting":
                cost = hardness / ( level / levelsPerHardnessWoodcutting );
                break;

            case "excavation":
                cost = hardness / ( level / levelsPerHardnessExcavation );
                break;

            case "farming":
                cost = hardness / ( level / levelsPerHardnessFarming );
                break;

            case "crafting":
                level = Skill.getLevel( Skill.CRAFTING.toString(), player );
                cost = hardness / ( level / levelsPerHardnessCrafting );
                break;

            default:
                cost = hardness;
                break;
        }

        if( cost < minVeinCost )
            cost = minVeinCost;

        return cost;
    }

    public static void updateVein( EntityPlayer player, double gap )
    {
        Map<String, Double> abilitiesMap = FConfig.getAbilitiesMap( player );

        if( !abilitiesMap.containsKey( "veinLeft" ) )
            abilitiesMap.put( "veinLeft", maxVeinCharge );

        double veinLeft = abilitiesMap.get( "veinLeft" );
        if( veinLeft < 0 )
            veinLeft = 0D;

        if( !activeVein.containsKey( player ) )
            veinLeft += Math.min( gap, 2 );

        if( veinLeft > maxVeinCharge )
            veinLeft = maxVeinCharge;

        abilitiesMap.put( "veinLeft", veinLeft );

        NetworkHandler.sendToPlayer( new MessageUpdatePlayerNBT(NBTHelper.mapStringToNbt( abilitiesMap ), 1 ), (EntityPlayerMP) player );
    }
}
