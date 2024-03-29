package harmonised.pmmo.commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import harmonised.pmmo.config.Config;
import harmonised.pmmo.events.FishedHandler;
import harmonised.pmmo.pmmo_saved_data.PmmoSavedData;
import harmonised.pmmo.skills.Skill;
import harmonised.pmmo.util.XP;
import harmonised.pmmo.util.DP;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;

public class CheckStatCommand
{
    public static int execute(CommandContext<CommandSource> context) throws CommandException
    {
        PlayerEntity sender = (PlayerEntity) context.getSource().getEntity();
        String[] args = context.getInput().split(" ");
        String skill = args[3].toLowerCase();

        try
        {
            double level = 1;

            ServerPlayerEntity target = EntityArgument.getPlayer(context, "player name");
            if(args[3].toLowerCase().equals("power"))
            {
                level = XP.getPowerLevel(target.getUniqueID());
                sender.sendStatusMessage(new TranslationTextComponent("pmmo.playerLevelDisplay", target.getDisplayName().getString(), (level % 1 == 0 ? (int) Math.floor(level) : DP.dp(level)), new TranslationTextComponent("pmmo.power").setStyle(XP.textStyle.get("cyan"))), false);
            }
            else
            {
                level = Skill.getLevelDecimal(skill, target.getUniqueID());
                sender.sendStatusMessage(new TranslationTextComponent("pmmo.playerLevelDisplay", target.getDisplayName().getString(), (level % 1 == 0 ? (int) Math.floor(level) : DP.dp(level)), new TranslationTextComponent("pmmo." + skill).setStyle(Skill.getSkillStyle(skill))), false);
            }

            //EXTRA INFO
            switch(skill)
            {
                case "fishing":
                    sender.sendStatusMessage(new TranslationTextComponent("pmmo.fishPoolChance", DP.dpSoft(FishedHandler.getFishPoolChance(sender)) ).setStyle(Skill.getSkillStyle(skill)), false);
                    break;
            }
        }
        catch(CommandSyntaxException e)
        {
            sender.sendStatusMessage( new TranslationTextComponent("pmmo.invalidPlayer", args[2]).setStyle(XP.textStyle.get("red")), false);
        }

        return 1;
    }
}
