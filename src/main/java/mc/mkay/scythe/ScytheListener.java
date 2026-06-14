package mc.mkay.scythe;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

import java.util.*;

public class ScytheListener implements Listener {

    private static final String OWNER = "mkaymc";
    private static final long ABILITY_COOLDOWN_MS = 8000;
    private static final long PULL_COOLDOWN_MS    = 6000;
    private static final double PULL_RANGE        = 12.0;

    private final ScythePlugin plugin;
    private final Map<UUID, Long> abilityCooldowns = new HashMap<>();
    private final Map<UUID, Long> pullCooldowns    = new HashMap<>();

    // ── Kill tracking ──────────────────────────────────────────────────────────
    // 0 = scythe form, 5 kills = dagger form, 10 kills = bow form
    private final Map<UUID, Integer> killCounts = new HashMap<>();
    // Track current form so we can announce transitions
    public enum ScytheForm { SCYTHE, DAGGER, BOW }
    private final Map<UUID, ScytheForm> currentForm = new HashMap<>();

    public ScytheListener(ScythePlugin plugin) {
        this.plugin = plugin;
    }

    // ── PUBLIC: add kills (for /scythekills command) ──────────────────────────
    public void addKills(Player player, int amount) {
        int prev = killCounts.getOrDefault(player.getUniqueId(), 0);
        int next = prev + amount;
        killCounts.put(player.getUniqueId(), next);
        checkFormTransition(player, prev, next);
        player.sendMessage(Component.text("☠ Scythe kills: " + next, NamedTextColor.LIGHT_PURPLE)
            .decoration(TextDecoration.ITALIC, false));
    }

    // ── KILL TRACKING ─────────────────────────────────────────────────────────
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        if (!killer.getName().equalsIgnoreCase(OWNER)) return;
        if (!ScytheItem.isScythe(killer.getInventory().getItemInMainHand(), plugin)) return;

