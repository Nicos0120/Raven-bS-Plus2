package keystrokesmod.module.impl.player;

import keystrokesmod.module.Module;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;

public class AutoSwap extends Module {
    public ButtonSetting sameType;
    public ButtonSetting spoofItem;
    public ButtonSetting swapToGreaterStack;
    public SliderSetting swapAt;
    public ButtonSetting legit;

    public AutoSwap() {
        super("AutoSwap", category.player);
        this.registerSetting(new DescriptionSetting("Automatically swaps blocks."));
        this.registerSetting(sameType = new ButtonSetting("Only same type", false));
        this.registerSetting(spoofItem = new ButtonSetting("Spoof item", false));
        this.registerSetting(swapToGreaterStack = new ButtonSetting("Swap to greater stack", true));
        this.registerSetting(swapAt = new SliderSetting("Swap at", " blocks", 3, 1, 7, 1));
        this.registerSetting(legit = new ButtonSetting("Legit", false));
        this.canBeEnabled = false;
    }

    public void guiUpdate() {
        this.swapAt.setVisible(!swapToGreaterStack.isToggled(), this);
    }
}