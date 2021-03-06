package minecrafttransportsimulator.packets.general;

import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PacketManualPageUpdate implements IMessage{
	private short pageNumber;

	public PacketManualPageUpdate() {}
	
	public PacketManualPageUpdate(short pageNumber){
		this.pageNumber = pageNumber;
	}
	
	@Override
	public void fromBytes(ByteBuf buf){
		this.pageNumber=buf.readShort();
	}

	@Override
	public void toBytes(ByteBuf buf){
		buf.writeShort(this.pageNumber);
	}

	public static class Handler implements IMessageHandler<PacketManualPageUpdate, IMessage>{
		@Override
		public IMessage onMessage(final PacketManualPageUpdate message, final MessageContext ctx){
			FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(new Runnable(){
				@Override
				public void run(){
					ItemStack stack = ctx.getServerHandler().playerEntity.inventory.getCurrentItem();
					if(!stack.hasTagCompound()){
						stack.setTagCompound(new NBTTagCompound());
					}
					stack.getTagCompound().setShort("page", message.pageNumber);
				}
			});
			return null;
		}
	}

}
