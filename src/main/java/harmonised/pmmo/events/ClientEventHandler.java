package harmonised.pmmo.events;

import harmonised.pmmo.config.Requirements;
import harmonised.pmmo.gui.XPOverlayGUI;
import harmonised.pmmo.network.MessageCrawling;
import harmonised.pmmo.network.MessageDoubleTranslation;
import harmonised.pmmo.network.NetworkHandler;
import harmonised.pmmo.proxy.ClientHandler;
import harmonised.pmmo.skills.XP;
import harmonised.pmmo.util.DP;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.*;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber
public class ClientEventHandler
{
    private static boolean wasCrawling = false;

    public static void subscribeClientEvents( IEventBus eventBus )
    {
        eventBus.register( harmonised.pmmo.events.ClientEventHandler.class );
    }

    @SubscribeEvent
    public static void keyPressEvent( net.minecraftforge.client.event.InputEvent.KeyInputEvent event )
    {
        if( Minecraft.getInstance().player != null )
        {
            if( wasCrawling != ClientHandler.CRAWL_KEY.isKeyDown() )
            {
                wasCrawling = ClientHandler.CRAWL_KEY.isKeyDown();
                NetworkHandler.sendToServer( new MessageCrawling( ClientHandler.CRAWL_KEY.isKeyDown() ) );
            }
        }
    }

