package creators.type;

import arc.func.Cons;
import arc.graphics.Color;
import arc.graphics.g2d.TextureRegion;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.scene.Group;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.Button.ButtonStyle;
import arc.scene.ui.ButtonGroup;
import arc.scene.ui.ImageButton;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectSet;
import arc.struct.OrderedSet;
import arc.struct.Seq;
import arc.util.Align;
import arc.util.Strings;
import arc.util.Structs;
import arc.util.Time;
import arc.util.io.Reads;
import arc.util.io.Writes;
import creators.draw.CreatorsNumberValue;
import mindustry.ctype.ContentType;
import mindustry.ctype.UnlockableContent;
import mindustry.gen.Building;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.type.Item;
import mindustry.type.ItemStack;
import mindustry.type.Liquid;
import mindustry.type.LiquidStack;
import mindustry.ui.*;
import mindustry.ui.fragments.BlockInventoryFragment;
import mindustry.world.blocks.production.GenericCrafter;
import mindustry.world.consumers.ConsumePower;
import mindustry.world.meta.BlockStatus;
import mindustry.world.meta.Stat;
import mindustry.world.meta.StatUnit;
import mindustry.world.modules.ItemModule;
import mindustry.world.modules.LiquidModule;

import static arc.Core.*;
import static creators.draw.CreatorsStyles.clearToggleTransi;
import static mindustry.Vars.*;

public class CreatorsMultiCrafter extends GenericCrafter {
    public int RecipeShowIS = 0;
    public Color TableColor = Color.yellow;
    public boolean[] ItemLiquid = null;

    public final CreatorsRecipe[] recs;
    public final ObjectSet<Item> inputItemSet = new ObjectSet<>(), outputItemSet = new ObjectSet<>();;
    public final ObjectSet<Liquid> inputLiquidSet = new ObjectSet<>(), outputLiquidSet = new ObjectSet<>(),
    liquidSet = new ObjectSet<>();
    public boolean dumpToggle, isSmelter;
    private final Seq<CreatorsRecipe.InputContents> needUnlocked = new Seq<>();

    private final MultiCrafterBlockInventoryFragment invFrag = new MultiCrafterBlockInventoryFragment();

    private boolean powerBarI, powerBarO, hasOutputItem;
    private int index = 0;
    private ButtonStyle infoStyle;

    public CreatorsMultiCrafter(String name, CreatorsRecipe[] recs){
        super(name);
        this.recs = recs;
        configurable = true;
        hasItems = true;
        hasLiquids = true;
        hasPower = false;
        saveConfig = true;
        config(Integer.class, (CreatorsMultiCrafterBuild tile, Integer value) -> {
            if(tile.toggle >= 0) tile.progressArr[tile.toggle] = tile.progress;
            if(value == -1){
                tile.condValid = false;
                tile.cond = false;
            }
            if(dumpToggle){
                tile.toOutputItemSet.clear();
                tile.toOutputLiquidSet.clear();
                if(value > -1){
                    ItemStack[] oItems = recs[value].output.items;
                    LiquidStack[] oLiquids = recs[value].output.liquids;
                    for(int i = 0, len = oItems.length; i < len; i++){
                        Item item = oItems[i].item;
                        if(tile.items.has(item)) tile.toOutputItemSet.add(item);
                    }
                    for(int i = 0, len = oLiquids.length; i < len; i++){
                        Liquid liquid = oLiquids[i].liquid;
                        if(tile.liquids.get(liquid) > 0.001f) tile.toOutputLiquidSet.add(liquid);
                    }
                }
            }
            tile.progress = 0;
            tile.toggle = value;
        });

        this.buildType = CreatorsMultiCrafterBuild::new;
    }

    public CreatorsMultiCrafter(String name, int recLen){
        this(name, new CreatorsRecipe[recLen]);

        this.buildType = CreatorsMultiCrafterBuild::new;
    }

    public void addRecipe(CreatorsRecipe.InputContents input, CreatorsRecipe.OutputContents output, float craftTime, boolean needUnlocked){
        recs[index++] = new CreatorsRecipe(input, output, craftTime);
        if(needUnlocked) this.needUnlocked.add(input);
    }

