package mc.mkay.scythe;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ScytheKillsCommand implements CommandExecutor {

    private final ScytheListener listener;

    public ScytheKillsCommand(ScytheListener listener) {
        this.listener = listener;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(Component.text("Usage: /scythekills <player> <amount>", NamedTextColor.RED));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(Component.text("Player not found.", NamedTextColor.RED));
            return true;
        }

        int amount;
        try {
            amount = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sender.sendMessage(Component.text("Amount must be a number.", NamedTextColor.RED));
            return true;
        }

        listener.addKills(target, amount);
        int total = listener.getKills(target.getUniqueId());
        ScytheListener.ScytheForm form = listener.getForm(target.getUniqueId());

        sender.sendMessage(Component.text(
            "Added " + amount + " kills to " + target.getName() +
            " → total: " + total + " kills | Form: " + form.name(),
            NamedTextColor.LIGHT_PURPLE
        ));
        return true;
    }
}
