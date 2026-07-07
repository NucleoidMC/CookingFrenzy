package me.ellieis.cooking_frenzy.textures;

import eu.pb4.polymer.resourcepack.extras.api.ResourcePackExtras;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.elements.GuiElementBuilderCreator;
import net.minecraft.network.chat.Component;

import java.util.function.Function;
import java.util.function.Supplier;

import static me.ellieis.cooking_frenzy.CookingFrenzy.identifier;
import static me.ellieis.cooking_frenzy.textures.UiResourceCreator.background;
import static me.ellieis.cooking_frenzy.textures.UiResourceCreator.icon16;

public class GuiTextures {
    public static final Function<Component, Component> FARMER_SHOP = background("farmer_shop");

    public static void register() {
        ResourcePackExtras.forDefault().addBridgedModelsFolder(identifier("sgui"));
        UiResourceCreator.setup();
    }

}