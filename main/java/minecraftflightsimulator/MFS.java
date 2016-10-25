package minecraftflightsimulator;

import java.util.List;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.ModMetadata;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@Mod(modid = MFS.MODID, name = MFS.MODNAME, version = MFS.MODVER)
public class MFS {
	public static final String MODID="mfs";
	public static final String MODNAME="Minecraft Flight Simulator";
	public static final String MODVER="7.0.3";
	
	@Instance(value = MFS.MODID)
	public static MFS instance;
	public static final SimpleNetworkWrapper MFSNet = NetworkRegistry.INSTANCE.newSimpleChannel("MFSNet");
	@SidedProxy(clientSide="minecraftflightsimulator.ClientProxy", serverSide="minecraftflightsimulator.CommonProxy")
	public static CommonProxy proxy;
	public static final CreativeTabs tabMFS = new CreativeTabs("tabMFS") {
	    @Override
		@SideOnly(Side.CLIENT)
	    public Item getTabIconItem() {
	    	return MFSRegistry.planeMC172;
	    }
	    
	    @Override
	    @SideOnly(Side.CLIENT)
	    public void displayAllReleventItems(List givenList){
	    	super.displayAllReleventItems(givenList);
	    	ItemStack[] itemArray = (ItemStack[]) givenList.toArray(new ItemStack[givenList.size()]); 
	    	int currentIndex = 0;
	    	for(int i=0; i<MFSRegistry.itemList.size(); ++i){
	    		for(int j=0; j<givenList.size(); ++j){
	    			if(MFSRegistry.itemList.get(i).equals(itemArray[j].getItem())){
	    				givenList.set(currentIndex++, itemArray[j]);
	    			}else{
	    			}
	    		}
	    	}
	    }
	};
	
	/*INS194
	public MFS(){
		FluidRegistry.enableUniversalBucket();
	}
	INS194*/
	
	@EventHandler
	public void PreInit(FMLPreInitializationEvent event){
		proxy.preInit(event);
		this.initModMetadata(event);
	}
	
	@EventHandler
	public void Init(FMLInitializationEvent event){
		proxy.init(event);
	}
	
	private void initModMetadata(FMLPreInitializationEvent event){
        ModMetadata meta = event.getModMetadata();
        meta.name = "Minecraft Flight Simulator";
        meta.description = "Realistic planes for Minecraft!";
        meta.authorList.clear();
        meta.authorList.add("don_bruce & CO.");
        meta.logoFile = "Vingette.png";
        meta.url = "http://minecraft.curseforge.com/projects/minecraft-flight-simulator";
        
        meta.modId = this.MODID;
        meta.version = this.MODVER;
        meta.autogenerated = false;
	}
}

