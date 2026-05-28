package com.aeromace;

import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class AeroMacePlugin extends JavaPlugin implements Listener {

    private final HashMap<UUID, Long> dashCooldowns = new HashMap<>();
    private final long DASH_CD_MILLIS = 15 * 1000;
    private final long DASH_CD_TICKS = 15 * 20;

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
            lore.add(ChatColor.YELLOW + "Right-Click: Wind Burst Dash (15s CD)");
            lore.add(ChatColor.AQUA + "Passive: Immune to Fall Damage");
            meta.setLore(lore);

            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
            meta.addEnchant(Enchantment.DENSITY, 5, true);
            meta.addEnchant(Enchantment.MENDING, 1, true);
            meta.addEnchant(Enchantment.UNBREAKING, 3, true);
            meta.addEnchant(Enchantment.WIND_BURST, 1, true);

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
                
                if (checkCooldown(player)) {
                    // Set Speed to 2.0
                    player.setVelocity(player.getLocation().getDirection().multiply(2.0));
                    
                    // Sound Only (Sonic/Flash particles removed)
                    player.getWorld().playSound(player.getLocation(), Sound.ENTITY_BREEZE_WIND_BURST, 1.2f, 1.0f);
                    
                    // White Cloud Trail
                    new BukkitRunnable() {
                        int ticks = 0;
                        @Override
                        public void run() {
                            if (ticks > 12 || !player.isOnline()) {
                                this.cancel();
                                return;
                            }
                            player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation(), 5, 0.1, 0.1, 0.1, 0.02);
                            ticks++;
                        }
                    }.runTaskTimer(this, 0, 1);

                    dashCooldowns.put(player.getUniqueId(), System.currentTimeMillis());
                    new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (player.isOnline()) {
                                player.sendMessage(ChatColor.GREEN + "Dash is ready!");
                                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.6f, 2.0f);
                            }
                        }
                    }.runTaskLater(this, DASH_CD_TICKS);
                }
            }
        }
    }

    @EventHandler
    public void onFallDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (isAeroMace(player.getInventory().getItemInMainHand())) {
                if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
                    event.setCancelled(true);
                }
            }
        }
    }

    private boolean checkCooldown(Player p) {
        if (dashCooldowns.containsKey(p.getUniqueId())) {
            long timeLeft = (dashCooldowns.get(p.getUniqueId()) + DASH_CD_MILLIS) - System.currentTimeMillis();
            if (timeLeft > 0) {
                p.sendMessage(ChatColor.RED + "Dash is on cooldown! (" + (timeLeft / 1000) + "s)");
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
