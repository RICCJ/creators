package creators.xvx;

import arc.Core;
import arc.Events;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.type.StatusEffect;

public class WorldDifficulty {
    public DifficultyConfigure ������ = new DifficultyConfigure();
    public StatusEffect ����ϵͳ;
    public void set() {
        if(����ϵͳ == null){
            ����ϵͳ = new StatusEffect("difficulty"){{
                localizedName = "����״̬";
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
            ����Ĭ���Ѷ�(MapName);
            �����Ѷ�(MapName);

            Groups.unit.each(this::UnitDraw);
        });
    }

    public void ����Ĭ���Ѷ�(String MapName){
        var keyA = Vars.state.rules.waveTimer;
        if(keyA) {
            var key = Vars.state.rules.waveSpacing;
            setWorldMap(MapName, "����", key);
        }

        var keyB = Vars.state.rules.waveTeam;

        var key1 = Vars.state.rules.teams.get(keyB).unitDamageMultiplier;
        //var key2 = Vars.state.rules.teams.get(keyB).blockHealthMultiplier;
        //var key3 = Vars.state.rules.teams.get(keyB).blockDamageMultiplier;

        setWorldMap(MapName, "��ͼ���˵�λ����", key1);
        //setWorldMap(MapName, "��ͼ���˽���Ѫ��", key2);
        //setWorldMap(MapName, "��ͼ���˽�������", key3);
    }

    public void �����Ѷ�(String MapName){
        var keyA = Vars.state.rules.waveTimer;
        if(keyA) {
            var key = getWorldMap(MapName, "����");
            Vars.state.rules.waveSpacing = key * ������.ʱ�䱶��(Core.settings.getInt("��Ϸ�Ѷ�"));
        }

        var keyB = Vars.state.rules.waveTeam;

        var key1 = getWorldMap(MapName, "��ͼ���˵�λ����");
        //var key2 = getWorldMap(MapName, "��ͼ���˽���Ѫ��");
        //var key3 = getWorldMap(MapName, "��ͼ���˽�������");

        Vars.state.rules.teams.get(keyB).unitDamageMultiplier = key1 * ������.��λ��������(Core.settings.getInt("��Ϸ�Ѷ�"));
        //Vars.state.rules.teams.get(keyB).blockHealthMultiplier = key2 * ������.����Ѫ������(Core.settings.getInt("��ͼ���˽���Ѫ��"));
        //Vars.state.rules.teams.get(keyB).blockDamageMultiplier = key3 * ������.������������(Core.settings.getInt("��ͼ���˽�������"));
    }

    public void UnitDraw(Unit unit){
        if(unit.team == Vars.state.rules.waveTeam) {
            ����ϵͳ.healthMultiplier = ������.��λѪ������(Core.settings.getInt("��Ϸ�Ѷ�"));
            ����ϵͳ.reloadMultiplier = ������.��λ���ٱ���(Core.settings.getInt("��Ϸ�Ѷ�"));
            ����ϵͳ.speedMultiplier = ������.��λ�ƶ�����(Core.settings.getInt("��Ϸ�Ѷ�"));
            unit.apply(����ϵͳ, 100f);
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