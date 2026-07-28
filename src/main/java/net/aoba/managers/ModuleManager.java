/*
 * Aoba Hacked Client
 * Copyright (C) 2019-2024 coltonk9043
 *
 * Licensed under the GNU General Public License, Version 3 or later.
 * See <http://www.gnu.org/licenses/>.
 */

package net.aoba.managers;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.lwjgl.glfw.GLFW;
import com.mojang.blaze3d.platform.InputConstants.Key;
import com.mojang.logging.LogUtils;

import net.aoba.Aoba;
import net.aoba.api.IAddon;
import net.aoba.event.events.KeyDownEvent;
import net.aoba.event.listeners.KeyDownListener;
import net.aoba.gui.GuiManager;
import net.aoba.module.AntiCheat;
import net.aoba.module.Module;
import net.aoba.module.modules.combat.Aimbot;
import net.aoba.module.modules.combat.AntiInvis;
import net.aoba.module.modules.combat.AntiKnockback;
import net.aoba.module.modules.combat.AutoCooldown;
import net.aoba.module.modules.combat.AutoAnchor;
import net.aoba.module.modules.combat.AutoRespawn;
import net.aoba.module.modules.combat.AutoSoup;
import net.aoba.module.modules.combat.AutoTotem;
import net.aoba.module.modules.combat.BedAura;
import net.aoba.module.modules.combat.BowAimbot;
import net.aoba.module.modules.combat.Criticals;
import net.aoba.module.modules.combat.AutoCrystal;
import net.aoba.module.modules.combat.ElytraTarget;
import net.aoba.module.modules.combat.KillAura;
import net.aoba.module.modules.combat.KnockbackDisplacer;
import net.aoba.module.modules.combat.MaceAura;
import net.aoba.module.modules.combat.Nametags;
import net.aoba.module.modules.combat.NoMissDelay;
import net.aoba.module.modules.combat.Reach;
import net.aoba.module.modules.combat.ShieldBreaker;
import net.aoba.module.modules.combat.TriggerBot;
import net.aoba.module.modules.misc.*;
import net.aoba.module.modules.movement.ClickTP;
import net.aoba.module.modules.movement.EntityControl;
import net.aoba.module.modules.movement.FastLadder;
import net.aoba.module.modules.movement.Fly;
import net.aoba.module.modules.movement.Freecam;
import net.aoba.module.modules.movement.Glide;
import net.aoba.module.modules.movement.GuiMove;
import net.aoba.module.modules.movement.HighJump;
import net.aoba.module.modules.movement.Jesus;
import net.aoba.module.modules.movement.Jetpack;
import net.aoba.module.modules.movement.NoFall;
import net.aoba.module.modules.movement.NoJumpDelay;
import net.aoba.module.modules.movement.NoSlowdown;
import net.aoba.module.modules.movement.Noclip;
import net.aoba.module.modules.movement.ReverseStep;
import net.aoba.module.modules.movement.Safewalk;
import net.aoba.module.modules.movement.Sneak;
import net.aoba.module.modules.movement.Speed;
import net.aoba.module.modules.movement.Spider;
import net.aoba.module.modules.movement.Sprint;
import net.aoba.module.modules.movement.Step;
import net.aoba.module.modules.movement.Strafe;
import net.aoba.module.modules.render.Breadcrumbs;
import net.aoba.module.modules.render.ChestESP;
import net.aoba.module.modules.render.EntityESP;
import net.aoba.module.modules.render.FocusFps;
import net.aoba.module.modules.render.Fullbright;
import net.aoba.module.modules.render.ItemESP;
import net.aoba.module.modules.render.NoRender;
import net.aoba.module.modules.render.POV;
import net.aoba.module.modules.render.BlockESP;
import net.aoba.module.modules.render.Tooltips;
import net.aoba.module.modules.render.Tracer;
import net.aoba.module.modules.render.Trajectory;
import net.aoba.module.modules.render.XRay;
import net.aoba.module.modules.render.Zoom;
import net.aoba.module.modules.world.*;
import net.aoba.settings.Setting;
import net.aoba.settings.types.EnumSetting;
import net.minecraft.client.Minecraft;

public class ModuleManager implements KeyDownListener {
	private static final Minecraft MC = Minecraft.getInstance();

	public ArrayList<Module> modules = new ArrayList<Module>();

