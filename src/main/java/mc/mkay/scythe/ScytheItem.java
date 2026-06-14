package mc.mkay.scythe;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.List;

public class ScytheItem {

    public static final String NBT_KEY = "mkay_scythe";
    public static final int CUSTOM_MODEL_DATA = 1000;

    public static ItemStack build(Plugin plugin) {
        ItemStack item = new ItemStack(Material.MACE);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(
            Component.text("Bloom's Scythe", NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.ITALIC, false)
                .decoration(TextDecoration.BOLD, true)
        );

        meta.lore(List.of(
            Component.text("  The relic chose only one.", NamedTextColor.DARK_PURPLE).decoration(TextDecoration.ITALIC, false),
            Component.text("  Petals fall where others don't.", NamedTextColor.DARK_PURPLE).decoration(TextDecoration.ITALIC, false),
            Component.empty(),
            Component.text("Right-Click", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                .append(Component.text(" Blossom Burst", NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false)),
            Component.text("Sneak + Right-Click", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                .append(Component.text(" Petal Snare", NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false)),
            Component.empty(),
            Component.text("  Bound to:", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)
                .append(Component.text(" mkaymc", NamedTextColor.DARK_PURPLE).decoration(TextDecoration.ITALIC, false))
        ));

        meta.setCustomModelData(CUSTOM_MODEL_DATA);
        meta.getPersistentDataContainer().set(
            new NamespacedKey(plugin, NBT_KEY),
            PersistentDataType.BOOLEAN, true
        );

        meta.addUnsafeEnchantment(Enchantment.SHARPNESS, 5);
        meta.addUnsafeEnchantment(Enchantment.FIRE_ASPECT, 2);
        meta.addUnsafeEnchantment(Enchantment.SWEEPING_EDGE, 3);
        meta.addUnsafeEnchantment(Enchantment.LOOTING, 3);
        meta.addUnsafeEnchantment(Enchantment.UNBREAKING, 3);
        meta.addUnsafeEnchantment(Enchantment.MENDING, 1);
        meta.addUnsafeEnchantment(Enchantment.BREACH, 4);

        item.setItemMeta(meta);
        return item;
    }

    public static boolean isScythe(ItemStack item, Plugin plugin) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer()
            .has(new NamespacedKey(plugin, NBT_KEY), PersistentDataType.BOOLEAN);
    }
}
