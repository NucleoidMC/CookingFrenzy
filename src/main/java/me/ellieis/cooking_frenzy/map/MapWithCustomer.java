package me.ellieis.cooking_frenzy.map;

import xyz.nucleoid.map_templates.TemplateRegion;

import java.util.List;

public interface MapWithCustomer {
    List<TemplateRegion> getCustomerSpawns();
    List<TemplateRegion> getCustomerNodes();
    List<TemplateRegion> getSeats();
    TemplateRegion getCustomerLights();
}