	// Modules
	public Aimbot aimbot = new Aimbot();
	public AirPlace airplace = new AirPlace();
	public AntiCactus anticactus = new AntiCactus();
	public AntiInvis antiinvis = new AntiInvis();
	public AntiKnockback antiknockback = new AntiKnockback();
	public AutoBreed autobreed = new AutoBreed();
	public AutoCooldown autocooldown = new AutoCooldown();
	public AutoAnchor autoanchor = new AutoAnchor();
	public AutoCrystal autocrystal = new AutoCrystal();
	public AutoEat autoeat = new AutoEat();
	public AutoFarm autofarm = new AutoFarm();
	public AutoFish autofish = new AutoFish();
	public AntiHunger antihunger = new AntiHunger();
	public AutoOminousBottle autoOminousBottle = new AutoOminousBottle();
	public AutoShear autoShear = new AutoShear();
	public AutoSign autosign = new AutoSign();
	public AutoSoup autosoup = new AutoSoup();
	public AutoTotem autoTotem = new AutoTotem();
	public AutoTool autotool = new AutoTool();
	public AutoRespawn autorespawn = new AutoRespawn();
	public AutoWalk autowalk = new AutoWalk();
	public BedAura bedAura = new BedAura();
	public BlockESP blockesp = new BlockESP();
	public BowAimbot bowaimbot = new BowAimbot();
	public Breadcrumbs breadcrumbs = new Breadcrumbs();
	public ChestESP chestesp = new ChestESP();
	public Criticals criticals = new Criticals();
	public ClickTP clickTP = new ClickTP();
	public DiscordRPCModule discordRPC = new DiscordRPCModule();
	public ElytraTarget elytraTarget = new ElytraTarget();
	public EntityControl entityControl = new EntityControl();
	public EntityESP entityesp = new EntityESP();
	public EXPThrower expthrower = new EXPThrower();
	public FakePlayer fakeplayer = new FakePlayer();
	public FastLadder fastladder = new FastLadder();
	public FastPlace fastplace = new FastPlace();
	public FastBreak fastbreak = new FastBreak();
	public Fly fly = new Fly();
	public Freecam freecam = new Freecam();
	public Fullbright fullbright = new Fullbright();
	public ItemESP itemesp = new ItemESP();
	public NoRender norender = new NoRender();
	public FocusFps focusfps = new FocusFps();
	public Glide glide = new Glide();
	public GuiMove guimove = new GuiMove();
	public HighJump higherjump = new HighJump();
	public Jesus jesus = new Jesus();
	public Jetpack jetpack = new Jetpack();
	public KnockbackDisplacer knockbackDisplacer = new KnockbackDisplacer();
	public KillAura killaura = new KillAura();
	public MaceAura maceaura = new MaceAura();
	public MCA mcf = new MCA();
	public Nametags nametags = new Nametags();
	public Noclip noclip = new Noclip();
	public NoFall nofall = new NoFall();
	public NoJumpDelay nojumpdelay = new NoJumpDelay();
	public NoMissDelay nomissdelay = new NoMissDelay();
	public NoSlowdown noslowdown = new NoSlowdown();
	public Nuker nuker = new Nuker();
	public POV pov = new POV();
	public RandomPlace randomplace = new RandomPlace();
	public Reach reach = new Reach();
	public ReverseStep reverseStep = new ReverseStep();
	public Safewalk safewalk = new Safewalk();
	public Scaffold scaffold = new Scaffold();
	public ShieldBreaker shieldBreaker = new ShieldBreaker();
	public Sneak sneak = new Sneak();
	public Speed speed = new Speed();
	public Spider spider = new Spider();
	public Sprint sprint = new Sprint();
	public Step step = new Step();
	public Strafe strafe = new Strafe();
	public Surround surround = new Surround();
	public TileBreaker tilebreaker = new TileBreaker();
	public Timer timer = new Timer();
	public Tooltips tooltips = new Tooltips();
	public Tracer tracer = new Tracer();
	public Trajectory trajectory = new Trajectory();
	public TriggerBot triggerbot = new TriggerBot();
	public XCarry xCarry = new XCarry();
	public XRay xray = new XRay();
	public Zoom zoom = new Zoom();

	public EnumSetting<AntiCheat> antiCheat = EnumSetting.<AntiCheat>builder().id("aoba_anticheat")
			.displayName("Current AntiCheat")
			.description(
					"This setting will disable any modules or features that are known to be detected by a specific anticheat. ")
			.defaultValue(AntiCheat.Vanilla).onUpdate(s -> {
				for (Module module : modules) {
					if (module.isDetectable(s))
						module.state.setValue(false);
				}
			}).build();

	public ModuleManager(List<IAddon> addons) {
		try {
			// Attempts to find each field of type Module and add it to the module list.
			for (Field field : ModuleManager.class.getDeclaredFields()) {
				if (!Module.class.isAssignableFrom(field.getType()))
					continue;
				Module module = (Module) field.get(this);
				addModule(module);
			}

			// Gets each Addon and adds their modules to the client.
			addons.stream().filter(Objects::nonNull).forEach(addon -> {
				addon.modules().forEach(module -> {
					addModule(module);
				});
			});
		} catch (Exception e) {
			LogUtils.getLogger().error("Error initializing Aoba modules: " + e.getMessage());
		}

		// Registers all Module settings to the settings manager.
		for (Module module : modules) {
			for (Setting<?> setting : module.getSettings()) {
				SettingManager.registerSetting(setting);
			}
		}

		Aoba.getInstance().eventManager.AddListener(KeyDownListener.class, this);
	}

	public void addModule(Module module) {
		modules.add(module);
	}

	public void disableAll() {
		for (Module module : modules) {
			module.state.setValue(false);
		}
	}

	public Module getModuleByName(String string) {
		for (Module module : modules) {
			if (module.getName().equalsIgnoreCase(string)) {
				return module;
			}
		}
		return null;
	}

	@Override
	public void onKeyDown(KeyDownEvent event) {
		if (GuiManager.isKeyboardInputActive())
			return;
		if (event.GetKey() == GLFW.GLFW_KEY_UNKNOWN)
			return;

		if (MC.gui.screen() == null) {
			for (Module module : modules) {
				if (module.isDetectable(antiCheat.getValue()))
					continue;

				Key binding = module.getBind().getValue();
				if (binding.getValue() == GLFW.GLFW_KEY_UNKNOWN)
					continue;
				if (binding.getValue() == event.GetKey()) {
					module.toggle();
				}
			}
		}
	}
}
