package creators.xvx;

import arc.Core;
import arc.Events;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.type.StatusEffect;

public class WorldDifficulty {
    public DifficultyConfigure 配置项 = new DifficultyConfigure();
    public StatusEffect 作弊系统;
    public void set() {
        if(作弊系统 == null){
            作弊系统 = new StatusEffect("difficulty"){{
                localizedName = "作弊状态";
                fullIcon = Core.atlas.find("creators-difficulty");
                uiIcon = Core.atlas.find("creators-difficulty");
            }
                @Override
                public void loadIcon(){
                    fullIcon = Core.atlas.find("creators-difficulty");
                    uiIcon = Core.atlas.find("creators-difficulty");
                }
            };
        }

        Events.run(EventType.Trigger.update, () -> {
            var MapName = Vars.state.map.name();
            保存默认难度(MapName);
            设置难度(MapName);

            Groups.unit.each(this::UnitDraw);
        });
    }

    public void 保存默认难度(String MapName){
        var keyA = Vars.state.rules.waveTimer;
        if(keyA) {
            var key = Vars.state.rules.waveSpacing;
            setWorldMap(MapName, "波次", key);
        }

        var keyB = Vars.state.rules.waveTeam;

        var key1 = Vars.state.rules.teams.get(keyB).unitDamageMultiplier;
        //var key2 = Vars.state.rules.teams.get(keyB).blockHealthMultiplier;
        //var key3 = Vars.state.rules.teams.get(keyB).blockDamageMultiplier;

        setWorldMap(MapName, "地图敌人单位攻击", key1);
        //setWorldMap(MapName, "地图敌人建筑血量", key2);
        //setWorldMap(MapName, "地图敌人建筑攻击", key3);
    }

    public void 设置难度(String MapName){
        var keyA = Vars.state.rules.waveTimer;
        if(keyA) {
            var key = getWorldMap(MapName, "波次");
            Vars.state.rules.waveSpacing = key * 配置项.时间倍率(Core.settings.getInt("游戏难度"));
        }

        var keyB = Vars.state.rules.waveTeam;

        var key1 = getWorldMap(MapName, "地图敌人单位攻击");
        //var key2 = getWorldMap(MapName, "地图敌人建筑血量");
        //var key3 = getWorldMap(MapName, "地图敌人建筑攻击");

        Vars.state.rules.teams.get(keyB).unitDamageMultiplier = key1 * 配置项.单位攻击倍率(Core.settings.getInt("游戏难度"));
        //Vars.state.rules.teams.get(keyB).blockHealthMultiplier = key2 * 配置项.建筑血量倍率(Core.settings.getInt("地图敌人建筑血量"));
        //Vars.state.rules.teams.get(keyB).blockDamageMultiplier = key3 * 配置项.建筑攻击倍率(Core.settings.getInt("地图敌人建筑攻击"));
    }

    public void UnitDraw(Unit unit){
        if(unit.team == Vars.state.rules.waveTeam) {
            作弊系统.healthMultiplier = 配置项.单位血量倍率(Core.settings.getInt("游戏难度"));
            作弊系统.reloadMultiplier = 配置项.单位射速倍率(Core.settings.getInt("游戏难度"));
            作弊系统.speedMultiplier = 配置项.单位移动倍率(Core.settings.getInt("游戏难度"));
            unit.apply(作弊系统, 100f);
        }
    }

    public float setWorldMap(String MapName, String State, float key){
        if(Vars.state.rules.tags.get(MapName + "-" + State + "-Difficulty") == null) {
            Vars.state.rules.tags.put(MapName + "-" + State + "-Difficulty", String.valueOf(key));
        }
        return key;
    }
    public float getWorldMap(String MapName, String State){
        if(Vars.state.rules.tags.get(MapName + "-" + State + "-Difficulty") == null){
            setWorldMap(MapName, State, 0);
        }
        return Float.parseFloat(Vars.state.rules.tags.get(MapName + "-" + State + "-Difficulty"));
    }
}