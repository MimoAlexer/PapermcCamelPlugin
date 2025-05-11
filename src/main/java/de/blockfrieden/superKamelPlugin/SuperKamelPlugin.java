package de.blockfrieden.superKamelPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Camel;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityBreedEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

public class SuperKamelPlugin extends JavaPlugin implements Listener {
    private final Map<UUID, Inventory> camelInventories = new HashMap<>();
    private NamespacedKey keySize;
    private NamespacedKey keyBaseSpeed;
    private final Map<Material, Double> itemWeights = new HashMap<>();

    @Override
    public void onEnable() {
        keySize = new NamespacedKey(this, "camelSize");
        keyBaseSpeed = new NamespacedKey(this, "baseSpeed");
        itemWeights.put(Material.NETHERITE_BLOCK, 100.0);
        itemWeights.put(Material.DIAMOND_BLOCK, 64.0);
        itemWeights.put(Material.IRON_BLOCK, 32.0);
        itemWeights.put(Material.GOLD_BLOCK, 32.0);
        itemWeights.put(Material.CHEST, 8.0);
        itemWeights.put(Material.ENDER_CHEST, 8.0);
        itemWeights.put(Material.BARREL, 8.0);
        itemWeights.put(Material.HOPPER, 8.0);
        itemWeights.put(Material.SHULKER_BOX, 8.0);
        itemWeights.put(Material.BEACON, 8.0);
        itemWeights.put(Material.CAMPFIRE, 4.0);
        itemWeights.put(Material.FURNACE, 4.0);
        itemWeights.put(Material.BLAST_FURNACE, 4.0);
        itemWeights.put(Material.SMOKER, 4.0);
        itemWeights.put(Material.CRAFTING_TABLE, 2.0);
        itemWeights.put(Material.ANVIL, 32.0);
        itemWeights.put(Material.COBBLESTONE, 4.0);
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("CamelCarryPlugin enabled!");
    }

    @EventHandler
    public void onCamelSpawn(EntitySpawnEvent e) {
        if (!(e.getEntity() instanceof Camel camel)) return;
        PersistentDataContainer pdc = camel.getPersistentDataContainer();
        if (!pdc.has(keySize, PersistentDataType.DOUBLE)) {
            pdc.set(keySize, PersistentDataType.DOUBLE, 1.0);
        }
        if (!pdc.has(keyBaseSpeed, PersistentDataType.DOUBLE)) {
            AttributeInstance spd = camel.getAttribute(Attribute.MOVEMENT_SPEED);
            if (spd != null) {
                pdc.set(keyBaseSpeed, PersistentDataType.DOUBLE, spd.getBaseValue());
            }
        }
    }

    @EventHandler
    public void onBreed(EntityBreedEvent event) {
        if (!(event.getEntity() instanceof Camel baby)) return;
        LivingEntity mother = event.getMother();
        LivingEntity father = event.getFather();
        if (!(mother instanceof Camel mom) || !(father instanceof Camel dad)) return;
        double sizeMom = mom.getPersistentDataContainer().getOrDefault(keySize, PersistentDataType.DOUBLE, 1.0);
        double sizeDad = dad.getPersistentDataContainer().getOrDefault(keySize, PersistentDataType.DOUBLE, 1.0);
        double minSize = Math.min(sizeMom, sizeDad);
        double maxSize = Math.max(sizeMom, sizeDad);
        double childSize = minSize + Math.random() * (maxSize - minSize);
        baby.getPersistentDataContainer().set(keySize, PersistentDataType.DOUBLE, childSize);
        AttributeInstance scaleAttr = baby.getAttribute(Attribute.SCALE);
        if (scaleAttr != null) {
            scaleAttr.setBaseValue(childSize);
        }
    }

    @EventHandler
    public void onCamelInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Camel camel)) return;
        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();

        if (camel.getInventory().getSaddle() == null) {
            player.sendMessage("You need to saddle the camel first!");
            return;
        }

        if (hand.getType() == Material.CHEST) {
            event.setCancelled(true);
        } else if (hand.getType() != Material.AIR) {
            return;
        }

        Inventory inv = camelInventories.computeIfAbsent(
                camel.getUniqueId(),
                // doooooooont care, where is rick?
                id -> Bukkit.createInventory(camel, 54, camel.getName() + "'s Storage")
        );
        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        Inventory inv = e.getInventory();
        if (inv.getHolder() instanceof Camel camel) {
            updateCamelSpeed(camel);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        Inventory inv = e.getInventory();
        if (inv.getHolder() instanceof Camel camel) {
            updateCamelSpeed(camel);
        }
    }

    private void updateCamelSpeed(Camel camel) {
        Inventory inv = camelInventories.get(camel.getUniqueId());
        if (inv == null) return;
        double totalWeight = 0.0;
        for (ItemStack item : inv.getContents()) {
            if (item == null) continue;
            double weight = itemWeights.getOrDefault(item.getType(), 1.0);
            totalWeight += weight * item.getAmount();
        }
        double baseSpeed = camel.getPersistentDataContainer()
                .getOrDefault(keyBaseSpeed, PersistentDataType.DOUBLE, 0.1);
        double newSpeed = baseSpeed / (1 + totalWeight / 50.0);
        AttributeInstance speedAttr = camel.getAttribute(Attribute.MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.setBaseValue(newSpeed);
        }
    }

    @EventHandler
    public void onCamelDeath(EntityDeathEvent e) {
        if (!(e.getEntity() instanceof Camel camel)) return;
        Inventory inv = camelInventories.remove(camel.getUniqueId());
        if (inv != null) {
            for (ItemStack item : inv.getContents()) {
                if (item != null) {
                    camel.getWorld().dropItemNaturally(camel.getLocation(), item);
                }
            }
        }
    }
}