    public void addRecipe(CreatorsRecipe.InputContents input, CreatorsRecipe.OutputContents output, float craftTime){
        addRecipe(input, output, craftTime, false);
    }

    @Override
    public void getDependencies(Cons<UnlockableContent> cons){
        for(ItemStack stack : requirements) cons.get(stack.item);
        needUnlocked.each(input -> {
            for(ItemStack stack : input.items) cons.get(stack.item);
            for(LiquidStack stack : input.liquids) cons.get(stack.liquid);
        });
    }

    @Override
    public void init(){
        for(int i = 0; i < recs.length; i++){
            CreatorsRecipe.InputContents input = recs[i].input;
            CreatorsRecipe.OutputContents output = recs[i].output;

            if(input.power > 0f) powerBarI = true;
            if(output.power > 0f) powerBarO = true;
            if(input.items.length > 0){
                for(int j = 0, len = input.items.length; j < len; j++) inputItemSet.add(input.items[j].item);
            }
            if(input.liquids.length > 0){
                for(int j = 0, len = input.liquids.length; j < len; j++){
                    liquidSet.add(input.liquids[j].liquid);
                    inputLiquidSet.add(input.liquids[j].liquid);
                }
            }
            if(output.items.length > 0){
                hasOutputItem = true;
                for(int j = 0, len = output.items.length; j < len; j++) outputItemSet.add(output.items[j].item);
            }
            if(output.liquids.length > 0){
                for(int j = 0, len = output.liquids.length; j < len; j++){
                    liquidSet.add(output.liquids[j].liquid);
                    outputLiquidSet.add(output.liquids[j].liquid);
                }
            }
        }
        hasPower = powerBarI || powerBarO;
        if(powerBarI) consPower = new MultiCrafterConsumePower();
        consumesPower = powerBarI;
        outputsPower = powerBarO;

        super.init();
        if(!outputLiquidSet.isEmpty()) outputsLiquid = true;
        timers++;
        if(!headless) infoStyle = scene.getStyle(ButtonStyle.class);
    }

    @Override
    public void setStats() {
        super.setStats();

        stats.remove(Stat.powerUse);
        stats.remove(Stat.productionTime);

        stats.add(Stat.input, table -> {
            table.row();
            int recLen = recs.length;
            for (int i = 0; i < recLen; i++) {
                CreatorsRecipe rec = recs[i];
                ItemStack[] inputItems = rec.input.items;
                ItemStack[] outputItems = rec.output.items;
                LiquidStack[] inputLiquids = rec.input.liquids;
                LiquidStack[] outputLiquids = rec.output.liquids;
                float inputPower = rec.input.power;
                float outputPower = rec.output.power;
                int ii = i;
                table.table(infoStyle.up, part -> {
                    part.add("[accent]" + Stat.input.localized()).expandX().left().row();
                    part.table(row -> {
                        for (int l = 0, len = inputItems.length; l < len; l++)
                            row.add(new ItemDisplay(inputItems[l].item, inputItems[l].amount, true)).padRight(5f);
                    }).left().row();
                    part.table(row -> {
                        for (int l = 0, len = inputLiquids.length; l < len; l++)
                            row.add(new LiquidDisplay(inputLiquids[l].liquid, inputLiquids[l].amount, false));
                    }).left().row();
                    if (inputPower > 0f) {
                        part.table(row -> {
                            row.add("[lightgray]" + Stat.powerUse.localized() + ":[]").padRight(4f);
                            (new CreatorsNumberValue(inputPower * 60f, StatUnit.powerSecond)).display(row);
                        }).left().row();
                    }
                    part.add("[accent]" + Stat.output.localized()).left().row();
                    part.table(row -> {
                        for (int jj = 0, len = outputItems.length; jj < len; jj++)
                            row.add(new ItemDisplay(outputItems[jj].item, outputItems[jj].amount, true)).padRight(5f);
                    }).left().row();
                    part.table(row -> {
                        for (int jj = 0, len = outputLiquids.length; jj < len; jj++)
                            row.add(new LiquidDisplay(outputLiquids[jj].liquid, outputLiquids[jj].amount, false));
                    }).left().row();
                    if (outputPower > 0f) {
                        part.table(row -> {
                            row.add("[lightgray]" + Stat.basePowerGeneration.localized() + ":[]").padRight(4f);
                            (new CreatorsNumberValue(outputPower * 60f, StatUnit.powerSecond)).display(row);
                        }).left().row();
                    }
                    part.table(row -> {
                        row.add("[lightgray]" + Stat.productionTime.localized() + ":[]").padRight(4f);
                        (new CreatorsNumberValue(rec.craftTime / 60f, StatUnit.seconds)).display(row);
                    }).left().row();
                    multiCrafterDisplay(part, ii);
                }).color(TableColor).left().growX();
                table.add().size(18f).row();
            }
        });
    }

