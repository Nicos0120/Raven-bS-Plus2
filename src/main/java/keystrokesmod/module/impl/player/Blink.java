package keystrokesmod.module.impl.player;

import keystrokesmod.Raven;
import keystrokesmod.event.*;
import keystrokesmod.module.Module;
import keystrokesmod.module.ModuleManager;
import keystrokesmod.module.impl.combat.KillAura;
import keystrokesmod.module.impl.movement.LongJump;
import keystrokesmod.module.impl.render.HUD;
import keystrokesmod.module.setting.impl.ButtonSetting;
import keystrokesmod.module.setting.impl.DescriptionSetting;
import keystrokesmod.module.setting.impl.SliderSetting;
import keystrokesmod.utility.*;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.network.Packet;
import net.minecraft.network.handshake.client.C00Handshake;
import net.minecraft.network.login.client.C00PacketLoginStart;
import net.minecraft.network.login.client.C01PacketEncryptionResponse;
import net.minecraft.network.play.client.C00PacketKeepAlive;
import net.minecraft.network.play.client.C02PacketUseEntity;
import net.minecraft.network.play.client.C0FPacketConfirmTransaction;
import net.minecraft.network.play.server.*;
import net.minecraft.network.status.client.C00PacketServerQuery;
import net.minecraft.network.status.client.C01PacketPing;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;

import static keystrokesmod.module.impl.render.HUD.theme;

public class Blink extends Module {
    private static SliderSetting inf, maximumBlinkTicks;
    public ButtonSetting renderTimer, initialPosition;
    private ButtonSetting disableOnBreak, disableOnAttack;
    private int color = new Color(0, 187, 255, 255).getRGB();
    public boolean blink;

    public Blink() {
        super("Blink", category.player);
        this.registerSetting(maximumBlinkTicks = new SliderSetting("Maximum duration", "s", 0.00, 0.00, 10.00, 0.1));
        this.registerSetting(initialPosition = new ButtonSetting("Show initial position", true));
        this.registerSetting(renderTimer = new ButtonSetting("Render Timer", false));
        this.registerSetting(disableOnBreak = new ButtonSetting("Disable on Break", false));
        this.registerSetting(disableOnAttack = new ButtonSetting("Disable on Attack", false));
    }

    public void onDisable() {
        blink = false;
    }

    @SubscribeEvent
    public void onSendPacketAll(SendAllPacketsEvent e) {
        if (disableOnBreak.isToggled() && (Utils.usingBedAura() || ModuleUtils.isBreaking)) {
            disable();
        }
        if (disableOnAttack.isToggled() && e.getPacket() instanceof C02PacketUseEntity) {
            disable();
        }
        if (maximumBlinkTicks.getInput() != 0 && BlinkHandler.blinkTicks > (maximumBlinkTicks.getInput() * 20)) {
            disable();
        }
        blink = true;
    }

    @Override
    public String getInfo() {
        return BlinkHandler.blinkTicks + "";
    }
}

