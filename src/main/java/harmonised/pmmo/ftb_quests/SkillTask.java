package harmonised.pmmo.ftb_quests;

import dev.ftb.mods.ftblibrary.config.ConfigGroup;
import dev.ftb.mods.ftblibrary.config.NameMap;
import dev.ftb.mods.ftbquests.quest.Quest;
import dev.ftb.mods.ftbquests.quest.TeamData;
import dev.ftb.mods.ftbquests.quest.task.*;
import harmonised.pmmo.skills.Skill;
import harmonised.pmmo.util.XP;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.stats.Stats;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.storage.PlayerData;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;


public class SkillTask extends Task
{
    public static TaskType SKILL = FTBQHandler.SKILL;
    public String skill;
    public double requiredLevel;

    public SkillTask(Quest quest)
    {
        super(quest);
        skill = Skill.MINING.toString();
        requiredLevel = 1;
    }

    @Override
    public TaskType getType()
    {
        return SKILL;
    }

    @Override
    public void writeData(CompoundNBT nbt)
    {
        super.writeData(nbt);
        nbt.putString("skill", skill);
        nbt.putDouble("requiredLevel", requiredLevel);
    }

    @Override
    public void readData(CompoundNBT nbt)
    {
        super.readData(nbt);
        skill = nbt.getString("skill");
        requiredLevel = nbt.getDouble("requiredLevel");
    }

    @Override
    public void writeNetData(PacketBuffer buffer)
    {
        super.writeNetData(buffer);
        buffer.writeString(skill, Short.MAX_VALUE);
        buffer.writeDouble(requiredLevel);
    }

    @Override
    public void readNetData(PacketBuffer buffer)
    {
        super.readNetData(buffer);
        skill = buffer.readString(Short.MAX_VALUE);
        requiredLevel = buffer.readDouble();
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void getConfig(ConfigGroup config)
    {
        super.getConfig(config);
        config.addEnum("skill", skill, input -> skill = (String) input, NameMap.of(Skill.MINING.toString(), Skill.getSkills().keySet().toArray()).create());
        config.addDouble("requiredLevel", requiredLevel, input -> requiredLevel = input, requiredLevel, 1, XP.getMaxLevel());
    }

    @Override
    public IFormattableTextComponent getAltTitle()
    {
//        return I18n.format("ftbquests.task.ftbquests.skill") + ": " + TextFormatting.DARK_GREEN + skill;
        return new TranslationTextComponent(skill).setStyle(XP.textStyle.get("dark_green"));
    }

    @Override
    public int autoSubmitOnPlayerTick()
    {
        return 20;
    }

    @Override
    public void submitTask(TeamData teamData, ServerPlayerEntity player, ItemStack craftedItem)
    {
        if(teamData.isCompleted(this))
            return;
        teamData.setProgress(this, Skill.getLevel(this.skill, player));
    }

    @Override
    public long getMaxProgress()
    {
        return (long) requiredLevel;
    }
}