    @Override
    public void setBars(){
        super.setBars();
        removeBar("liquid");
        removeBar("items");
        if(!powerBarI && hasPower) removeBar("power");
        if(powerBarO) addBar("poweroutput",
        (CreatorsMultiCrafterBuild entity) -> new Bar(
        () -> bundle.format("bar.poweroutput",
        Strings.fixed(entity.getPowerProduction() * 60 * entity.timeScale(), 1)),
        () -> Pal.powerBar, () -> entity.productionEfficiency));
        if(!liquidSet.isEmpty()){
            liquidSet.each(k -> {
                addBar(k.localizedName, entity -> new Bar(() -> k.localizedName, k::barColor,
                () -> entity.liquids.get(k) / liquidCapacity));
            });
        }
    }

    @Override
    public boolean outputsItems(){
        return hasOutputItem;
    }

    public void multiCrafterDisplay(Table part, int i){

    }

    public class CreatorsMultiCrafterBuild extends GenericCrafterBuild{
        protected int toggle = 0, dumpItemEntry = 0, itemHas = 0;
        protected float[] progressArr = new float[recs.length];
        protected boolean cond = false, condValid = false;
        public float productionEfficiency = 0f;
        public final OrderedSet<Item> toOutputItemSet = new OrderedSet<>();
        public final OrderedSet<Liquid> toOutputLiquidSet = new OrderedSet<>();

        public int getToggle(){
            return toggle;
        }

        public boolean getCondValid(){
            return condValid;
        }

        public boolean getCond(){
            return cond;
        }

        @Override
        public boolean acceptItem(Building source, Item item){
            if(items.get(item) >= getMaximumAccepted(item)) return false;
            return inputItemSet.contains(item);
        }

        @Override
        public boolean acceptLiquid(Building source, Liquid liquid){
            return inputLiquidSet.contains(liquid);
        }

        @Override
        public void displayConsumption(Table table){
            int recLen = recs.length;
            if(recLen <= 0) return;
            int z = 0, y = 0, x = 0;

            table.left();
            for(int i = 0; i < recLen; i++){
                ItemStack[] itemStacks = recs[i].input.items;
                LiquidStack[] liquidStacks = recs[i].input.liquids;
                for(int j = 0, len = itemStacks.length; j < len; j++){
                    ItemStack stack = itemStacks[j];
                    table.add(new ReqImage(new ItemImage(stack.item.uiIcon, stack.amount), () -> items != null && items.has(stack.item, stack.amount))).size(8 * 4);//.padRight(8);
                }
                z += itemStacks.length;
                for(int j = 0, len = liquidStacks.length; j < len; j++){
                    LiquidStack stack = liquidStacks[j];
                    table.add(new ReqImage(stack.liquid.uiIcon, () -> liquids != null && liquids.get(stack.liquid) > stack.amount)).size(8 * 4);
                }
                z += liquidStacks.length;
                if(z == 0){
                    table.image(Icon.cancel).size(8f * 4f);
                    x += 1;
                }
                if(i < recLen - 1){
                    CreatorsRecipe.InputContents next = recs[i + 1].input;
                    y += next.items.length + next.liquids.length;
                    x += z;
                    if((x + y <= 8 && y != 0) || (x + y <= 7 && y == 0)){
                        table.image(Icon.pause).size(8f * 4f);
                        x += 1;
                    }else{
                        table.row();
                        x = 0;
                    }
                }
                y = 0;
                z = 0;
            }
        }