    @SubscribeEvent
    public static void tooltipEvent( ItemTooltipEvent event )
    {
        Item item = event.getItemStack().getItem();
        PlayerEntity player = event.getPlayer();
        List<ITextComponent> tooltip = event.getToolTip();
        Map<String, Double> wearReq = Requirements.wearReq.get( item.getRegistryName().toString() );
        Map<String, Double> toolReq = Requirements.toolReq.get( item.getRegistryName().toString() );
        Map<String, Double> weaponReq = Requirements.weaponReq.get( item.getRegistryName().toString() );
        Map<String, Double> xpValue = Requirements.xpValue.get( item.getRegistryName().toString() );
        Map<String, Double> oreInfo = Requirements.oreInfo.get( item.getRegistryName().toString() );
        Map<String, Double> logInfo = Requirements.logInfo.get( item.getRegistryName().toString() );
        Map<String, Double> plantInfo = Requirements.plantInfo.get( item.getRegistryName().toString() );
        int level, value;
        double dValue;

        if( wearReq != null && wearReq.size() > 0 )      //WEAR REQUIREMENT
        {
            if( XP.checkReq( player, item.getRegistryName(), "wear" ) )
                tooltip.add( new TranslationTextComponent( "pmmo.text.Armor" ).setStyle( new Style().setColor( TextFormatting.GREEN ) ) );
            else
                tooltip.add( new TranslationTextComponent( "pmmo.text.Armor" ).setStyle( new Style().setColor( TextFormatting.RED ) ) );


            for( String key : wearReq.keySet() )
            {
                if(XPOverlayGUI.skills.containsKey( key ))
                    level = XP.levelAtXp( XPOverlayGUI.skills.get( key ).goalXp );
                else
                    level = 1;

                value = (int) Math.floor( wearReq.get( key ) );

                if( level < value )
                    tooltip.add( new TranslationTextComponent( "pmmo.text.levelDisplay", " " + key, value ).setStyle( new Style().setColor( TextFormatting.RED ) ) );
                else
                    tooltip.add( new TranslationTextComponent( "pmmo.text.levelDisplay", " " + key, value ).setStyle( new Style().setColor( TextFormatting.GREEN ) ) );
            }
        }

        if( toolReq != null && toolReq.size() > 0 )      //TOOL REQUIREMENT
        {
            if( XP.checkReq( player, item.getRegistryName(), "tool" ) )
                tooltip.add( new TranslationTextComponent( "pmmo.text.Tool" ).setStyle( new Style().setColor( TextFormatting.GREEN ) ) );
            else
                tooltip.add( new TranslationTextComponent( "pmmo.text.Tool" ).setStyle( new Style().setColor( TextFormatting.RED ) ) );


            for( String key : toolReq.keySet() )
            {
                if(XPOverlayGUI.skills.containsKey( key ))
                    level = XP.levelAtXp( XPOverlayGUI.skills.get( key ).goalXp );
                else
                    level = 1;

                value = (int) Math.floor( toolReq.get( key ) );

                if( level < value )
                    tooltip.add( new TranslationTextComponent( "pmmo.text.levelDisplay", " " + key, value ).setStyle( new Style().setColor( TextFormatting.RED ) ) );
                else
                    tooltip.add( new TranslationTextComponent( "pmmo.text.levelDisplay", " " + key, value ).setStyle( new Style().setColor( TextFormatting.GREEN ) ) );
            }
        }

        if( weaponReq != null && weaponReq.size() > 0 )      //WEAPON REQUIREMENT
        {
            if( XP.checkReq( player, item.getRegistryName(), "weapon" ) )
                tooltip.add( new TranslationTextComponent( "pmmo.text.Weapon" ).setStyle( new Style().setColor( TextFormatting.GREEN ) ) );
            else
                tooltip.add( new TranslationTextComponent( "pmmo.text.Weapon" ).setStyle( new Style().setColor( TextFormatting.RED ) ) );


            for( String key : weaponReq.keySet() )
            {
                if(XPOverlayGUI.skills.containsKey( key ))
                    level = XP.levelAtXp( XPOverlayGUI.skills.get( key ).goalXp );
                else
                    level = 1;

                value = (int) Math.floor( weaponReq.get( key ) );

                if( level < value )
                    tooltip.add( new TranslationTextComponent( "pmmo.text.levelDisplay", " " + key, value ).setStyle( new Style().setColor( TextFormatting.RED ) ) );
                else
                    tooltip.add( new TranslationTextComponent( "pmmo.text.levelDisplay", " " + key, value ).setStyle( new Style().setColor( TextFormatting.GREEN ) ) );
            }
        }

        if( xpValue != null && xpValue.size() > 0 )      //XP VALUE
        {
            tooltip.add( new TranslationTextComponent( "pmmo.text.xpValue" ) );

            for( String key : xpValue.keySet() )
            {
                if(XPOverlayGUI.skills.containsKey( key ))
                    level = XP.levelAtXp( XPOverlayGUI.skills.get( key ).goalXp );
                else
                    level = 1;

                dValue = xpValue.get( key );

                tooltip.add( new TranslationTextComponent( "pmmo.text.levelDisplay", " " + key, DP.dp( dValue ) ) );
            }
        }

        if( oreInfo != null && oreInfo.size() > 0 )      //ORE INFO
        {
            if( oreInfo.get( "extraChance" ) != null )
            {
                if(XPOverlayGUI.skills.containsKey( "mining" ) )
                    level = XP.levelAtXp( XPOverlayGUI.skills.get( "mining" ).goalXp );
                else
                    level = 1;

                tooltip.add( new TranslationTextComponent( "pmmo.text.oreExtraChance", DP.dp( oreInfo.get( "extraChance" ) * level ) ).setStyle( new Style().setColor( TextFormatting.GREEN ) ) );
            }
        }

        if( logInfo != null && logInfo.size() > 0 )      //LOG INFO
        {
            if( logInfo.get( "extraChance" ) != null )
            {
                if(XPOverlayGUI.skills.containsKey( "woodcutting" ) )
                    level = XP.levelAtXp( XPOverlayGUI.skills.get( "woodcutting" ).goalXp );
                else
                    level = 1;

                tooltip.add( new TranslationTextComponent( "pmmo.text.logExtraChance", DP.dp( logInfo.get( "extraChance" ) * level ) ).setStyle( new Style().setColor( TextFormatting.GREEN ) ) );
            }
        }

        if( plantInfo != null && plantInfo.size() > 0 )      //PLANT INFO
        {
            if( plantInfo.get( "extraChance" ) != null )
            {
                if(XPOverlayGUI.skills.containsKey( "farming" ) )
                    level = XP.levelAtXp( XPOverlayGUI.skills.get( "farming" ).goalXp );
                else
                    level = 1;
                tooltip.add( new TranslationTextComponent( "pmmo.text.plantExtraChance", DP.dp( plantInfo.get( "extraChance" ) * level ) ).setStyle( new Style().setColor( TextFormatting.GREEN ) ) );
            }
        }
    }
}