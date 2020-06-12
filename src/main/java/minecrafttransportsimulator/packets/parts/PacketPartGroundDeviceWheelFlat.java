package minecrafttransportsimulator.packets.parts;

import minecrafttransportsimulator.vehicles.parts.PartGroundDevice;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public final class PacketPartGroundDeviceWheelFlat extends APacketPart{
	public PacketPartGroundDeviceWheelFlat(){}
	
	public PacketPartGroundDeviceWheelFlat(PartGroundDevice part){
		super(part);
	}

	public static class Handler implements IMessageHandler<PacketPartGroundDeviceWheelFlat, IMessage>{
		@Override
		public IMessage onMessage(final PacketPartGroundDeviceWheelFlat message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					PartGroundDevice wheel = (PartGroundDevice) getVehiclePartFromMessage(message, ctx);
					if(wheel != null){
						wheel.setFlat();
					}
				}
			});
			return null;
		}
	}

}