        @Override
        public float getPowerProduction(){
            if(toggle < 0 || recs.length <= 0) return 0;
            float oPower = recs[toggle].output.power;
            if(oPower > 0 && cond){
                if(recs[toggle].input.power > 0){
                    productionEfficiency = efficiency();
                    return oPower * efficiency();
                }else{
                    productionEfficiency = 1;
                    return oPower;
                }
            }
            productionEfficiency = 0;
            return 0;
        }

        @Override
        public float getProgressIncrease(float baseTime){
            if(toggle < 0) return 0f;
            else if(recs[toggle].input.power > 0) return super.getProgressIncrease(baseTime);
            else return 1f / baseTime * delta();
        }

        protected boolean checkInput(){
            if(toggle < 0) return false;
            ItemStack[] itemStacks = recs[toggle].input.items;
            LiquidStack[] liquidStacks = recs[toggle].input.liquids;
            if(!items.has(itemStacks)) return true;
            for(int i = 0, len = liquidStacks.length; i < len; i++){
                if(liquids.get(liquidStacks[i].liquid) < liquidStacks[i].amount) return true;
            }
            return false;
        }

        protected boolean checkOutput(){
            if(toggle < 0) return false;
            ItemStack[] itemStacks = recs[toggle].output.items;
            LiquidStack[] liquidStacks = recs[toggle].output.liquids;
            for(int i = 0, len = itemStacks.length; i < len; i++){
                if(items.get(itemStacks[i].item) + itemStacks[i].amount > getMaximumAccepted(itemStacks[i].item)) return true;
            }
            for(int i = 0, len = liquidStacks.length; i < len; i++){
                if(liquids.get(liquidStacks[i].liquid) + liquidStacks[i].amount > liquidCapacity) return true;
            }
            return false;
        }

        protected boolean checkCond(){
            if(toggle < 0) return false;
            if(recs[toggle].input.power > 0 && power.status <= 0){
                condValid = false;
                cond = false;
                return false;
            }else if(checkInput()){
                condValid = false;
                cond = false;
                return false;
            }else if(checkOutput()){
                condValid = true;
                cond = false;
                return false;
            }
            condValid = true;
            cond = true;
            return true;
        }

        protected void multiCrafterCons(){
            if(toggle < 0) return;
            if(checkCond()){
                if(progressArr[toggle] != 0){
                    progress = progressArr[toggle];
                    progressArr[toggle] = 0;
                }
                progress += getProgressIncrease(recs[toggle].craftTime);
                totalProgress += delta();
                warmup = Mathf.lerpDelta(warmup, 1, 0.02f);
                if(Mathf.chance(Time.delta * updateEffectChance))
                    updateEffect.at(getX() + Mathf.range(size * 4), getY() + Mathf.range(size * 4));
            }else warmup = Mathf.lerp(warmup, 0, 0.02f);
        }

        protected void multiCrafterProd(){
            if(toggle < 0) return;
            ItemStack[] inputItems = recs[toggle].input.items;
            LiquidStack[] inputLiquids = recs[toggle].input.liquids;
            ItemStack[] outputItems = recs[toggle].output.items;
            LiquidStack[] outputLiquids = recs[toggle].output.liquids;

            for(int i = 0, len = inputItems.length; i < len; i++) items.remove(inputItems[i]);
            for(int i = 0, len = inputLiquids.length; i < len; i++) liquids.remove(inputLiquids[i].liquid, inputLiquids[i].amount);
            for(int i = 0, len = outputItems.length; i < len; i++){
                int amount = outputItems[i].amount;
                Item temp = outputItems[i].item;
                for(int j = 0; j < amount; j++) offload(temp);
            }
            for(int i = 0, len = outputLiquids.length; i < len; i++) liquids.add(outputLiquids[i].liquid, outputLiquids[i].amount);
            craftEffect.at(x, y);
            progress = 0;
        }

