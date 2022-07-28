package creators.world.block;

import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.math.geom.Intersector;
import arc.util.Time;
import mindustry.entities.units.WeaponMount;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.type.weapons.RepairBeamWeapon;

public class CreatorsRepairBeamWeapon extends RepairBeamWeapon {
    public CreatorsRepairBeamWeapon(String name){
        super(name);

        reload = 1f;
        predictTarget = false;
        autoTarget = true;
        controllable = false;
        rotate = true;
        useAmmo = false;
        mountType = HealBeamMount::new;
        recoil = 0f;
        noAttack = true;
    }

    public CreatorsRepairBeamWeapon(){
        reload = 1f;
        predictTarget = false;
        autoTarget = true;
        controllable = false;
        rotate = true;
        useAmmo = false;
        mountType = HealBeamMount::new;
        recoil = 0f;
        noAttack = true;
    }

    public void update(Unit unit, WeaponMount mount) {
        super.update(unit, mount);
        HealBeamMount heal = (HealBeamMount)mount;
        if (heal.target instanceof Building && mount.shoot) {
            Building b = (Building)heal.target;

            float range = b.block.size * 6.5F;
            Groups.bullet.intersect(b.x - range, b.y - range, range * 2.0F, range * 2.0F, (example) -> {
                if (example.team != unit.team && Intersector.isInsideHexagon(b.x, b.y, range * 2.0F, example.x(), example.y())) {
                    example.absorb();
                }
            });
        }

    }

    public void draw(Unit unit, WeaponMount mount) {
        super.draw(unit, mount);
        if (mount.target instanceof Building && mount.shoot) {
            Building b = (Building)mount.target;

            float range = b.block.size * 6.5F;
            Draw.color(Color.white, b.team.color, Mathf.absin(Time.time, 4.0F, 1.0F));
            Lines.poly(b.x, b.y, 6, range);
        }
    }
}