        int prev = killCounts.getOrDefault(killer.getUniqueId(), 0);
        int next = prev + 1;
        killCounts.put(killer.getUniqueId(), next);
        checkFormTransition(killer, prev, next);
    }

    private void checkFormTransition(Player player, int prev, int next) {
        ScytheForm prevForm = formForKills(prev);
        ScytheForm nextForm = formForKills(next);
        currentForm.put(player.getUniqueId(), nextForm);

        if (prevForm != nextForm) {
            switch (nextForm) {
                case DAGGER -> {
                    player.getWorld().playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1.5f, 1.2f);
                    player.getWorld().spawnParticle(Particle.CHERRY_LEAVES, player.getLocation().add(0,1,0), 30, 0.5, 0.5, 0.5, 0.05);
                    player.sendActionBar(Component.text("⚔ Scythe transformed: DAGGER MODE", NamedTextColor.AQUA)
                        .decoration(TextDecoration.ITALIC, false));
                    player.sendMessage(
                        Component.text("≋ The scythe splinters into ", NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false)
                        .append(Component.text("Dagger Form", NamedTextColor.AQUA).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false))
                        .append(Component.text(" at 5 kills!", NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false))
                    );
                }
                case BOW -> {
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.5f, 0.6f);
                    player.getWorld().spawnParticle(Particle.CHERRY_LEAVES, player.getLocation().add(0,1,0), 50, 0.8, 0.8, 0.8, 0.05);
                    player.sendActionBar(Component.text("🏹 Scythe transformed: BOW MODE", NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false));
                    player.sendMessage(
                        Component.text("≋ The scythe ascends to ", NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false)
                        .append(Component.text("Bow Form", NamedTextColor.YELLOW).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false))
                        .append(Component.text(" at 10 kills!", NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false))
                    );
                }
                default -> {}
            }
        }
    }

    private ScytheForm formForKills(int kills) {
        if (kills >= 10) return ScytheForm.BOW;
        if (kills >= 5)  return ScytheForm.DAGGER;
        return ScytheForm.SCYTHE;
    }

    // ── SHIFT + SWAP HANDS (F): cycle forms manually ─────────────────────────
    @EventHandler
    public void onSwapHands(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (!player.getName().equalsIgnoreCase(OWNER)) return;
        if (!player.isSneaking()) return;
        if (!ScytheItem.isScythe(player.getInventory().getItemInMainHand(), plugin)) return;

        event.setCancelled(true);
        cycleForm(player);
    }

    private void cycleForm(Player player) {
        UUID uid = player.getUniqueId();
        ScytheForm current = currentForm.getOrDefault(uid, ScytheForm.SCYTHE);
        ScytheForm next = switch (current) {
            case SCYTHE -> ScytheForm.DAGGER;
            case DAGGER -> ScytheForm.BOW;
            case BOW -> ScytheForm.SCYTHE;
        };
        currentForm.put(uid, next);

        World world = player.getWorld();
        Location loc = player.getLocation();

        switch (next) {
            case SCYTHE -> {
                world.playSound(loc, Sound.BLOCK_CHERRY_LEAVES_BREAK, 1.2f, 1.0f);
                world.spawnParticle(Particle.CHERRY_LEAVES, loc.clone().add(0, 1, 0), 25, 0.4, 0.4, 0.4, 0.05);
                player.sendActionBar(Component.text(" Scythe Form", NamedTextColor.LIGHT_PURPLE)
                    .decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
            }
            case DAGGER -> {
                world.playSound(loc, Sound.ITEM_TRIDENT_THROW, 1.2f, 1.4f);
                world.spawnParticle(Particle.CRIT, loc.clone().add(0, 1, 0), 20, 0.3, 0.3, 0.3, 0.1);
                world.spawnParticle(Particle.CHERRY_LEAVES, loc.clone().add(0, 1, 0), 15, 0.3, 0.3, 0.3, 0.04);
                player.sendActionBar(Component.text("⚔ Dagger Form", NamedTextColor.AQUA)
                    .decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
            }
            case BOW -> {
                world.playSound(loc, Sound.ENTITY_ARROW_SHOOT, 1.2f, 0.8f);
                world.spawnParticle(Particle.CHERRY_LEAVES, loc.clone().add(0, 1, 0), 30, 0.5, 0.5, 0.5, 0.05);
                player.sendActionBar(Component.text("🏹 Bow Form", NamedTextColor.YELLOW)
                    .decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
            }
        }
    }

    // ── RIGHT CLICK ────────────────────────────────────────────────────────────
    @EventHandler
    public void onRightClick(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        if (!player.getName().equalsIgnoreCase(OWNER)) return;
        if (!ScytheItem.isScythe(player.getInventory().getItemInMainHand(), plugin)) return;

        event.setCancelled(true);

        ScytheForm form = currentForm.getOrDefault(player.getUniqueId(), ScytheForm.SCYTHE);

        switch (form) {
            case SCYTHE -> {
                if (player.isSneaking()) handlePetalSnare(player);
                else handleBlossomBurst(player);
            }
            case DAGGER -> {
                if (player.isSneaking()) handleDaggerDash(player);
                else handleDaggerSlash(player);
            }
            case BOW -> {
                if (player.isSneaking()) handleBowVolley(player);
                else handleBowShot(player);
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SCYTHE FORM ABILITIES (original — untouched)
    // ══════════════════════════════════════════════════════════════════════════

    private void handleBlossomBurst(Player player) {
        long now = System.currentTimeMillis();
        UUID uid = player.getUniqueId();
        long last = (long) abilityCooldowns.getOrDefault(uid, 0L);
        long remaining = ABILITY_COOLDOWN_MS - (now - last);
        if (remaining > 0) {
            player.sendActionBar(Component.text("Blossom Burst recharging... " + (remaining / 1000),
                NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false));
            return;
        }
        abilityCooldowns.put(uid, now);

        Location eyeLoc = player.getEyeLocation();
        World world = player.getWorld();
        Vector dir = eyeLoc.getDirection();

        RayTraceResult result = world.rayTraceEntities(eyeLoc, dir, 20.0, 0.5,
            e -> !e.equals(player));
        Entity hitEntity = result != null ? result.getHitEntity() : null;

        if (!(hitEntity instanceof LivingEntity target)) {
            player.sendActionBar(Component.text("No target in range.", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
            return;
        }

        Location targetLoc = target.getLocation();
        world.playSound(targetLoc, Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 1.0f, 1.2f);
        world.playSound(targetLoc, Sound.BLOCK_CHERRY_LEAVES_PLACE, 1.0f, 0.8f);
        player.sendActionBar(Component.text(" Blossom Burst!", NamedTextColor.LIGHT_PURPLE)
            .decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));

        new BukkitRunnable() {
            int ticks = 0;
            @Override public void run() {
                if (ticks++ >= 6) { cancel(); return; }
                world.spawnParticle(Particle.CHERRY_LEAVES, targetLoc.clone().add(0, 1, 0), 12, 0.4, 0.4, 0.4, 0.03);
                world.playSound(targetLoc, Sound.BLOCK_CHERRY_LEAVES_BREAK, 0.5f, 1.2f);
            }
        }.runTaskTimer(plugin, 0, 2);

        Location origin = player.getLocation();
        World w = world;
        new BukkitRunnable() {
            @Override public void run() {
                for (Entity entity : w.getNearbyEntities(targetLoc, 3.5, 3.5, 3.5)) {
                    if (entity.equals(player)) continue;
                    if (entity.getGameMode() == GameMode.CREATIVE) continue;
                    if (!(entity instanceof LivingEntity living)) continue;
                    Vector knockback = entity.getLocation().toVector()
                        .subtract(origin.toVector()).normalize().multiply(1.4).setY(0.5);
                    living.damage(6.0, player);
                    living.setVelocity(knockback);
                    living.setFireTicks(60);
                    w.spawnParticle(Particle.CHERRY_LEAVES,
                        living.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.04);
                }
                w.playSound(targetLoc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.2f, 0.7f);
                w.playSound(targetLoc, Sound.ITEM_TOTEM_USE, 0.6f, 1.5f);
            }
        }.runTaskLater(plugin, 8);

        new BukkitRunnable() {
            @Override public void run() {
                Location burstLoc = targetLoc.clone().add(0, 1, 0);
                for (int i = 0; i < 3; i++) {
                    w.spawnParticle(Particle.CHERRY_LEAVES, burstLoc, 20, 0.6, 0.6, 0.6, 0.05);
                }
            }
        }.runTaskLater(plugin, 10);
    }

    private void handlePetalSnare(Player player) {
        long now = System.currentTimeMillis();
        UUID uid = player.getUniqueId();
        long last = (long) pullCooldowns.getOrDefault(uid, 0L);
        long remaining = PULL_COOLDOWN_MS - (now - last);
        if (remaining > 0) {
            player.sendActionBar(Component.text("Petal Snare recharging... " + (remaining / 1000),
                NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false));
            return;
        }
        pullCooldowns.put(uid, now);

        Location eyeLoc = player.getEyeLocation();
        World world = player.getWorld();
        Vector dir = eyeLoc.getDirection();

        RayTraceResult result = world.rayTraceEntities(eyeLoc, dir, PULL_RANGE, 0.5,
            e -> !e.equals(player));
        Entity hitEntity = result != null ? result.getHitEntity() : null;

        if (!(hitEntity instanceof LivingEntity target)) {
            player.sendActionBar(Component.text("No target in range.", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
            return;
        }

        Location targetLoc = target.getLocation();
        world.playSound(targetLoc, Sound.ENTITY_FISHING_BOBBER_RETRIEVE, 1.5f, 0.6f);
        world.playSound(targetLoc, Sound.BLOCK_CHERRY_LEAVES_PLACE, 1.0f, 0.5f);
        player.sendActionBar(Component.text(" Petal Snare!", NamedTextColor.LIGHT_PURPLE)
            .decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));

        new BukkitRunnable() {
            int ticks = 0;
            @Override public void run() {
                if (ticks++ >= 6 || target.isDead()) { cancel(); return; }
                world.spawnParticle(Particle.CHERRY_LEAVES,
                    target.getLocation().add(0, 1, 0), 8, 0.3, 0.3, 0.3, 0.02);
                Vector pull = player.getLocation().toVector()
                    .subtract(target.getLocation().toVector()).normalize().multiply(0.6);
                pull.setY(0.15);
                target.setVelocity(pull);
                target.damage(2.0, player);
            }
        }.runTaskTimer(plugin, 0, 3);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // DAGGER FORM ABILITIES (5 kills)
    // ══════════════════════════════════════════════════════════════════════════

    private void handleDaggerSlash(Player player) {
        long now = System.currentTimeMillis();
        UUID uid = player.getUniqueId();
        long last = (long) abilityCooldowns.getOrDefault(uid, 0L);
        long remaining = 5000 - (now - last);
        if (remaining > 0) {
            player.sendActionBar(Component.text("⚔ Slash recharging... " + (remaining / 1000), NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));
            return;
        }
        abilityCooldowns.put(uid, now);

        World world = player.getWorld();
        Location loc = player.getEyeLocation();

        // Wide sweep — hit everything in a cone in front
        for (Entity entity : world.getNearbyEntities(player.getLocation(), 4, 3, 4)) {
            if (entity.equals(player)) continue;
            if (!(entity instanceof LivingEntity living)) continue;
            Vector toEntity = entity.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
            double dot = toEntity.dot(player.getLocation().getDirection());
            if (dot > 0.3) { // within front cone
                living.damage(7.0, player);
                living.setVelocity(toEntity.multiply(0.6).setY(0.3));
                world.spawnParticle(Particle.CRIT, living.getLocation().add(0,1,0), 10, 0.3, 0.3, 0.3, 0.1);
            }
        }
        world.spawnParticle(Particle.SWEEP_ATTACK, loc, 5, 0.3, 0.3, 0.3, 0);
        world.spawnParticle(Particle.CHERRY_LEAVES, loc, 15, 0.5, 0.3, 0.5, 0.04);
        world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.5f, 1.1f);
        world.playSound(player.getLocation(), Sound.BLOCK_CHERRY_LEAVES_BREAK, 1f, 1.3f);
        player.sendActionBar(Component.text("⚔ Dagger Slash!", NamedTextColor.AQUA)
            .decoration(TextDecoration.ITALIC, false));
    }

    private void handleDaggerDash(Player player) {
        long now = System.currentTimeMillis();
        UUID uid = player.getUniqueId();
        long last = (long) pullCooldowns.getOrDefault(uid, 0L);
        long remaining = 8000 - (now - last);
        if (remaining > 0) {
            player.sendActionBar(Component.text("⚔ Dash recharging... " + (remaining / 1000), NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));
            return;
        }
        pullCooldowns.put(uid, now);

        World world = player.getWorld();
        Location eyeLoc = player.getEyeLocation();
        Vector dir = eyeLoc.getDirection();

        RayTraceResult result = world.rayTraceEntities(eyeLoc, dir, 10.0, 0.5, e -> !e.equals(player));
        if (result == null || !(result.getHitEntity() instanceof LivingEntity target)) {
            player.sendActionBar(Component.text("No target in range.", NamedTextColor.GRAY)
                .decoration(TextDecoration.ITALIC, false));
            return;
        }

        world.playSound(player.getLocation(), Sound.ITEM_TRIDENT_THROW, 1f, 1.5f);
        world.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 1.2f);
        world.spawnParticle(Particle.CHERRY_LEAVES, player.getLocation().add(0,1,0), 20, 0.3, 0.3, 0.3, 0.05);

        Location dashEnd = target.getLocation().clone()
            .add(dir.clone().multiply(1.5)).add(0, 0.1, 0);
        dashEnd.setYaw(player.getLocation().getYaw());
        dashEnd.setPitch(player.getLocation().getPitch());

        new BukkitRunnable() {
            @Override public void run() {
                player.teleport(dashEnd);
                target.damage(10.0, player);
                world.spawnParticle(Particle.SWEEP_ATTACK, target.getLocation().add(0,1,0), 8, 0.3, 0.3, 0.3, 0);
                world.spawnParticle(Particle.CHERRY_LEAVES, target.getLocation().add(0,1,0), 25, 0.5, 0.5, 0.5, 0.05);
                world.playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1.5f, 1.2f);
                player.sendActionBar(Component.text("⚔ Dagger Dash!", NamedTextColor.AQUA)
                    .decoration(TextDecoration.ITALIC, false));
            }
        }.runTaskLater(plugin, 3);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // BOW FORM ABILITIES (10 kills)
    // ══════════════════════════════════════════════════════════════════════════

    private void handleBowShot(Player player) {
        long now = System.currentTimeMillis();
        UUID uid = player.getUniqueId();
        long last = (long) abilityCooldowns.getOrDefault(uid, 0L);
        long remaining = 3000 - (now - last);
        if (remaining > 0) {
            player.sendActionBar(Component.text("🏹 Shot recharging... " + (remaining / 1000), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
            return;
        }
        abilityCooldowns.put(uid, now);

        World world = player.getWorld();
        // Fire a petal-infused arrow
        Arrow arrow = player.getWorld().spawnArrow(
            player.getEyeLocation(), player.getEyeLocation().getDirection(), 3.0f, 0f);
        arrow.setShooter(player);
        arrow.setDamage(8.0);
        arrow.setPickupStatus(Arrow.PickupStatus.DISALLOWED);
        arrow.setGlowing(true);

        // Trail particles on the arrow
        new BukkitRunnable() {
            @Override public void run() {
                if (!arrow.isValid() || arrow.isOnGround()) { cancel(); return; }
                world.spawnParticle(Particle.CHERRY_LEAVES, arrow.getLocation(), 4, 0.05, 0.05, 0.05, 0.01);
            }
        }.runTaskTimer(plugin, 0, 1);

        world.playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 1.5f, 1.0f);
        world.spawnParticle(Particle.CHERRY_LEAVES, player.getEyeLocation(), 10, 0.2, 0.2, 0.2, 0.03);
        player.sendActionBar(Component.text("🏹 Petal Arrow!", NamedTextColor.YELLOW)
            .decoration(TextDecoration.ITALIC, false));
    }

    private void handleBowVolley(Player player) {
        long now = System.currentTimeMillis();
        UUID uid = player.getUniqueId();
        long last = (long) pullCooldowns.getOrDefault(uid, 0L);
        long remaining = 12000 - (now - last);
        if (remaining > 0) {
            player.sendActionBar(Component.text("🏹 Volley recharging... " + (remaining / 1000), NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
            return;
        }
        pullCooldowns.put(uid, now);

        World world = player.getWorld();
        world.playSound(player.getLocation(), Sound.ENTITY_ARROW_SHOOT, 2f, 0.6f);
        world.spawnParticle(Particle.CHERRY_LEAVES, player.getLocation().add(0,1,0), 40, 0.6, 0.6, 0.6, 0.05);
        player.sendActionBar(Component.text("🏹 Petal Volley!", NamedTextColor.YELLOW)
            .decoration(TextDecoration.ITALIC, false));

        // Fire 5 arrows in spread
        new BukkitRunnable() {
            int shot = 0;
            @Override public void run() {
                if (shot++ >= 5) { cancel(); return; }
                Vector spread = player.getEyeLocation().getDirection()
                    .add(new Vector(
                        (Math.random() - 0.5) * 0.15,
                        (Math.random() - 0.5) * 0.15,
                        (Math.random() - 0.5) * 0.15
                    )).normalize();
                Arrow arrow = world.spawnArrow(player.getEyeLocation(), spread, 2.8f, 0f);
                arrow.setShooter(player);
                arrow.setDamage(5.0);
                arrow.setPickupStatus(Arrow.PickupStatus.DISALLOWED);
                arrow.setGlowing(true);
                world.spawnParticle(Particle.CHERRY_LEAVES, player.getEyeLocation(), 5, 0.1, 0.1, 0.1, 0.02);
            }
        }.runTaskTimer(plugin, 0, 2);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ORIGINAL: attack boost + owner lock
    // ══════════════════════════════════════════════════════════════════════════

    @EventHandler
    public void onScytheAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!ScytheItem.isScythe(player.getInventory().getItemInMainHand(), plugin)) return;
        // Existing attack boost — slightly increased base damage
        event.setDamage(event.getDamage() * 1.15);
        player.getWorld().spawnParticle(Particle.CHERRY_LEAVES,
            event.getEntity().getLocation().add(0, 1, 0), 6, 0.2, 0.2, 0.2, 0.03);
    }

    @EventHandler
    public void onNonOwnerHold(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (player.getName().equalsIgnoreCase(OWNER)) return;
        var item = player.getInventory().getItem(event.getNewSlot());
        if (!ScytheItem.isScythe(item, plugin)) return;

        player.sendMessage(
            Component.text("This relic is not yours to wield.", NamedTextColor.DARK_PURPLE)
                .decoration(TextDecoration.ITALIC, false)
                .append(Component.text(" ", NamedTextColor.DARK_PURPLE))
        );
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 1f, 0.5f);
    }

    // ── GETTERS for command ───────────────────────────────────────────────────
    public int getKills(UUID uid) {
        return killCounts.getOrDefault(uid, 0);
    }

    public ScytheForm getForm(UUID uid) {
        return currentForm.getOrDefault(uid, ScytheForm.SCYTHE);
    }
}
