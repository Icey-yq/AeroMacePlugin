package com.aeromace;

import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class AeroMacePlugin extends JavaPlugin implements Listener {

    private final HashMap<UUID, Long> dashCooldowns = new HashMap<>();
    private final HashMap<UUID, Long> slamCooldowns = new HashMap<>();
    
    private final long DASH_CD = 15 * 1000; 
    private final long SLAM_CD = 30 * 1000; 

    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        registerMaceRecipe();
        
        getCommand("giveaeromace").setExecutor((sender, command, label, args) -> {
            if (sender instanceof Player p) {
                p.getInventory().addItem(createMace());
                p.sendMessage(ChatColor.GOLD + "You have received the Aero-Breaker!");
            }
            return true;
        });
    }

    public ItemStack createMace() {
        ItemStack mace = new ItemStack(Material.MACE);
        ItemMeta meta = mace.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.GOLD + "Aero-Breaker");
            List<String> lore = new ArrayList<>();
            lore.add(ChatColor.GRAY + "Forged in the eye of the storm.");
            lore.add("");
            lore.add(ChatColor.YELLOW + "Right-Click: 10-Block Omni-Dash (15s CD)");
            lore.add(ChatColor.AQUA + "Passive: Immune to Fall Damage");
            lore.add(ChatColor.RED + "Passive: Meteor Slam (8+ Blocks Fall)");
            lore.add(ChatColor.DARK_RED + " -> Deals 5 Hearts True Damage (30s CD)");
            meta.setLore(lore);
            mace.setItemMeta(meta);
        }
        return mace;
    }

    private void registerMaceRecipe() {
        NamespacedKey key = new NamespacedKey(this, "aero_breaker");
        ShapedRecipe recipe = new ShapedRecipe(key, createMace());
        recipe.shape("PEP", "NHN", "PBP");
        recipe.setIngredient('P', Material.PHANTOM_MEMBRANE);
        recipe.setIngredient('E', Material.ELYTRA);
        recipe.setIngredient('N', Material.NETHERITE_INGOT);
        recipe.setIngredient('H', Material.HEAVY_CORE);
        recipe.setIngredient('B', Material.BREEZE_ROD);
        Bukkit.addRecipe(recipe);
    }

    @EventHandler
    public void onDash(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (isAeroMace(player.getInventory().getItemInMainHand())) {
            if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
                event.setCancelled(true);
                if (checkCooldown(player, dashCooldowns, DASH_CD, "Dash")) {
                    player.setVelocity(player.getLocation().getDirection().multiply(2.5));
                    player.getWorld().spawnParticle(Particle.SONIC_BOOM, player.getLocation(), 1);
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WIND_CHARGE_THROW, 1f, 1.2f);
                    dashCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
                }
            }
        }
    }

    @EventHandler
    public void onFallDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (isAeroMace(player.getInventory().getItemInMainHand())) {
            if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
                float dist = player.getFallDistance();
                event.setCancelled(true);
                if (dist >= 8.0f && checkCooldown(player, slamCooldowns, SLAM_CD, "Ground Slam")) {
                    triggerSlam(player);
                    slamCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
                }
            }
        }
    }

    private void triggerSlam(Player player) {
        Location loc = player.getLocation();
        loc.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, loc, 1);
        loc.getWorld().playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1f, 0.7f);

        for (Entity e : player.getNearbyEntities(6, 3, 6)) {
            if (e instanceof LivingEntity target && !e.equals(player)) {
                target.damage(10.0); // True Damage (ignores armor)
                target.getWorld().spawnParticle(Particle.WITCH, target.getLocation().add(0, 1, 0), 10);
                target.setVelocity(target.getLocation().toVector().subtract(loc.toVector()).normalize().multiply(1.5));
            }
        }
    }

    private boolean checkCooldown(Player p, HashMap<UUID, Long> map, long cd, String name) {
        if (map.containsKey(p.getUniqueId())) {
            long timeLeft = (map.get(p.getUniqueId()) + cd) - System.currentTimeMillis();
            if (timeLeft > 0) {
                p.sendMessage(ChatColor.RED + name + " is on cooldown! (" + (timeLeft / 1000) + "s)");
                return false;
            }
        }
        return true;
    }

    private boolean isAeroMace(ItemStack item) {
        return item != null && item.getType() == Material.MACE && item.hasItemMeta() && 
               item.getItemMeta().getDisplayName().equals(ChatColor.GOLD + "Aero-Breaker");
    }
}