        @Override
        public void updateTile(){
            if(recs.length < 0) return;
            if(timer.get(1, 6)){
                itemHas = 0;
                items.each((item, amount) -> itemHas++);
            }
            //if(!headless && control.input.frag.config.getSelectedTile() != this) invFrag.hide();
            multiCrafterUpdate();
            if(toggle >= 0){
                multiCrafterCons();
                if(progress >= 1) multiCrafterProd();
            }
            if(dumpToggle && toggle < 0) return;
            Seq<Item> que = toOutputItemSet.orderedItems();
            int len = que.size, i = 0;
            if(timer.get(dumpTime) && len > 0){
                for(; i < len; i++){
                    dump(que.get((i + dumpItemEntry) % len));
                    break;
                }
                if(i != len) dumpItemEntry = (i + dumpItemEntry) % len;
            }
            Seq<Liquid> queL = toOutputLiquidSet.orderedItems();
            len = queL.size;
            if(len > 0){
                for(i = 0; i < len; i++){
                    dumpLiquid(queL.get(i));
                    break;
                }
            }
        }

        public void multiCrafterUpdate(){

        }

        @Override
        public void draw() {
            if (isSmelter) {
                super.draw();
            } else {
                drawer.draw(this);
            }
        }

        @Override
        public void drawLight(){
            if(isSmelter) super.drawLight();
        }

        @Override
        public boolean shouldConsume(){
            return condValid && productionValid();
        }

        @Override
        public boolean productionValid(){
            return cond && enabled;
        }

        @Override
        public void updateTableAlign(Table table){
            float pos = input.mouseScreen(x, y - size * 4 - 1).y;
            Vec2 relative = input.mouseScreen(x, y + size * 4);

            table.setPosition(relative.x, Math.min(pos, (float)(relative.y - Math.ceil((float)itemHas / 3f) * 48f - 4f)), Align.top);
            if(!invFrag.isShown() && control.input.config.getSelected() == this && items.any()) invFrag.showFor(this);
        }

        @Override
        public void buildConfiguration(Table table){
            if(recs.length <= 0) return;
            if(!invFrag.isBuilt()) invFrag.build(table.parent);
            if(invFrag.isShown()){
                invFrag.hide();
                control.input.config.hideConfig();
                return;
            }
            ButtonGroup<ImageButton> group = new ButtonGroup<ImageButton>();
            group.setMinCheckCount(0);
            group.setMaxCheckCount(1);
            int recLen = recs.length;

            int A = 0;
            for(int i = 0; i < recLen; i++) {
                int ii = i;
                CreatorsRecipe.OutputContents output = recs[i].output;
                ImageButton button = table.button(Tex.whiteui, clearToggleTransi, 40, () -> {
                }).group(group).get();

                button.clicked(() -> configure(button.isChecked() ? ii : -1));

                TextureRegion icon;
                if(ItemLiquid == null) {
                    icon = output.items.length > 0 ? output.items[0].item.uiIcon : output.liquids.length > 0 ? output.liquids[0].liquid.uiIcon : region;
                }else{
                    icon = ItemLiquid[ii] ? output.items[0].item.uiIcon : output.liquids[0].liquid.uiIcon;
                }

                button.getStyle().imageUp = region == icon ? output.power > 0 ? Icon.power : Icon.cancel : new TextureRegionDrawable(icon);
                button.update(() -> button.setChecked(toggle == ii));
                A += 1;
                if (A == (RecipeShowIS != 0 ? RecipeShowIS : size+1)) {
                    A = 0;
                    table.row();
                }
            }
        }

        @Override
        public boolean onConfigureBuildTapped(Building other){
            return items.any() || this != other;
        }

        protected boolean decideItemSet(Item item){
            return dumpToggle ? toggle > -1 && Structs.contains(recs[toggle].output.items, i -> i.item == item) : outputItemSet.contains(item);
        }

        protected boolean decideLiquidSet(Liquid liquid){
            return dumpToggle ? toggle > -1 && Structs.contains(recs[toggle].output.liquids, i -> i.liquid == liquid) : outputLiquidSet.contains(liquid);
        }

        @Override
        public void created(){
            if(hasItems) items = new MultiCrafterItemModule();
            if(hasLiquids) liquids = new MultiCrafterLiquidModule();
        }

        @Override
        public Integer config(){
            return toggle;
        }

        @Override
        public BlockStatus status(){
            if(productionValid()) return BlockStatus.active;
            if(getCondValid()) return BlockStatus.noOutput;
            return BlockStatus.noInput;
        }

