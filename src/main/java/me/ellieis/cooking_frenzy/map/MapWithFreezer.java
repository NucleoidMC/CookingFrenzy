package me.ellieis.cooking_frenzy.map;

import xyz.nucleoid.map_templates.TemplateRegion;

import java.util.List;

public interface MapWithFreezer {
    TemplateRegion getFoodDropper();
    TemplateRegion getSnowballContainer();
    List<TemplateRegion> getFreezerPlates();
    List<TemplateRegion> getMeatProviders();
    TemplateRegion getFreezerArea();
    TemplateRegion getFreezerDoor();

}
