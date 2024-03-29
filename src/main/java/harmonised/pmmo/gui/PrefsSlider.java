package harmonised.pmmo.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.fml.client.gui.widget.Slider;

import java.util.function.Consumer;

public class PrefsSlider extends Slider
{
    private Consumer<PrefsSlider> guiResponder;
    private final boolean isSwitch;
    private static double lastValue;
    public String preference;

    public PrefsSlider(int xPos, int yPos, int width, int height, String preference, ITextComponent prefix, ITextComponent suf, double minVal, double maxVal, double currentVal, boolean showDec, boolean drawStr, boolean isSwitch, IPressable handler)
    {
        super(xPos, yPos, width, height, prefix, suf, minVal, maxVal, currentVal, showDec, drawStr, handler);
        this.preference = preference;
        this.isSwitch = isSwitch;
    }

    @Override
    public void updateSlider()
    {
        if(isSwitch)
        {
            this.sliderValue = this.sliderValue < 0.5 ? 0 : 1;
            if(drawString)
                setMessage(new StringTextComponent(this.sliderValue == 1 ? "On" : "Off"));
        }
        else
        {
            if (this.sliderValue < 0.0F)
                this.sliderValue = 0.0F;

            if (this.sliderValue > 1.0F)
                this.sliderValue = 1.0F;

            String val;

            if (showDecimal)
            {
                val = Double.toString(sliderValue * (maxValue - minValue) + minValue);

                if (val.substring(val.indexOf(".") + 1).length() > precision)
                {
                    val = val.substring(0, val.indexOf(".") + precision + 1);

                    if (val.endsWith("."))
                    {
                        val = val.substring(0, val.indexOf(".") + precision);
                    }
                }
                else
                {
                    while (val.substring(val.indexOf(".") + 1).length() < precision)
                    {
                        val = val + "0";
                    }
                }
            }
            else
            {
                val = Integer.toString((int)Math.round(sliderValue * (maxValue - minValue) + minValue));
            }

            if(drawString)
                setMessage(new StringTextComponent(dispString.getString() + val + suffix.getString()));
        }

        if (parent != null)
        {
            parent.onChangeSliderValue(this);
        }

        if (this.guiResponder != null)
            this.guiResponder.accept(this);
    }

    public void setResponder(Consumer<PrefsSlider> rssponderIn)
    {
        this.guiResponder = rssponderIn;
    }
}
