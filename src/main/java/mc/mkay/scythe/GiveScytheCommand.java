package mc.mkay.scythe;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class GiveScytheCommand implements CommandExecutor {

    private final ScythePlugin plugin;

    public GiveScytheCommand(ScythePlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.isOp() && !(sender instanceof ConsoleCommandSender)) {
            sender.sendMessage(Component.text("No permission.", NamedTextColor.RED));
            return true;
        }

        String targetName = args.length > 0 ? args[0] : "mkaymc";
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            sender.sendMessage(Component.text("Player " + targetName + " is not online.", NamedTextColor.RED));
            return true;
        }

        ItemStack scythe = ScytheItem.build(plugin);
        target.getInventory().addItem(scythe);
        target.sendMessage(
            Component.text("The relic has found you.", NamedTextColor.LIGHT_PURPLE)
                .decoration(TextDecoration.ITALIC, false)
        );
        sender.sendMessage(
            Component.text("Gave the Bloom's Scythe to ", NamedTextColor.GREEN)
                .decoration(TextDecoration.ITALIC, false)
                .append(Component.text(target.getName(), NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false))
        );
        return true;
    }
}
