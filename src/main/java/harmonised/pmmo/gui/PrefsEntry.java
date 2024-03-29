package harmonised.pmmo.gui;

import harmonised.pmmo.util.DP;
import harmonised.pmmo.util.XP;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;

public class PrefsEntry
{
    public static FontRenderer font = Minecraft.getInstance().fontRenderer;
    public PrefsSlider slider;
    public Button button;
    public TextFieldWidget textField;
    public String preference, prefix, suffix;
    public double defaultVal;
    public final int sliderWidth = 150, height = 16;
    private final int textFieldWidth = 36;
    public final boolean isSwitch, removeIfMax;

    public PrefsEntry(String preference, String prefix, String suffix, double minVal, double maxVal, double curVal, double defaultVal, boolean showDec, boolean showStr, boolean removeIfMax, boolean isSwitch)
    {
        this.preference = preference;
        this.prefix = prefix;
        this.suffix = suffix;
        this.isSwitch = isSwitch;
        this.removeIfMax = removeIfMax;

        if(minVal == maxVal)
            maxVal = 0.00001;
        if(Double.isNaN(curVal))
            curVal = defaultVal;
        if(defaultVal > maxVal)
            defaultVal = maxVal;
        if(defaultVal < minVal)
            defaultVal = minVal;
        if(curVal > maxVal)
            curVal = maxVal;
        if(curVal < minVal)
            curVal = minVal;

        this.defaultVal = defaultVal;

        slider = new PrefsSlider(0, 0, sliderWidth, height, preference, new StringTextComponent(prefix), new StringTextComponent(suffix), minVal, maxVal, curVal, showDec, showStr, isSwitch, button ->
        {
        });

        if(!isSwitch)
        {
            textField = new TextFieldWidget(font, 0, 0, textFieldWidth, height, new TranslationTextComponent(""));
            textField.setMaxStringLength(5);
            textField.setText(slider.getMessage().getString());
        }
        button = new Button(0, 0, height + (isSwitch ? textFieldWidth : 0), height, new TranslationTextComponent(isSwitch ? "RESET" : "R"), button ->
        {
            resetValue();
        });
    }

    public void resetValue()
    {
        slider.setValue(defaultVal);
        slider.updateSlider();
        if(isSwitch)
            slider.setMessage(new StringTextComponent(slider.getValue() == 1 ? "On" : "Off"));
        else
            textField.setText(slider.getMessage().getString());
//        System.out.println(slider.getMessage().getString());
    }

    public int getWidth()
    {
        return sliderWidth + height + textFieldWidth;
    }

    public int getHeight()
    {
        return height + 11;
    }

    public int getX()
    {
        return slider.x;
    }

    public int getY()
    {
        return slider.y;
    }

    public void setX(int x)
    {
        slider.x = x;

        if(isSwitch)
            button.x = x + sliderWidth;
        else
        {
            button.x = x + sliderWidth + textFieldWidth;
            textField.x = x + sliderWidth;
        }
    }

    public void setY(int y)
    {
        slider.y = y;
        button.y = y;
        if(!isSwitch)
            textField.y = y;
    }

    public void mouseClicked(double mouseX, double mouseY, int button)
    {
        this.slider.mouseClicked(mouseX, mouseY, button);
        this.button.mouseClicked(mouseX, mouseY, button);
    }

    public void mouseReleased(double mouseX, double mouseY, int button)
    {
        this.slider.mouseReleased(mouseX, mouseY, button);
        this.button.mouseReleased(mouseX, mouseY, button);
    }

    public void mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY)
    {
        this.slider.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        this.button.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }
}
