package harmonised.pmmo.commands;

import com.mojang.brigadier.context.CommandContext;
import harmonised.pmmo.config.Config;
import harmonised.pmmo.network.MessageUpdatePlayerNBT;
import harmonised.pmmo.network.NetworkHandler;
import harmonised.pmmo.pmmo_saved_data.PmmoSavedData;
import harmonised.pmmo.skills.AttributeHandler;
import harmonised.pmmo.util.NBTHelper;
import harmonised.pmmo.util.XP;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.HashMap;
import java.util.Map;

public class PrefCommand
{
    public static int execute(CommandContext<CommandSource> context) throws CommandException
    {
        PlayerEntity player = (PlayerEntity) context.getSource().getEntity();
        String[] args = context.getInput().split(" ");
        Map<String, Double> prefsMap = Config.getPreferencesMap(player);
        Double value = null;
        if(args.length > 3)
        {
            value = Double.parseDouble(args[3]);
            if(value < 0)
                value = 0D;
        }


        boolean matched = false;
        String match = "ERROR";

//        for(String element : PmmoCommand.suggestPref)
//        {
//            if(args[2].toLowerCase().equals(element.toLowerCase()))
//            {
//                match = element;
//                matched = true;
//            }
//        }

        if(matched)
        {
            if(value != null)
            {
                prefsMap.put(match, value);

                NetworkHandler.sendToPlayer(new MessageUpdatePlayerNBT(NBTHelper.mapStringToNbt(prefsMap), 0), (ServerPlayerEntity) player);
                AttributeHandler.updateAll(player);

                player.sendStatusMessage(new TranslationTextComponent("pmmo.hasBeenSet", match, args[3]), false);
            }
            else if(prefsMap.containsKey(match))
                player.sendStatusMessage(new TranslationTextComponent("pmmo.hasTheValue", "" + match, "" + prefsMap.get(match)), false);
            else
                player.sendStatusMessage(new TranslationTextComponent("pmmo.hasUnsetValue", "" + match), false);
        }
        else
            player.sendStatusMessage(new TranslationTextComponent("pmmo.invalidChoice", args[2]).setStyle(XP.textStyle.get("red")), false);

        return 1;
    }
}
