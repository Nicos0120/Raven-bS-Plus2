package keystrokesmod.mixin.impl.accessor;

import net.minecraft.network.INetHandler;
import net.minecraft.network.NetworkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(NetworkManager.class)
public interface IAccessorNetworkManager {
    @Accessor("packetListener")
    INetHandler getPacketListener();
}