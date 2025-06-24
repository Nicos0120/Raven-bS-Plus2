package keystrokesmod.module.setting.impl;

import com.google.gson.JsonObject;
import keystrokesmod.module.Module;
import keystrokesmod.module.setting.Setting;

public class ButtonSetting extends Setting {
    private String name;
    private boolean isEnabled;
    public boolean isMethodButton;
    private Runnable method;
    public GroupSetting group;
    private boolean exclusive;
    private Module parentModule;

    public ButtonSetting(String name, boolean isEnabled) {
        super(name);
        this.name = name;
        this.isEnabled = isEnabled;
        this.isMethodButton = false;
        this.parentModule = null;
    }

    public ButtonSetting(String name, Module parentModule, boolean isEnabled) {
        super(name);
        this.name = name;
        this.parentModule = parentModule;
        this.isEnabled = isEnabled;
        this.isMethodButton = false;
    }

    public ButtonSetting(GroupSetting group, String name, boolean isEnabled) {
        super(name);
        this.group = group;
        this.name = name;
        this.isEnabled = isEnabled;
        this.isMethodButton = false;
        this.parentModule = null;
    }

    public ButtonSetting(GroupSetting group, String name, Module parentModule, boolean isEnabled) {
        super(name);
        this.group = group;
        this.name = name;
        this.parentModule = parentModule;
        this.isEnabled = isEnabled;
        this.isMethodButton = false;
    }

    public ButtonSetting(String name, Runnable method) {
        super(name);
        this.name = name;
        this.isEnabled = false;
        this.isMethodButton = true;
        this.method = method;
        this.parentModule = null;
    }

    public Module getParentModule() {
        return parentModule;
    }

    public void runMethod() {
        if (method != null) {
            method.run();
        }
    }

    public String getName() {
        return this.name;
    }

    public boolean isToggled() {
        return this.isEnabled;
    }

    public void toggle() {
        this.isEnabled = !this.isEnabled;
        if (exclusive && isEnabled) {
            handleExclusiveToggle();
        }
    }

    private void handleExclusiveToggle() {
        if (parentModule != null) {
            for (Setting setting : parentModule.getSettings()) {
                if (setting instanceof ButtonSetting && setting != this) {
                    ButtonSetting other = (ButtonSetting) setting;
                    if (other.exclusive) {
                        other.setToggled(false);
                    }
                }
            }
        }
    }


    public void enable() {
        this.isEnabled = true;
        if (exclusive) {
            handleExclusiveToggle();
        }
    }

    public void disable() {
        this.isEnabled = false;
    }

    public void setEnabled(boolean b) {
        this.isEnabled = b;
        if (b && exclusive) {
            handleExclusiveToggle();
        }
    }

    public void setExclusive(boolean exclusive) {
        this.exclusive = exclusive;
    }

    public void guiButtonToggled(ButtonSetting b) {
        if (b.exclusive && b.isToggled()) {

            for (Setting setting : parentModule.getSettings()) {
                if (setting instanceof ButtonSetting && setting != b) {
                    ((ButtonSetting) setting).setToggled(false);
                }
            }
        }
    }

    @Override
    public void loadProfile(JsonObject data) {
        if (data.has(getName()) && data.get(getName()).isJsonPrimitive() && !this.isMethodButton) {
            boolean booleanValue = isEnabled;
            try {
                booleanValue = data.getAsJsonPrimitive(getName()).getAsBoolean();
            }
            catch (Exception e) {}
            setEnabled(booleanValue);
        }
    }

    public void setToggled(boolean b) {
        if (this.isEnabled != b) {
            this.isEnabled = b;
            if (b && exclusive) {
                handleExclusiveToggle();
            }
        }
    }
}
