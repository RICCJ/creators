package creators;

import arc.Core;
import arc.Events;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.TextureRegion;
import arc.scene.ui.ImageButton;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import creators.ui.CreatorsClassification;
import creators.world.block.CTBlocks;
import creators.world.block.CTUnitTypes;
import creators.world.block.CreatorsModJS;
import creators.xvx.WorldDifficulty;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.graphics.Layer;
import mindustry.mod.Mod;
import mindustry.mod.Scripts;
import mindustry.type.Planet;
import mindustry.type.UnitType;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.world.Block;
import mindustry.world.blocks.distribution.Sorter;
import mindustry.world.blocks.sandbox.ItemSource;
import mindustry.world.blocks.sandbox.LiquidSource;
import rhino.Context;
import rhino.Scriptable;
import rhino.ScriptableObject;

import java.util.Objects;

import static arc.Core.camera;
import static mindustry.Vars.*;

public class Creators extends Mod {
    public Creators(){}

    public static void setPlanet(Planet planet) {
        planet.ruleSetter = r -> {
            var B = new ObjectSet<Block>();
            for (var b : content.blocks()) {
                if (b.minfo.mod == null) {
                    B.add(b);
                }
            }
            r.bannedBlocks = B;

            var U = new ObjectSet<UnitType>();
            for (var u : content.units()) {
                if (u.minfo.mod == null) {
                    U.add(u);
                }
            }
            r.bannedUnits = U;
        };
    }

    public void loadContent(){
        CTUnitTypes.load();
        CTBlocks.load();

        new CreatorsClassification();
        Scripts scripts = Vars.mods.getScripts();
        Scriptable scope = scripts.scope;
        try {
            Object obj = Context.javaToJS(new CreatorsClassification(), scope);
            ScriptableObject.putProperty(scope, "CreatorsClassification", obj);
        } catch (Exception var5) {
            Vars.ui.showException(var5);
        }

        CreatorsModJS.DawnMods();
    }

    public final static Seq<Runnable> BlackListRun = new Seq<>();

    public Seq<String> 白名单 = new Seq<>();

    @Override
    public void init() {
        ini();

        Events.on(EventType.ClientLoadEvent.class, e -> 选择方块显示图标());

        Events.on(EventType.ClientLoadEvent.class, e -> {
            for (var a : Vars.mods.list()) {
                if(Objects.equals(a.meta.name, "creators")){
                    if(!白名单.contains(a.meta.name)) {
                        白名单.add(a.meta.name);
                    }
                }

                if(a.meta.dependencies.size != 0){
                    for(var d : a.meta.dependencies){
                        if(Objects.equals(d, "creators")){
                            if(!白名单.contains(a.meta.name)) {
                                白名单.add(a.meta.name);
                            }
                        }
                    }
                }

                if(a.meta.hidden){
                    if(!白名单.contains(a.meta.name)) {
                        白名单.add(a.meta.name);
                    }
                }
            }

            boolean yes = false;

            for (var a : Vars.mods.list()) {
                if(!白名单.contains(a.meta.name)){
                    yes = true;
                }
            }

            if (yes) {
                for (var i = 0; i < BlackListRun.size; i++) {
                    BlackListRun.get(i).run();
                }
            }
        });
    }

    public void ini() {
        Events.on(EventType.ClientLoadEvent.class, e -> {
            ui.settings.game.sliderPref("难度调整难度", 3, 1, 5, 1, i -> Core.bundle.format("Difficulty-" + i));
            ui.settings.game.sliderPref("分类器显示", 100, 0, 150, 1, i -> i + "格");
            new WorldDifficulty().set();
        });
    }

    public void 选择方块显示图标(){
        Events.run(EventType.Trigger.draw, () -> {
            if(Vars.ui != null && Core.settings.getInt("分类器显示") != 0) {
                indexer.eachBlock(null, camera.position.x, camera.position.y,  (Core.settings.getInt("分类器显示") * tilesize), b -> true, b -> {
                    if(b instanceof LiquidSource.LiquidSourceBuild) {
                        var source = (LiquidSource.LiquidSourceBuild) b;
                        if(source.config() != null) {
                            Draw.z(Layer.block + 1);
                            Draw.rect(source.config().fullIcon, b.x, b.y, 3, 3);
                        }
                    }
                    if(b instanceof ItemSource.ItemSourceBuild) {
                        var source = (ItemSource.ItemSourceBuild) b;
                        if(source.config() != null) {
                            Draw.z(Layer.block + 1);
                            Draw.rect(source.config().fullIcon, b.x, b.y, 3, 3);
                        }
                    }
                    if(b instanceof Sorter.SorterBuild) {
                        var sorter = (Sorter.SorterBuild) b;
                        if(sorter.config() != null) {
                            Draw.z(Layer.block + 1);
                            Draw.rect(sorter.config().fullIcon, b.x, b.y, 3, 3);
                        }
                    }
                });
            }
        });
    }

    public static ImageButton CreatorsIcon(String IconName, ImageButton.ImageButtonStyle imageButtonStyle, BaseDialog dialog) {
        TextureRegion A = Core.atlas.find("creators-" + IconName);

        ImageButton buttonA = new ImageButton(A, imageButtonStyle);
        buttonA.clicked(dialog::show);
        return buttonA;
    }
}