        @Override
        public void write(Writes write){
            super.write(write);
            write.s(toggle);
            Seq<Item> queItem = toOutputItemSet.orderedItems();
            short lenI = (short)queItem.size;
            write.s(lenI);
            for(short i = 0; i < lenI; i++) write.s(queItem.get(i).id);
            Seq<Liquid> queLiquid = toOutputLiquidSet.orderedItems();
            short lenL = (short)queLiquid.size;
            write.s(lenL);
            for(short i = 0; i < lenL; i++) write.s(queLiquid.get(i).id);
        }

        @Override
        public void read(Reads read, byte revision){
            super.read(read, revision);
            toggle = read.s();
            toOutputItemSet.clear();
            toOutputLiquidSet.clear();
            short lenI = read.s();
            for(short i = 0; i < lenI; i++) toOutputItemSet.add(content.getByID(ContentType.item, read.s()));
            short lenL = read.s();
            for(short i = 0; i < lenL; i++) toOutputLiquidSet.add(content.getByID(ContentType.liquid, read.s()));
        }

        class MultiCrafterItemModule extends ItemModule{
            @Override
            public Item take(){
                for(int i = 0; i < items.length; i++){
                    int index = (i + takeRotation);
                    if(index >= items.length) index -= items.length;
                    if(items[index] > 0){
                        Item ret = content.item(index);
                        items[index]--;
                        total--;
                        takeRotation = index + 1;
                        if(items[index] <= 0 && decideItemSet(ret)) toOutputItemSet.remove(ret);
                        return ret;
                    }
                }
                return null;
            }

            public void endTake(Item item){
                int var10002 = this.items[item.id]--;
                --this.total;
                this.takeRotation = item.id + 1;
                if(items[item.id] <= 0 && decideItemSet(item)) toOutputItemSet.remove(item);
            }

            @Override
            public void set(Item item, int amount){
                super.set(item, amount);
                if(decideItemSet(item)){
                    if(amount == 0) toOutputItemSet.remove(item);
                    else toOutputItemSet.add(item);
                }
            }

            @Override
            public void add(Item item, int amount){
                super.add(item, amount);
                if(decideItemSet(item)){
                    if(items[item.id] <= 0) toOutputItemSet.remove(item);
                    else toOutputItemSet.add(item);
                }
            }

            @Override
            public void remove(Item item, int amount){
                super.remove(item, amount);
                if(decideItemSet(item)){
                    if(items[item.id] <= 0) toOutputItemSet.remove(item);
                    else toOutputItemSet.add(item);
                }
            }

            @Override
            public void clear(){
                super.clear();
                toOutputItemSet.clear();
            }
        }

        public class MultiCrafterLiquidModule extends LiquidModule{
            @Override
            public void reset(Liquid liquid, float amount){
                super.reset(liquid, amount);
                if(decideLiquidSet(liquid)){
                    if(amount > 0.001f) toOutputLiquidSet.add(liquid);
                    else toOutputLiquidSet.remove(liquid);
                }
            }

            @Override
            public void clear(){
                super.clear();
                toOutputLiquidSet.clear();
            }

            @Override
            public void add(Liquid liquid, float amount){
                super.add(liquid, amount);
                if(decideLiquidSet(liquid)){
                    if(get(liquid) > 0.001f) toOutputLiquidSet.add(liquid);
                    else toOutputLiquidSet.remove(liquid);
                }
            }
        }
    }

    public class MultiCrafterConsumePower extends ConsumePower{
        @Override
        public float requestedPower(Building entity){
            if(entity instanceof CreatorsMultiCrafterBuild) {
                var MC = (CreatorsMultiCrafterBuild)entity;
                if (MC.tile().build == null) return 0;
                int i = MC.getToggle();
                if (i < 0) return 0;
                float input = recs[i].input.power;
                if (input > 0 && MC.getCond()) return input;
                return 0;
            }
            return 0;
        }
    }

    public static class MultiCrafterBlockInventoryFragment extends BlockInventoryFragment{
        private boolean built = false;
        private boolean visible = false;

        public boolean isBuilt(){
            return built;
        }

        public boolean isShown(){
            return visible;
        }

        @Override
        public void showFor(Building t){
            visible = true;
            super.showFor(t);
        }

        @Override
        public void hide(){
            visible = false;
            super.hide();
        }

        @Override
        public void build(Group parent){
            built = true;
            super.build(parent);
        }
    }
}
