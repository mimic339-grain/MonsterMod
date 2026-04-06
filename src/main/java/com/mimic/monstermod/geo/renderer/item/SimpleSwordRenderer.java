package com.mimic.monstermod.geo.renderer.item;


import com.mimic.monstermod.geo.model.item.SimpleSwordModel;
import com.mimic.monstermod.item.weapon.SimpleSwordItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;

public class SimpleSwordRenderer extends GeoItemRenderer<SimpleSwordItem> {
    public SimpleSwordRenderer() {super(new SimpleSwordModel());
    }
}
