package me.ellieis.cooking_frenzy.ui;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class ProgressBarComponent {
    public static MutableComponent create(int textWidth, float val, float minVal, float maxVal, boolean showBackground, ChatFormatting progressColor, ChatFormatting backgroundColor) {
        int progressCharacterCount = Math.round(Common.mapRange(Math.min(val, maxVal), minVal, maxVal, 0, textWidth));
        StringBuilder progressBarText = new StringBuilder();
        progressBarText.repeat("▄", Math.max(0, progressCharacterCount));
        if (!showBackground) {
            progressBarText.repeat(" ", textWidth - Math.clamp(progressCharacterCount, 0, textWidth));
        }

        MutableComponent finalComponent = (Component.literal(progressBarText.toString()).withStyle(progressColor));
        if (showBackground) {
            StringBuilder backgroundText = new StringBuilder();
            backgroundText.repeat("▄", textWidth - Math.clamp(progressCharacterCount, 0, textWidth));
            finalComponent.append((Component.literal(backgroundText.toString()).withStyle(backgroundColor)));
        }
        return finalComponent;
    }

    public static MutableComponent create(int textWidth, float val, float minVal, float maxVal, boolean showBackground, ChatFormatting progressColor) {
        return ProgressBarComponent.create(textWidth, val, minVal, maxVal, showBackground, progressColor, ChatFormatting.GRAY);
    }

    public static MutableComponent create(int textWidth, float val, float minVal, float maxVal, boolean showBackground) {
        return ProgressBarComponent.create(textWidth, val, minVal, maxVal, showBackground, ChatFormatting.GREEN, ChatFormatting.GRAY);
    }
}
