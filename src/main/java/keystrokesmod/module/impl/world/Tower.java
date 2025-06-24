package keystrokesmod.module.impl.world;

import keystrokesmod.event.*;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.client.Settings;
import keystrokesmod.module.impl.movement.LongJump;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.GroupSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.ModuleUtils;
import keystrokesmod.utility.RotationUtils;
import keystrokesmod.utility.Utils;
import net.minecraft.network.play.client.*;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.potion.PotionEffect;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class Tower extends Module {
    final public SliderSetting towerMove;
    private SliderSetting speedSetting;
    final public SliderSetting verticalTower;
    final private SliderSetting slowedSpeed;
    final private SliderSetting slowedTicks;
    final private ButtonSetting disableWhileHurt;
    private GroupSetting cancelKnockbackGroup;
    private final ButtonSetting cancelKnockback;
    private ButtonSetting cancelVelocityRequired;
    public SliderSetting extraBlockDelay;

    final private String[] towerMoveModes = new String[]{"§cDisabled", "Vanilla", "Watchdog-vanilla", "Hyp", "2.5-tick", "1.5-tick", "1-tick", "10-tick", "Up"};
    final private String[] verticalTowerModes = new String[]{ "§cDisabled", "Vanilla", "Extra block", "Watchdog-vanilla", "Slowed" };

    private int slowTicks;
    private boolean wasTowering, vtowering;
    private int towerTicks;
    public boolean towering;
    private boolean hasTowered, startedTowerInAir, setLowMotion, firstJump;
    private int cMotionTicks, placeTicks;
    public int dCount;
    public float yaw;
    private int vt;

    public boolean blink;

    public int activeTicks;

    public float pitch;

    public boolean finishedTower, delay;
    public int delayTicks;

    //vertical tower
    private boolean aligning, aligned, placed;
    private double blockX;
    private double firstX, firstY, firstZ;
    public boolean placeExtraBlock;
    public int ebDelay;
    public boolean firstVTP;

    public boolean speed;

    private int grounds, towerVL;

    public int upFaces;

    private boolean jump;

    public Tower() {
        super("Tower", category.world);
        this.registerSetting(towerMove = new SliderSetting("Tower Move", 0, towerMoveModes));
        this.registerSetting(speedSetting = new SliderSetting("Speed", 3.0, 0.5, 8.0, 0.1));
        this.registerSetting(verticalTower = new SliderSetting("Vertical Tower", 0, verticalTowerModes));
        this.registerSetting(extraBlockDelay = new SliderSetting("Extra block delay", "", 0, 0, 10, 1));
        this.registerSetting(slowedSpeed = new SliderSetting("Slowed speed", "%", 0, 0, 100, 1));
        this.registerSetting(slowedTicks = new SliderSetting("Slowed ticks", 1, 0, 20, 1));
        this.registerSetting(disableWhileHurt = new ButtonSetting("Disable while hurt", false));
        this.registerSetting(cancelKnockbackGroup = new GroupSetting("Cancel knockback"));
        this.registerSetting(cancelKnockback = new ButtonSetting(cancelKnockbackGroup, "Enable Cancel knockback", false));
        this.registerSetting(cancelVelocityRequired = new ButtonSetting(cancelKnockbackGroup, "Require velocity enabled", false));
        //this.registerSetting(capUpFaces = new ButtonSetting("Cap up faces", false));

        this.canBeEnabled = false;
    }

    public void guiUpdate() {
        this.extraBlockDelay.setVisible(verticalTower.getInput() == 2, this);
    }

    @SubscribeEvent
    public void onReceivePacket(ReceivePacketEvent e) {
        if (!Utils.nullCheck() || !cancelKnockback()) {
            return;
        }
        if (e.getPacket() instanceof S12PacketEntityVelocity) {
            if (((S12PacketEntityVelocity) e.getPacket()).getEntityID() == mc.thePlayer.getEntityId()) {
                e.setCanceled(true);
            }
        }
        else if (e.getPacket() instanceof S27PacketExplosion) {
            e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public void onPreMotion(PreMotionEvent e) {
        if (!Utils.nullCheck()) {
            return;
        }
        if (delay) {
            return;
        }
        if (canTower() && Utils.keysDown()) {
            if (disableWhileHurt.isToggled() && ModuleUtils.damage) {
                return;
            }
            switch ((int) towerMove.getInput()) {
                case 1:

                    break;
                case 2:

                    break;
                case 3:

                    break;
                case 4:
                    if (towering) {
                        if (towerTicks == 6) {
                            e.setPosY(e.getPosY() + 0.000383527);
                            ModuleManager.scaffold.rotateForward(true);
                        }
                    }
                    break;
                case 5:
                    break;
                case 6:

                    break;
                case 7:
                    if (towering) {
                        if (towerTicks == 0) {
                            //e.setOnGround(true);
                        }
                    }
                    break;
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onClientRotation(ClientRotationEvent e) {
        if (!Utils.nullCheck()) {
            return;
        }
        int valY = (int) Math.round((mc.thePlayer.posY % 1) * 10000);
        int simpleY = (int) Math.round((mc.thePlayer.posY % 1.0D) * 100.0D);
        if (towerVL > 0) {
            --towerVL;
        }
        if (delay && ++delayTicks > 1) {
            delay = false;
            delayTicks = 0;
        }
        blink = false;
        if (towerMove.getInput() > 0) {
            if (canTower() && Utils.keysDown() && !delay) {
                ++activeTicks;
                speed = false;
                wasTowering = hasTowered = true;
                if (disableWhileHurt.isToggled() && ModuleUtils.damage) {
                    towerTicks = 0;
                    towering = false;
                    return;
                }
                switch ((int) towerMove.getInput()) {
                    case 1:
                        mc.thePlayer.motionY = 0.41965;
                        switch (towerTicks) {
                            case 1:
                                mc.thePlayer.motionY = 0.33;
                                break;
                            case 2:
                                mc.thePlayer.motionY = 1 - mc.thePlayer.posY % 1;
                                break;
                        }
                        if (towerTicks >= 3) {
                            towerTicks = 0;
                        }
                    case 2:
                        if (mc.thePlayer.posY % 1 == 0 && mc.thePlayer.onGround || towerVL > 0) {
                            towering = true;
                        }
                        if (towering) {
                            if (valY == 0) {
                                mc.thePlayer.motionY = 0.42f;
                                Utils.setSpeed(getTowerGroundSpeed(getSpeedLevel()));
                                startedTowerInAir = false;
                            } else if (valY > 4000 && valY < 4300) {
                                mc.thePlayer.motionY = 0.33f;
                                Utils.setSpeed(getTowerSpeed(getSpeedLevel()));
                                speed = true;
                            } else if (valY > 7000) {
                                mc.thePlayer.motionY = 1 - mc.thePlayer.posY % 1;
                            }
                            towerVL = 2;
                        }
                        break;
                    case 3:
                        if ((mc.thePlayer.posY % 1 == 0 && mc.thePlayer.onGround || towerVL > 0) && !setLowMotion) {
                            towering = true;
                        }
                        if (towering) {
                            if (valY == 0) {
                                mc.thePlayer.motionY = 0.42f;
                                Utils.setSpeed(getTowerGroundSpeed(getSpeedLevel()));
                                startedTowerInAir = false;
                            } else if (valY > 4000 && valY < 4300) {
                                mc.thePlayer.motionY = 0.33f;
                                Utils.setSpeed(getTowerSpeed(getSpeedLevel()));
                                speed = true;
                            } else if (valY > 7000) {
                                if (setLowMotion) {
                                    towering = false;
                                }
                                mc.thePlayer.motionY = 1 - mc.thePlayer.posY % 1;
                            }
                            towerVL = 2;
                        } else if (setLowMotion) {
                            ++cMotionTicks;
                            if (cMotionTicks == 1) {
                                mc.thePlayer.motionY = 0.08f;
                                Utils.setSpeed(getTowerSpeed(getSpeedLevel()));
                            } else if (cMotionTicks == 4) {
                                cMotionTicks = 0;
                                setLowMotion = false;
                                towering = true;
                                Utils.setSpeed(getTowerGroundSpeed(getSpeedLevel()) - 0.02);
                            }
                        }
                        break;
                    case 4:
                        if (mc.thePlayer.posY % 1 == 0 && mc.thePlayer.onGround) {
                            towering = true;
                        }
                        if (towering) {
                            towerTicks = mc.thePlayer.onGround ? 0 : ++towerTicks;
                            switch (simpleY) {
                                case 0:
                                    mc.thePlayer.motionY = 0.42f;
                                    if (towerTicks == 6) {
                                        mc.thePlayer.motionY = -0.078400001525879;
                                    }
                                    Utils.setSpeed(getTowerSpeed(getSpeedLevel()));
                                    speed = true;
                                    break;
                                case 42:
                                    mc.thePlayer.motionY = 0.33f;
                                    Utils.setSpeed(getTowerSpeed(getSpeedLevel()));
                                    speed = true;
                                    break;
                                case 75:
                                    mc.thePlayer.motionY = 1 - mc.thePlayer.posY % 1;
                                    break;
                            }
                        }
                        break;
                    case 5:
                        if (mc.thePlayer.posY % 1 == 0 && mc.thePlayer.onGround) {
                            towering = true;
                        }
                        if (towering) {
                            towerTicks = mc.thePlayer.onGround ? 0 : ++towerTicks;
                            switch (towerTicks) {
                                case 0:
                                    mc.thePlayer.motionY = 0.42f;
                                    Utils.setSpeed(get15tickspeed(getSpeedLevel())); // Speed + Strafe tick
                                    speed = true;
                                    break;
                                case 1:
                                    mc.thePlayer.motionY = 0.33f;
                                    Utils.setSpeed(Utils.getHorizontalSpeed()); // Strafe tick
                                    break;
                                case 2:
                                    mc.thePlayer.motionY = 1 - mc.thePlayer.posY % 1;
                                    break;
                                case 3:
                                    mc.thePlayer.motionY = 0.42f;
                                    Utils.setSpeed(Utils.getHorizontalSpeed()); // Strafe tick
                                    break;
                                case 4:
                                    mc.thePlayer.motionY = 0.33f;
                                    Utils.setSpeed(Utils.getHorizontalSpeed()); // Strafe tick
                                    break;
                                case 5:
                                    mc.thePlayer.motionY = 1 - mc.thePlayer.posY % 1f + 0.0000001;
                                    break;
                                case 6:
                                    mc.thePlayer.motionY = -0.01f;
                                    break;
                            }
                        }
                        break;
                    case 6:
                        if (mc.thePlayer.posY % 1 == 0 && mc.thePlayer.onGround) {
                            grounds++;
                        }
                        if (mc.thePlayer.posY % 1 == 0) {
                            towering = true;
                        }
                        if (towering) {
                            towerTicks = mc.thePlayer.onGround ? 0 : ++towerTicks;
                            switch (towerTicks) {
                                case 0:
                                    mc.thePlayer.motionY = 0.42f;
                                    Utils.setSpeed(get1tickspeed(getSpeedLevel())); // Speed + Strafe tick
                                    speed = true;
                                    break;
                                case 1:
                                    mc.thePlayer.motionY = 0.33f;
                                    Utils.setSpeed(Utils.getHorizontalSpeed()); // Strafe tick
                                    break;
                                case 2:
                                    mc.thePlayer.motionY = 1 - mc.thePlayer.posY % 1;
                                    break;
                                case 3:
                                    mc.thePlayer.motionY = 0.005;
                                    break;
                            }
                        }
                        break;
                    case 7:
                        if (mc.thePlayer.posY % 1 == 0 && mc.thePlayer.onGround) {
                            towering = true;
                        }
                        if (towering) {
                            ++towerTicks;
                            switch (towerTicks) {
                                case 7:
                                case 4:
                                case 1:
                                    mc.thePlayer.motionY = 0.42f;
                                    Utils.setSpeed(getTowerSpeed(getSpeedLevel())); // Speed + Strafe tick
                                    speed = true;
                                    break;
                                case 8:
                                case 5:
                                case 2:
                                    mc.thePlayer.motionY = 0.33f;
                                    Utils.setSpeed(Utils.getHorizontalSpeed()); // Strafe tick
                                    break;
                                case 6:
                                case 3:
                                    mc.thePlayer.motionY = 1 - mc.thePlayer.posY % 1;
                                    break;
                                case 9:
                                    mc.thePlayer.motionY = 1 - mc.thePlayer.posY % 1 + 0.0000001;
                                    break;
                                case 10:
                                    mc.thePlayer.motionY = -0.01f;
                                    towerTicks = 0;
                                    Utils.setSpeed(getTowerSpeed(getSpeedLevel())); // Speed + Strafe tick
                                    speed = true;
                                    break;
                            }
                        }
                        break;
                    case 8:
                        if (mc.thePlayer.posY % 1 == 0 && mc.thePlayer.onGround) {
                            towering = true;
                        }
                        if (towering && mc.thePlayer.onGround && Utils.isMoving()) {
                            mc.thePlayer.setSprinting(true);
                            mc.thePlayer.jump();
                            if (!Settings.movementFix.isToggled()) {
                                Utils.setSpeed(getTowerSpeed(getSpeedLevel()));
                            }
                            speed = true;
                        }
                        break;
                }
                if (speed) {
                    if (!Settings.movementFix.isToggled()) {
                        blink = true;
                        ModuleManager.scaffold.rotateForward(false);
                    }
                    else {
                        ModuleManager.scaffold.rotateForward(true);
                    }
                }
            } else {
                if (finishedTower) {
                    finishedTower = false;
                }
                if (hasTowered) {
                    finishedTower = true;
                }
                if (wasTowering && modulesEnabled()) {
                    if (slowedTicks.getInput() > 0 && slowedTicks.getInput() != 100 && slowTicks++ < slowedTicks.getInput()) {
                        mc.thePlayer.motionX *= slowedSpeed.getInput() / 100;
                        mc.thePlayer.motionZ *= slowedSpeed.getInput() / 100;
                    } else {
                        ModuleUtils.handleSlow();
                    }
                    if (slowTicks >= slowedTicks.getInput()) {
                        slowTicks = 0;
                        wasTowering = false;
                    }
                } else {
                    if (wasTowering) {
                        wasTowering = false;
                    }
                    slowTicks = 0;
                }
                if (speed || hasTowered && mc.thePlayer.onGround) {
                    if (!Settings.movementFix.isToggled()) {
                        Utils.setSpeed(Utils.getHorizontalSpeed(mc.thePlayer) / 1.6);
                    }
                }
                hasTowered = towering = firstJump = startedTowerInAir = setLowMotion = speed = jump = false;
                cMotionTicks = placeTicks = towerTicks = grounds = upFaces = activeTicks = 0;
                reset();
            }
        }
        if (verticalTower.getInput() > 0) {
            if (canTower() && !Utils.keysDown()) {
                wasTowering = true;
                switch ((int) verticalTower.getInput()) {
                    case 1:
                        mc.thePlayer.motionY = 0.42f;
                        break;
                    case 2:
                        if (!aligned) {
                            if (mc.thePlayer.onGround) {
                                if (!aligning) {
                                    blockX = (int) mc.thePlayer.posX + 1;

                                    firstX = mc.thePlayer.posX - 10;
                                    firstY = mc.thePlayer.posY;
                                    firstZ = mc.thePlayer.posZ;
                                }
                                mc.thePlayer.motionX = 0.2;
                                aligning = true;
                            }
                            if (aligning && mc.thePlayer.posX >= blockX) {
                                aligned = true;
                            }
                            yaw = RotationUtils.getRotations(firstX, firstY, firstZ)[0];
                            pitch = 0;
                        }
                        if (aligned) {
                            if (placed) {
                                yaw = RotationUtils.getRotations(firstX, firstY, firstZ)[0];
                                //yaw = RotationUtils.getRotations(mc.thePlayer.posX + 10, mc.thePlayer.posY, mc.thePlayer.posZ)[0];
                                /*if (ModuleManager.scaffold.placedVP) {
                                    pitch = 89.6F;
                                    ModuleManager.scaffold.placedVP = false;
                                }
                                else {*/
                                pitch = 86.6F;
                                //}
                            } else {
                                yaw = RotationUtils.getRotations(firstX, firstY, firstZ)[0];
                                pitch = 0;
                            }
                            placeExtraBlock = true;
                            Utils.setSpeed(0);
                            mc.thePlayer.motionX = 0;
                            mc.thePlayer.motionY = verticalTowerValue();
                            mc.thePlayer.motionZ = 0;
                            towerVL = 2;
                        }
                        break;
                    case 3:
                        if (mc.thePlayer.posY % 1 == 0 && mc.thePlayer.onGround || towerVL > 0) {
                            vtowering = true;
                        }
                        if (vtowering) {
                            mc.thePlayer.motionY = verticalTowerValue();
                            towerVL = 2;
                        }
                        break;
                    case 4:
                        if (mc.thePlayer.posY % 1 == 0 && mc.thePlayer.onGround || towerVL > 0) {
                            vtowering = true;
                        }
                        if (vtowering) {
                            ++vt;
                            towerVL = 2;
                            if (vt <= 6 && verticalTowerValue() != 0) {
                                mc.thePlayer.motionY = verticalTowerValue();
                            }
                            else {
                                vt = 0;
                            }
                        }
                        break;
                }
            } else {
                yaw = pitch = 0;
                aligning = aligned = placed = vtowering = false;
                firstX = 0;
                placeExtraBlock = firstVTP = false;
                ebDelay = vt = 0;
                ModuleManager.scaffold.placedVP = false;
            }
        }
    }

    public boolean isVerticalTowering() {
        return canTower() && !Utils.keysDown() && verticalTower.getInput() > 0;
    }

    private boolean disableJump;

    @SubscribeEvent
    public void onPostPlayerInput(PostPlayerInputEvent e) {
        disableJump = false;
        if (!ModuleManager.scaffold.isEnabled) {
            return;
        }
        if (canTower() && Utils.keysDown() && towerMove.getInput() > 0) {
            disableJump = true;
            if (!firstJump) {
                if (!mc.thePlayer.onGround) {
                    if (!startedTowerInAir) {
                        //Utils.setSpeed(getTowerGroundSpeed(getSpeedLevel()) - 0.04);
                    }
                    startedTowerInAir = true;
                }
                else if (mc.thePlayer.onGround) {
                    //Utils.setSpeed(getTowerGroundSpeed(getSpeedLevel()));
                    firstJump = true;
                }
            }
        }
        if (canTower() && !Utils.keysDown() && verticalTower.getInput() > 0) {
            disableJump = true;
        }
        if (delay) {
            disableJump = true;
        }
        if (disableJump) {
            if (disableWhileHurt.isToggled() && ModuleUtils.damage && !delay) {
                return;
            }
            mc.thePlayer.movementInput.jump = false;
        }
    }

    @SubscribeEvent
    public void onSendPacket(SendPacketEvent e) {
        if (e.getPacket() instanceof C08PacketPlayerBlockPlacement) {
            if (firstVTP) {
                ebDelay++;
                if (ebDelay >= extraBlockDelay.getInput() + 2) {
                    ebDelay = 0;
                }
            }
            if (canTower() && Utils.isMoving()) {
                ++placeTicks;
                if (((C08PacketPlayerBlockPlacement) e.getPacket()).getPlacedBlockDirection() == 1 && hasTowered) {
                    upFaces++;
                    if (placeTicks > 5) {
                        dCount++;
                        if (dCount >= 2) {
                            //Utils.sendMessage("Hey");
                            setLowMotion = true;
                        }
                    }
                }
                else {
                    dCount = upFaces = 0;
                }
            }
            else {
                placeTicks = upFaces = 0;
            }

            if (aligned) {
                placed = true;
            }
        }
    }

    private void reset() {
        towerTicks = 0;
        towering = false;
        placeTicks = 0;
        setLowMotion = false;
    }

    public boolean cancelKnockback() {
        if (!canTower()) {
            return false;
        }
        if (cancelVelocityRequired.isToggled() && !ModuleManager.velocity.isEnabled()) {
            return false;
        }
        if (cancelKnockback.isToggled()) {
            return true;
        }
        return false;
    }

    public boolean canTower() {
        if (!Utils.nullCheck() || !Utils.jumpDown() || !Utils.tabbedIn()) {
            return false;
        }
        else if (mc.thePlayer.isCollidedHorizontally) {
            return false;
        }
        else if ((mc.thePlayer.isInWater() || mc.thePlayer.isInLava())) {
            return false;
        }
        else if (modulesEnabled()) {
            return true;
        }
        return false;
    }

    private boolean modulesEnabled() {
        return (ModuleManager.scaffold.moduleEnabled && ModuleManager.scaffold.holdingBlocks() && ModuleManager.scaffold.hasSwapped && !LongJump.function);
    }

    private int getSpeedLevel() {
        for (PotionEffect potionEffect : mc.thePlayer.getActivePotionEffects()) {
            if (potionEffect.getEffectName().equals("potion.moveSpeed")) {
                return potionEffect.getAmplifier() + 1;
            }
            return 0;
        }
        return 0;
    }

    private double verticalTowerValue() {
        int valY = (int) Math.round((mc.thePlayer.posY % 1) * 10000);
        double value = 0;
        if (valY == 0) {
            value = 0.42f;
        } else if (valY > 4000 && valY < 4300) {
            value = 0.33f;
        } else if (valY > 7000) {
            value = 1 - mc.thePlayer.posY % 1;
        }
        return value;
    }

    private double getTowerSpeed(int speedLevel) {
        if (speedLevel == 0) {
            return (speedSetting.getInput() / 10);
        } else if (speedLevel == 1) {
            return (speedSetting.getInput() / 10) + 0.04;
        } else if (speedLevel == 2) {
            return (speedSetting.getInput() / 10) + 0.08;
        } else if (speedLevel == 3) {
            return (speedSetting.getInput() / 10) + 0.12;
        } else if (speedLevel == 4) {
            return (speedSetting.getInput() / 10) + 0.12;
        }
        return (speedSetting.getInput() / 10);
    }

    private double getTowerGroundSpeed(int speedLevel) {
        if (speedLevel == 0) {
            return (speedSetting.getInput() / 10) - 0.085;
        } else if (speedLevel == 1) {
            return (speedSetting.getInput() / 10) - 0.05;
        } else if (speedLevel == 2) {
            return (speedSetting.getInput() / 10);
        } else if (speedLevel == 3) {
            return (speedSetting.getInput() / 10) + 0.05;
        } else if (speedLevel == 4) {
            return (speedSetting.getInput() / 10) + 0.10;
        }
        return (speedSetting.getInput() / 10) - 0.08;
    }

    private double get15tickspeed(int speedLevel) {
        if (speedLevel == 0) {
            return (speedSetting.getInput() / 10);
        } else if (speedLevel == 1) {
            return (speedSetting.getInput() / 10) + 0.04;
        } else if (speedLevel == 2) {
            return (speedSetting.getInput() / 10) + 0.08;
        } else if (speedLevel == 3) {
            return (speedSetting.getInput() / 10) + 0.12;
        } else if (speedLevel == 4) {
            return (speedSetting.getInput() / 10) + 0.13;
        }
        return (speedSetting.getInput() / 10);
    }

    private double get1tickspeed(int speedLevel) {
        if (speedLevel == 0) {
            return (speedSetting.getInput() / 10);
        } else if (speedLevel == 1) {
            return (speedSetting.getInput() / 10) + 0.03;
        } else if (speedLevel == 2) {
            return (speedSetting.getInput() / 10) + 0.06;
        } else if (speedLevel == 3) {
            return (speedSetting.getInput() / 10) + 0.1;
        } else if (speedLevel == 4) {
            return (speedSetting.getInput() / 10) + 0.11;
        }
        return (speedSetting.getInput() / 10);
    }

}