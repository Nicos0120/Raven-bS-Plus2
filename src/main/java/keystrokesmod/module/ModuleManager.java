package keystrokesmod.module;

import keystrokesmod.module.impl.client.ChatCommands;
import keystrokesmod.module.impl.client.CommandLine;
import keystrokesmod.module.impl.client.Gui;
import keystrokesmod.module.impl.client.Settings;
import keystrokesmod.module.impl.combat.*;
import keystrokesmod.module.impl.fun.*;
import keystrokesmod.module.impl.minigames.*;
import keystrokesmod.module.impl.movement.*;
import keystrokesmod.module.impl.other.*;
import keystrokesmod.module.impl.player.*;
import keystrokesmod.module.impl.render.*;
import keystrokesmod.module.impl.world.*;
import keystrokesmod.utility.Utils;
import keystrokesmod.utility.profile.Manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ModuleManager {
    public static List<Module> modules = new ArrayList<>();
    public static List<Module> organizedModules = new ArrayList<>();
    public static Module nameHider;
    public static Module fastPlace;
    public static MurderMystery murderMystery;
    public static InvMove invmove;
    public static SkyWars skyWars;
    public static AntiFireball antiFireball;
    public static AutoSwap autoSwap;
    public static BedAura bedAura;
    public static FastMine fastMine;
    public static Module antiShuffle;
    public static Module commandLine;
    public static Module antiBot;
    public static NoSlow noSlow;
    public static KillAura killAura;
    public static Module autoClicker;
    public static Module hitBox;
    public static Module reach;
    public static BedESP bedESP;
    public static Chams chams;
    public static HUD hud;
    public static Module timer;
    public static Fly fly;
    public static Module wTap;
    public static TargetHUD targetHUD;
    public static NoFall noFall;
    public static Disabler disabler;
    public static NoRotate noRotate;
    public static PlayerESP playerESP;
    public static Module chrisESP;
    public static Module reduce;
    public static Safewalk safeWalk;
    public static Module keepSprint;
    public static ExtendCamera extendCamera;
    public static InvManager invManager;
    public static Tower tower;
    public static NoCameraClip noCameraClip;
    public static BedWars bedwars;
    public static Bhop bhop;
    public static NoHurtCam noHurtCam;
    public static Scaffold scaffold;
    public static AutoTool autoTool;
    public static Sprint sprint;
    public static Weather weather;
    public static Arrows arrows;
    public static ChatCommands chatCommands;
    public static LongJump LongJump;
    public static Blink blink;
    public static Velocity velocity;
    public static TargetStrafe targetStrafe;
    public static FastFall fastFall;
    public static AntiVoid antiVoid;
    public static Spammer spammer;
    public static AntiDebuff antiDebuff;
    public static Blink lagRange;

    public void register() {
        this.addModule(autoClicker = new AutoClicker());
        this.addModule(LongJump = new LongJump());
        this.addModule(new AimAssist());
        this.addModule(weather = new Weather());
        this.addModule(chatCommands = new ChatCommands());
        this.addModule(new ClickAssist());
        this.addModule(tower = new Tower());
        this.addModule(skyWars = new SkyWars());
        this.addModule(new DelayRemover());
        this.addModule(hitBox = new HitBox());
        this.addModule(new Radar());
        this.addModule(new Settings());
        this.addModule(reach = new Reach());
        this.addModule(extendCamera = new ExtendCamera());
        this.addModule(new RodAimbot());
        this.addModule(velocity = new Velocity());
        this.addModule(bhop = new Bhop());
        this.addModule(invManager = new InvManager());
        this.addModule(new ChatBypass());
        this.addModule(scaffold = new Scaffold());
        this.addModule(blink = new Blink());
        this.addModule(new AutoRequeue());
        this.addModule(new AntiAFK());
        this.addModule(autoTool = new AutoTool());
        this.addModule(noHurtCam = new NoHurtCam());
        this.addModule(new SpeedBuilders());
        this.addModule(new Teleport());
        this.addModule(fly = new Fly());
        this.addModule(invmove = new InvMove());
        this.addModule(new TPAura());
        this.addModule(new Trajectories());
        this.addModule(autoSwap = new AutoSwap());
        this.addModule(keepSprint = new KeepSprint());
        this.addModule(bedAura = new BedAura());
        this.addModule(noSlow = new NoSlow());
        this.addModule(new Indicators());
        this.addModule(new LatencyAlerts());
        this.addModule(noCameraClip = new NoCameraClip());
        this.addModule(sprint = new Sprint());
        this.addModule(timer = new Timer());
        this.addModule(new VClip());
        this.addModule(new AutoPlace());
        this.addModule(fastPlace = new FastPlace());
        this.addModule(new Freecam());
        this.addModule(noFall = new NoFall());
        this.addModule(disabler = new Disabler());
        this.addModule(safeWalk = new Safewalk());
        this.addModule(reduce = new Reduce());
        this.addModule(antiBot = new AntiBot());
        this.addModule(antiShuffle = new AntiShuffle());
        this.addModule(chams = new Chams());
        this.addModule(new ChestESP());
        this.addModule(new Nametags());
        this.addModule(playerESP = new PlayerESP());
        this.addModule(new Tracers());
        this.addModule(hud = new HUD());
        this.addModule(new Anticheat());
        this.addModule(new BreakProgress());
        this.addModule(wTap = new WTap());
        this.addModule(new Xray());
        this.addModule(new BridgeInfo());
        this.addModule(targetHUD = new TargetHUD());
        this.addModule(new DuelsStats());
        this.addModule(antiFireball = new AntiFireball());
        this.addModule(bedESP = new BedESP());
        this.addModule(murderMystery = new MurderMystery());
        this.addModule(new keystrokesmod.script.Manager());
        this.addModule(new SumoFences());
        this.addModule(new Fun.ExtraBobbing());
        this.addModule(killAura = new KillAura());
        this.addModule(new Fun.FlameTrail());
        this.addModule(new Fun.SlyPort());
        this.addModule(new ItemESP());
        this.addModule(new MobESP());
        this.addModule(new Fun.Spin());
        this.addModule(noRotate = new NoRotate());
        this.addModule(new FakeChat());
        this.addModule(nameHider = new NameHider());
        this.addModule(new FakeLag());
        this.addModule(new WaterBucket());
        this.addModule(commandLine = new CommandLine());
        this.addModule(bedwars = new BedWars());
        this.addModule(fastMine = new FastMine());
        this.addModule(arrows = new Arrows());
        this.addModule(new Manager());
        this.addModule(new ViewPackets());
        this.addModule(new AutoWho());
        this.addModule(new Gui());
        this.addModule(new Shaders());
        this.addModule(targetStrafe = new TargetStrafe());
        this.addModule(fastFall = new FastFall());
        this.addModule(antiVoid = new AntiVoid());
        this.addModule(spammer = new Spammer());
        this.addModule(antiDebuff = new AntiDebuff());
        antiBot.enable();
        Collections.sort(this.modules, Comparator.comparing(Module::getName));
    }

    public void addModule(Module m) {
        modules.add(m);
    }

    public List<Module> getModules() {
        return modules;
    }

    public List<Module> inCategory(Module.category categ) {
        ArrayList<Module> categML = new ArrayList<>();

        for (Module mod : this.getModules()) {
            if (mod.moduleCategory().equals(categ)) {
                categML.add(mod);
            }
        }

        return categML;
    }

    public Module getModule(String moduleName) {
        for (Module module : modules) {
            if (module.getName().equals(moduleName)) {
                return module;
            }
        }
        return null;
    }

    public static Module getModule(Class clazz) {
        for (Module module : modules) {
            if (module.getClass().equals(clazz)) {
                return module;
            }
        }
        return null;
    }

    public static void sort() {
        if (HUD.alphabeticalSort.isToggled()) {
            Collections.sort(organizedModules, Comparator.comparing(Module::getNameInHud));
        }
        else {
            organizedModules.sort((o1, o2) -> Utils.mc.fontRendererObj.getStringWidth(o2.getNameInHud() + ((HUD.showInfo.isToggled() && !o2.getInfo().isEmpty()) ? " " + o2.getInfo() : "")) - Utils.mc.fontRendererObj.getStringWidth(o1.getNameInHud() + (HUD.showInfo.isToggled() && !o1.getInfo().isEmpty() ? " " + o1.getInfo() : "")));
        }
    }
    public static boolean canExecuteChatCommand() {
        return ModuleManager.chatCommands != null && ModuleManager.chatCommands.isEnabled();
    }

    public static boolean lowercaseChatCommands() {
        return ModuleManager.chatCommands != null && ModuleManager.chatCommands.isEnabled() && ModuleManager.chatCommands.lowercase();
    }
}